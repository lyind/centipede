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

import net.talpidae.base.event.ServerShutdown;
import net.talpidae.base.event.ServerStarted;
import net.talpidae.base.insect.Queen;
import net.talpidae.base.util.thread.GeneralScheduler;
import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.bean.service.State;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.DependenciesChanged;
import net.talpidae.centipede.event.DependenciesModified;
import net.talpidae.centipede.event.NewMapping;
import net.talpidae.centipede.event.ServicesModified;
import net.talpidae.centipede.task.health.HealthCheck;
import net.talpidae.centipede.task.init.InitTask;
import net.talpidae.centipede.task.state.StateMachine;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import lombok.val;


@Slf4j
@Singleton
public class CentipedeLogic
{
    private final CentipedeRepository repository;

    private final EventBus eventBus;

    private final GeneralScheduler scheduler;

    private final HealthCheck pulseCheck;

    private final StateMachine stateMachine;

    private final Queen queen;


    @Inject
    public CentipedeLogic(CentipedeRepository repository,
                          EventBus eventBus,
                          GeneralScheduler scheduler,
                          HealthCheck pulseCheck,
                          StateMachine stateMachine,
                          Queen queen,
                          InitTask initTask)
    {
        this.repository = repository;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.pulseCheck = pulseCheck;
        this.stateMachine = stateMachine;
        this.queen = queen;

        eventBus.register(this);

        // initialize service database immediately
        scheduler.schedule(initTask);
    }


    @Subscribe
    public void onServerStarted(ServerStarted started)
    {
        queen.run();

        // set all services to state "UNKNOWN" initially
        overrideStateUnknown();

        // give services already running enough time to register
        scheduler.scheduleWithFixedDelay(pulseCheck, 4000L, 1500L, TimeUnit.MILLISECONDS);

        // don't try starting services while we are still waiting for externally launched services state
        scheduler.scheduleWithFixedDelay(stateMachine, 7000L, 1000L, TimeUnit.MILLISECONDS);
    }


    /**
     * Received a mapping for a previously unknown service. Persist if not done so already.
     */
    @Subscribe
    public void onNewMapping(NewMapping newMapping)
    {
        scheduler.schedule(() ->
        {
            try
            {
                val mapping = newMapping.getMapping();
                val updatedService = Service.builder()
                        .name(mapping.getName())
                        .retired(false)
                        .state(State.CHANGING)
                        .route(mapping.getRoute())
                        .host(mapping.getHost())
                        .port(mapping.getPort())
                        .build();

                repository.insertServiceState(updatedService);
                eventBus.post(new ServicesModified(Collections.singletonList(mapping.getName())));
            }
            catch (Throwable e)
            {
                log.error("onNewMapping(): scheduled task failed: {}", e.getMessage(), e);
            }
        });
    }


    /**
     * Dependencies of a service were updated. Track changes in database.
     */
    @Subscribe
    public void onDependenciesChanged(DependenciesChanged dependenciesChanged)
    {
        scheduler.schedule(() ->
        {
            try
            {
                repository.setDependencies(dependenciesChanged.getName(), dependenciesChanged.getDependencies());

                eventBus.post(new DependenciesModified());
            }
            catch (Throwable e)
            {
                log.error("onDependenciesChanged(): scheduled task failed: {}", e.getMessage(), e);
            }
        });
    }


    @Subscribe
    public void onServerShutdown(ServerShutdown shutdown)
    {
        try
        {
            queen.close();
        }
        catch (IOException e)
        {
            // never happens
        }
    }


    private void overrideStateUnknown()
    {
        scheduler.schedule(() ->
        {
            try
            {
                val names = repository.findAllNames();
                for (val name : names)
                {
                    val updatedService = Service.builder()
                            .name(name)
                            .state(State.UNKNOWN)
                            .build();

                    repository.insertServiceState(updatedService);
                }

                eventBus.post(new ServicesModified(names));
            }
            catch (Throwable e)
            {
                log.error("overrideStateUnknown(): scheduled task failed: {}", e.getMessage(), e);
            }
        });
    }
}