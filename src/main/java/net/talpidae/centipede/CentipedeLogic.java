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
import com.google.common.net.HostAndPort;
import com.google.inject.Singleton;

import net.talpidae.base.event.ServerShutdown;
import net.talpidae.base.event.ServerStarted;
import net.talpidae.base.insect.Queen;
import net.talpidae.base.insect.state.InsectState;
import net.talpidae.base.util.thread.GeneralScheduler;
import net.talpidae.centipede.bean.configuration.Configuration;
import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.bean.service.State;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.DependenciesChanged;
import net.talpidae.centipede.event.DependenciesModified;
import net.talpidae.centipede.event.Freezing;
import net.talpidae.centipede.event.NewMapping;
import net.talpidae.centipede.event.NewMetrics;
import net.talpidae.centipede.event.ServiceTimedOut;
import net.talpidae.centipede.event.ServicesModified;
import net.talpidae.centipede.task.init.InitTask;
import net.talpidae.centipede.task.maintenance.MaintenanceTask;
import net.talpidae.centipede.task.state.StateMachine;
import net.talpidae.centipede.util.service.ServiceUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;


@Slf4j
@Singleton
public class CentipedeLogic
{
    private final CentipedeRepository repository;

    private final EventBus eventBus;

    private final GeneralScheduler scheduler;

    private final StateMachine stateMachine;

    private final Queen queen;

    private final MaintenanceTask maintenanceTask;

    private final Configuration configuration;


    @Inject
    public CentipedeLogic(CentipedeRepository repository,
                          EventBus eventBus,
                          GeneralScheduler scheduler,
                          StateMachine stateMachine,
                          Queen queen,
                          Configuration configuration,
                          InitTask initTask,
                          MaintenanceTask maintenanceTask)
    {
        this.repository = repository;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.stateMachine = stateMachine;
        this.queen = queen;
        this.maintenanceTask = maintenanceTask;
        this.configuration = configuration;

        eventBus.register(this);

        // initialize service database immediately
        scheduler.schedule(initTask);
        scheduler.schedule(maintenanceTask); // perform immediate maintenance

        if (Boolean.TRUE.equals(configuration.getIsFrozen()))
        {
            eventBus.post(new Freezing(true));
        }
    }

    /**
     * Is the service in a stable state?
     */
    private static boolean isStableAndNotDown(Service service)
    {
        val state = service.getState();

        return state != State.DOWN && state != State.CHANGING && !(state == State.UNKNOWN && ServiceUtil.hasValidPid(service));
    }

    private static boolean isSameInstance(Service service, InsectState state)
    {
        val socketAddress = state.getSocketAddress();
        val servicePort = firstNonNull(service.getPort(), 0);

        val knownHostAndPort = HostAndPort.fromParts(nullToEmpty(service.getHost()), servicePort >= 0 ? servicePort : 0);
        val instanceHostAndPort = HostAndPort.fromParts(nullToEmpty(socketAddress.getHostString()), socketAddress.getPort());

        return knownHostAndPort.equals(instanceHostAndPort);
    }

    @Subscribe
    public void onServerStarted(ServerStarted started)
    {
        // set all services to state "UNKNOWN" initially
        initializeQueen();

        // don't try starting services while we are still waiting for externally launched services state
        scheduler.scheduleWithFixedDelay(stateMachine, 7000L, 1000L, TimeUnit.MILLISECONDS);

        // start periodic DB maintenance service
        scheduler.scheduleWithFixedDelay(maintenanceTask, configuration.getMaintenanceIntervalMinutes(), configuration.getMaintenanceIntervalMinutes(), TimeUnit.MINUTES);
    }

    /*
     * A service timed out. Register it as Status.DOWN in the DB.
     */
    @Subscribe
    public void onServiceTimedOut(ServiceTimedOut serviceTimedOut)
    {
        val state = serviceTimedOut.getState();
        val name = state.getName();
        scheduler.schedule(() ->
        {
            try
            {
                repository.findServiceByName(name)
                        .filter(service -> isSameInstance(service, state))
                        .filter(CentipedeLogic::isStableAndNotDown)
                        .map(service -> Service.builder().name(name).state(State.UNKNOWN).build())
                        .ifPresent(service ->
                        {
                            repository.insertServiceState(service);

                            eventBus.post(new ServicesModified(Collections.singletonList(name)));
                        });
            }
            catch (Throwable t)
            {
                log.error("failed to register service timeout for: {}: {}", name, t.getMessage(), t);
            }
        });
    }


    /**
     * Received a mapping for a service not present or timed-out before. Update info in DB.
     */
    @Subscribe
    public void onNewMapping(NewMapping newMapping)
    {
        scheduler.schedule(() ->
        {
            try
            {
                val mapping = newMapping.getMapping();
                val name = mapping.getName();
                val updatedService = Service.builder()
                        .name(name)
                        .retired(false)
                        .state(newMapping.getState().isOutOfService() ? State.OUT_OF_SERVICE : State.UP)
                        .route(mapping.getRoute())
                        .host(mapping.getHost())
                        .port(mapping.getPort())
                        .build();

                repository.insertServiceState(updatedService);
                eventBus.post(new ServicesModified(Collections.singletonList(name)));
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

    /**
     * Received new metrics from a slave.
     */
    @Subscribe
    public void onNewMetrics(NewMetrics newMetrics)
    {
        scheduler.schedule(() ->
        {
            try
            {
                repository.insertMetrics(newMetrics.getMetrics());
            }
            catch (Throwable e)
            {
                log.error("onNewMetrics(): scheduled task failed: {}", e.getMessage(), e);
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

    private void initializeQueen()
    {
        scheduler.schedule(() ->
        {
            try
            {
                val nowNanos = System.nanoTime();

                // restore queen state
                final Stream<Map.Entry<String, InsectState>> lastInsectStates = repository.findAll().stream()
                        .filter(service -> service.getState() != State.DOWN
                                && !isNullOrEmpty(service.getHost())
                                && service.getPort() != null && service.getPort() > 0 && service.getPort() < 65535)
                        .map(service ->
                        {
                            val state = InsectState.builder()
                                    .socketAddress(new InetSocketAddress(service.getHost(), service.getPort()))
                                    .name(service.getName())
                                    .isOutOfService(service.getState() == State.OUT_OF_SERVICE)
                                    .newEpoch(nowNanos, nowNanos)
                                    .build();

                            return new Map.Entry<String, InsectState>()
                            {

                                @Override
                                public String getKey()
                                {
                                    return service.getRoute();
                                }

                                @Override
                                public InsectState getValue()
                                {
                                    return state;
                                }

                                @Override
                                public InsectState setValue(InsectState value)
                                {
                                    throw new UnsupportedOperationException("setValue() is not supported");
                                }
                            };
                        });

                // initialize queen with the previous state for all non-DOWN services
                queen.initializeInsectState(lastInsectStates);

                // set all services state to State.UNKNOWN (to force update by queen or kill)
                val names = repository.findAllNames();
                for (val name : names)
                {
                    val updatedService = Service.builder()
                            .name(name)
                            .state(State.UNKNOWN)
                            .build();

                    repository.insertServiceState(updatedService);
                }

                queen.run();
            }
            catch (Throwable e)
            {
                log.error("initializeQueen(): scheduled task failed: {}", e.getMessage(), e);
            }
        });
    }
}