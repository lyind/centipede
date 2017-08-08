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

package net.talpidae.centipede.resource;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.talpidae.base.server.WebSocketEndpoint;
import net.talpidae.base.util.session.SessionHolder;
import net.talpidae.centipede.service.calls.CallHandlerQueue;

import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;


@SuppressWarnings("ServerEndpointInconsistencyInspection")
@Slf4j
@Singleton
@ServerEndpoint("/")
public class CentipedeWebSocketEndPoint extends WebSocketEndpoint
{
    private final SessionHolder sessionHolder;

    private final CallHandlerQueue apiCallQueue;


    @Inject
    public CentipedeWebSocketEndPoint(SessionHolder sessionHolder, CallHandlerQueue apiCallQueue)
    {
        this.sessionHolder = sessionHolder;
        this.apiCallQueue = apiCallQueue;
    }

    @Override
    public void connect(Session session) throws IOException
    {
        log.debug("connect(): {}", session.getId());

        apiCallQueue.installChainSender(session);

        sessionHolder.put(session);
    }

    @Override
    public void message(String message, Session session)
    {
        log.debug("message(): {}: {}", session.getId(), message);
        apiCallQueue.enqueue(session, message);
    }

    @Override
    public void message(byte[] bytes, boolean done, Session session)
    {
        log.debug("unhandled binary message(): {}: {}, done={}", session.getId(), bytes, done);
    }

    @Override
    public void error(Throwable throwable, Session session)
    {
        log.debug("error() for session: {}", session.getId(), throwable);
    }

    @Override
    public void close(CloseReason closeReason, Session session)
    {
        log.debug("close() for session: {}: reason={}", session.getId(), closeReason);
        sessionHolder.remove(session.getId());
    }
}
