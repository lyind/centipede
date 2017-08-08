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
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import net.talpidae.base.util.queue.ConcurrentArrayOffsetQueue;
import net.talpidae.base.util.queue.Enqueueable;
import net.talpidae.base.util.session.SessionHolder;
import net.talpidae.centipede.bean.service.Api;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Singleton
@Slf4j
public class ApiBroadcastQueue
{
    private static final int DEFAULT_CAPACITY = 60;

    private final byte[] NEW_BROADCAST_EVENT = new byte[0];

    // we store serialized messages in the queue, to support multiple clients efficiently
    private final ConcurrentArrayOffsetQueue<String> queue = new ConcurrentArrayOffsetQueue<>(DEFAULT_CAPACITY);

    private final ObjectWriter apiWriter;


    @Inject
    public ApiBroadcastQueue(ObjectMapper objectMapper)
    {
        this.apiWriter = objectMapper.writerFor(Api.class);
    }


    public void add(Api element)
    {
        try
        {
            queue.add(apiWriter.writeValueAsString(element));

            synchronized (NEW_BROADCAST_EVENT)
            {
                NEW_BROADCAST_EVENT.notifyAll();
            }
        }
        catch (JsonProcessingException e)
        {
            log.error("failed to add broadcast message to queue: {}: {}", element, e.getMessage());
        }
    }


    public List<Enqueueable<String>> pollSince(long offset)
    {
        return queue.pollSince(offset);
    }


    public void waitForElement(long timeout, TimeUnit unit) throws InterruptedException
    {
        synchronized (NEW_BROADCAST_EVENT)
        {
            NEW_BROADCAST_EVENT.wait(unit.toMillis(timeout));
        }
    }
}
