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

package net.talpidae.centipede.service.calls;


import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.talpidae.base.event.Shutdown;
import net.talpidae.base.util.thread.NamedThreadFactory;
import net.talpidae.centipede.service.ApiRunnableFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.websocket.Session;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;


@Singleton
@Slf4j
public class CallQueue
{
    private static final int DEFAULT_CORE_POOL_SIZE = Math.max(2, (int) Math.ceil(Runtime.getRuntime().availableProcessors() / 4));

    private final ScheduledExecutorService executorService;

    private final ApiRunnableFactory apiRunnableFactory;


    @Inject
    public CallQueue(ApiRunnableFactory apiRunnableFactory)
    {
        this.executorService = Executors.newScheduledThreadPool(DEFAULT_CORE_POOL_SIZE, new NamedThreadFactory(CallQueue.class.getSimpleName()));
        this.apiRunnableFactory = apiRunnableFactory;
    }


    @Subscribe
    public void onShutdown(Shutdown shutdown)
    {
        executorService.shutdown();
    }


    public void enqueue(Session session, String request)
    {
        try
        {
            executorService.submit(apiRunnableFactory.create(session, request));
        }
        catch (RejectedExecutionException e)
        {
            log.debug("rejected to enqueue request for session: {}", session.getId());
        }
    }


    public void enqueueBroadcasts(Session session)
    {
        try
        {
            executorService.submit(apiRunnableFactory.createBroadcasts(session));
        }
        catch (RejectedExecutionException e)
        {
            log.debug("rejected to enqueue broadcasts for session: {}", session.getId());
        }
    }
}
