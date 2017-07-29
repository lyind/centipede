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

package net.talpidae.centipede;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Singleton;
import lombok.val;
import net.talpidae.base.util.thread.GeneralScheduler;
import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.bean.service.State;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.NewMapping;
import net.talpidae.centipede.task.health.PulseCheck;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;


@Singleton
public class CentipedeLogic
{
    private final CentipedeRepository repository;

    private final EventBus eventBus;

    private final GeneralScheduler scheduler;


    @Inject
    public CentipedeLogic(CentipedeRepository repository, EventBus eventBus, GeneralScheduler scheduler, PulseCheck pulseCheck)
    {
        this.repository = repository;
        this.eventBus = eventBus;
        this.scheduler = scheduler;

        eventBus.register(this);

        scheduler.scheduleWithFixedDelay(pulseCheck, 3500L, 1500L, TimeUnit.MILLISECONDS);
    }


    /**
     * Received a mapping for a previously unknown service. Persist if not done so already.
     */
    @Subscribe
    public void onNewMapping(NewMapping newMapping)
    {
        val mapping = newMapping.getMapping();
        val updatedService = Service.builder()
                .name(mapping.getName())
                .state(State.CHANGING)
                .route(mapping.getRoute())
                .host(mapping.getHost())
                .port(mapping.getPort())
                .build();

        repository.insertServiceState(updatedService);
    }
}
