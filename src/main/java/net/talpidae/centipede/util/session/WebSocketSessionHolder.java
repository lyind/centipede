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

package net.talpidae.centipede.util.session;

import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.talpidae.base.event.Shutdown;
import net.talpidae.base.util.session.SessionHolder;
import net.talpidae.base.util.thread.NamedThreadFactory;
import net.talpidae.centipede.service.ApiBroadcastQueue;
import net.talpidae.centipede.service.calls.CallQueue;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.websocket.Session;
import java.util.concurrent.*;


@Singleton
@Slf4j
public class WebSocketSessionHolder implements SessionHolder
{
    private final static long BROADCAST_QUEUE_WAIT_TIMEOUT_MSEC = 1000;

    private final ConcurrentMap<String, Session> idToSession = new ConcurrentHashMap<>();

    private final ApiBroadcastQueue broadcastQueue;

    private final ExecutorService broadcastExecutorService;

    private final CallQueue apiCallQueue;


    @Inject
    public WebSocketSessionHolder(ApiBroadcastQueue broadcastQueue, CallQueue apiCallQueue)
    {
        this.broadcastQueue = broadcastQueue;
        this.apiCallQueue = apiCallQueue;
        this.broadcastExecutorService = Executors.newSingleThreadExecutor(new NamedThreadFactory("WebSocket-Broadcast"));

        this.broadcastExecutorService.submit(new BroadcastWorker());
    }


    @Subscribe
    public void onShutdown(Shutdown shutdown)
    {
        broadcastExecutorService.shutdown();
    }


    @Override
    public void put(Session session)
    {
        idToSession.put(session.getId(), session);
    }

    @Override
    public void remove(String id)
    {
        idToSession.remove(id);
    }


    private class BroadcastWorker implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                while (!Thread.interrupted())
                {
                    broadcastQueue.waitForElement(BROADCAST_QUEUE_WAIT_TIMEOUT_MSEC, TimeUnit.MILLISECONDS);
                    for (val session : idToSession.values())
                    {
                        if (session.isOpen())
                        {
                            apiCallQueue.enqueueBroadcasts(session);
                        }
                    }
                }
            }
            catch (InterruptedException e)
            {
                log.debug("interrupted");
                Thread.currentThread().interrupt();
            }
        }
    }
}
