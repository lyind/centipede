/*
 * Copyright (C) 2017  Jonas Zeiger <jonas.zeiger@talpidae.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.talpidae.centipede.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import net.talpidae.base.util.auth.AuthenticationSecurityContext;
import net.talpidae.base.util.queue.Enqueueable;
import net.talpidae.centipede.bean.Api;
import net.talpidae.centipede.service.calls.CallException;
import net.talpidae.centipede.service.calls.CallHandler;
import net.talpidae.centipede.service.calls.Security;
import net.talpidae.centipede.service.chain.ChainSender;
import net.talpidae.centipede.service.wrapper.Call;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

import lombok.extern.slf4j.Slf4j;
import lombok.val;


@Singleton
@Slf4j
public class ApiRunnableFactory
{
    private final static String SESSION_BROADCAST_OFFSET_KEY = ApiRunnableFactory.log.getName() + "-broadcast";

    private final static String SESSION_SECURITY_CONTEXT_KEY = ApiRunnableFactory.log.getName() + "-security-context";

    private final static String SESSION_CHAIN_SENDER_KEY = ApiRunnableFactory.log.getName() + "-chain-sender";

    private final List<CallHandler> handlersByPhase;

    private final ObjectReader apiReader;

    private final ObjectWriter apiWriter;

    private final ApiBroadcastQueue apiBroadcastQueue;

    private final String apiBroadcastOverflow;


    @Inject
    public ApiRunnableFactory(ObjectMapper objectMapper, Set<CallHandler> apiFunctions, ApiBroadcastQueue apiBroadcastQueue)
    {
        this.handlersByPhase = new ArrayList<>(apiFunctions);

        // important, sort the calls by phase to allow for some order (authentication first and things like that...)
        this.handlersByPhase.sort(Comparator.comparing(CallHandler::getPhase));

        this.apiReader = objectMapper.readerFor(Api.class);
        this.apiWriter = objectMapper.writerFor(Api.class);
        this.apiBroadcastQueue = apiBroadcastQueue;
        try
        {
            this.apiBroadcastOverflow = apiWriter.writeValueAsString(Api.builder().overflow(true).build());
        }
        catch (JsonProcessingException e)
        {
            log.error("failed to serialize broadcast overflow message, can't create {} instance", ApiRunnableFactory.class.getName(), e);
            throw new IllegalStateException("failed to serialize broadcast overflow message: " + e.getMessage());
        }
    }

    private static ChainSender getChainSender(Session session)
    {
        return (ChainSender) session.getUserProperties().get(SESSION_CHAIN_SENDER_KEY);
    }

    @SuppressWarnings("unchecked")
    private static AuthenticationSecurityContext getSessionSecurityContext(Session session)
    {
        return ((AtomicReference<AuthenticationSecurityContext>) session.getUserProperties().get(SESSION_SECURITY_CONTEXT_KEY)).get();
    }

    @SuppressWarnings("unchecked")
    private static void setSessionSecurityContext(Session session, AuthenticationSecurityContext securityContext)
    {
        ((AtomicReference<AuthenticationSecurityContext>) session.getUserProperties().get(SESSION_SECURITY_CONTEXT_KEY)).set(securityContext);
    }

    private void setSessionEventOffset(Session session, Long offset)
    {
        ((AtomicLong) session.getUserProperties().get(SESSION_BROADCAST_OFFSET_KEY)).set(offset);
    }

    public Runnable create(Session session, String request)
    {
        return () -> processRequest(session, request);
    }

    public Runnable create(Session session, Api request)
    {
        return () -> processRequest(session, request);
    }

    public Runnable createBroadcasts(Session session)
    {
        return () -> broadcastEvents(session);
    }

    private void sendResponse(Session session, Api api)
    {
        sendResponse(session, api, null);
    }

    private void sendResponse(Session session, String message)
    {
        sendResponse(session, message, null);
    }

    private void sendResponse(Session session, Api api, SendHandler sendHandler)
    {
        try
        {
            sendResponse(session, apiWriter.writeValueAsString(api), sendHandler);
        }
        catch (JsonProcessingException e)
        {
            log.error("failed to serialize message for session: {}: {}", session.getId(), api);
            if (sendHandler != null)
            {
                sendHandler.onResult(new SendResult(e));
            }
        }
    }

    private void sendResponse(Session session, String message, SendHandler sendHandler)
    {
        session.getAsyncRemote().sendText(message, result ->
        {
            Optional.ofNullable(result.getException())
                    .ifPresent(e -> log.warn("failed to send message for session: {}: {}", session.getId(), e));

            if (sendHandler != null)
            {
                sendHandler.onResult(result);
            }
        });
    }

    /**
     * Attach the ChainSender instance to a websocket session.
     */
    public void attachChainSender(Session session)
    {
        session.getUserProperties().put(SESSION_SECURITY_CONTEXT_KEY, new AtomicReference<AuthenticationSecurityContext>(null));
        session.getUserProperties().put(SESSION_BROADCAST_OFFSET_KEY, new AtomicLong(Long.MAX_VALUE));
        session.getUserProperties().put(SESSION_CHAIN_SENDER_KEY, new ChainSender((chainSender, previous, result) ->
        {
            if (previous == null || result.isOK())
            {
                // update offset to prevent previously sent value from being considered again
                if (previous != null)
                {
                    setSessionEventOffset(session, previous.getOffset());
                }

                val next = chainSender.next();
                if (next != null)
                {
                    sendResponse(session, (String) next.getElement(), chainSender);
                }
            }
            else
            {
                // failed to send one element: force OVERFLOW on next poll
                setSessionEventOffset(session, Long.MAX_VALUE);
            }
        }));
    }

    /**
     * Flush all broadcast events for the specified session.
     */
    private void broadcastEvents(Session session)
    {
        // restore security context
        if (getSessionSecurityContext(session) == null)
        {
            // no broadcasts for unauthorized sessions, send a token first
            return;
        }

        // try to acquire sender instance
        val chainSender = getChainSender(session);
        if (chainSender != null && chainSender.isProbablyFree())
        {
            // can send right now, no other messages in flight for this session
            val offset = getSessionEventOffset(session);
            val events = apiBroadcastQueue.pollSince(offset);
            if (!events.isEmpty())
            {
                // overflow, store offset for next poll
                val firstEvent = events.get(0);
                if (firstEvent.getElement() == null)
                {
                    val queueOverflowIndicator = new OverflowEnqueueableWrapper(firstEvent);
                    if (chainSender.enqueue(Collections.singletonList(queueOverflowIndicator).iterator()))
                    {
                        // next event is the one at the current offset
                        setSessionEventOffset(session, firstEvent.getOffset());
                    }
                }
                else
                {
                    // try to send out all new events
                    if (chainSender.enqueue(events.iterator()))
                    {
                        // mark all the elements as consumed, if a send fails later we just force an overflow later
                        val lastEvent = events.get(events.size() - 1);
                        setSessionEventOffset(session, lastEvent.getOffset());
                    }
                }
            }
        }
    }

    private void processRequest(Session session, String request)
    {
        try
        {
            processRequest(session, (Api) apiReader.readValue(request));
            return;
        }
        catch (JsonProcessingException e)
        {
            log.warn("failed to de-serialize API request: {}: {}", e.getMessage(), request);
        }
        catch (IOException e)
        {
            log.error("error reading request for session: {}: {}", session.getId(), e.getMessage());
        }

        sendResponse(session, Api.builder().error("BAD_REQUEST").build());
    }

    private void processRequest(Session session, Api request)
    {
        // security context is restored (if already available)
        val call = new Call(request, getSessionSecurityContext(session));
        try
        {
            if (request != null)
            {
                handlersByPhase.forEach(handler -> handler.accept(call));
            }
            else
            {
                call.getResponse().error("request is null");
            }
        }
        catch (Security.AuthenticationException e)
        {
            log.debug("session: {}: {}", session.getId(), e.getMessage());
            call.getResponse().token("");
            call.getResponse().error(e.getMessage());
        }
        catch (CallException e)
        {
            log.warn("processing API request from session: {}: {}", session.getId(), e.getMessage());
            call.getResponse().error(e.getMessage());
        }
        catch (RuntimeException e)
        {
            log.error("error handling API request for session: {}: {}", session.getId(), e.getMessage(), e);
            call.getResponse().error("INTERNAL_SERVER_ERROR");
        }

        // save security context
        setSessionSecurityContext(session, call.getSecurityContext());

        sendResponse(session, call.getResponse().build());
    }

    private long getSessionEventOffset(Session session)
    {
        return ((AtomicLong) session.getUserProperties().get(SESSION_BROADCAST_OFFSET_KEY)).get();
    }


    private class OverflowEnqueueableWrapper implements Enqueueable<String>
    {
        private final Enqueueable<?> enqueueable;


        OverflowEnqueueableWrapper(Enqueueable<?> enqueueable)
        {
            this.enqueueable = enqueueable;
        }


        @Override
        public long getOffset()
        {
            return enqueueable.getOffset();
        }

        @Override
        public String getElement()
        {
            return apiBroadcastOverflow;
        }
    }
}