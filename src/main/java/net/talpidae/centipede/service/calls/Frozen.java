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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import net.talpidae.centipede.bean.Api;
import net.talpidae.centipede.bean.frozen.FrozenState;
import net.talpidae.centipede.event.Freezing;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.Getter;
import lombok.val;


@Getter
@Singleton
public class Frozen implements CallHandler
{
    private final Phase phase = Phase.HANDLE;

    private final EventBus eventBus;

    // we track the last state here, for simplicity
    private final AtomicBoolean isFrozen = new AtomicBoolean(false);


    @Inject
    public Frozen(EventBus eventBus)
    {
        this.eventBus = eventBus;
        eventBus.register(this);
    }


    @Override
    public Api apply(Api request)
    {
        if (request != null && request.getFrozen() != null)
        {
            val frozenState = request.getFrozen();
            if (!frozenState.equals(FrozenState.UNKNOWN))
            {
                eventBus.post(new Freezing(FrozenState.TRUE.equals(frozenState)));
            }

            // updates are propagated asynchronously
            request.setFrozen(isFrozen.get() ? FrozenState.TRUE : FrozenState.FALSE);
        }

        return request;
    }


    @Subscribe
    public void onFreezing(Freezing freezing)
    {
        isFrozen.compareAndSet(!freezing.isFrozen(), freezing.isFrozen());
    }
}