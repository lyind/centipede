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

import net.talpidae.base.insect.SyncQueen;
import net.talpidae.base.insect.config.QueenSettings;
import net.talpidae.base.insect.message.payload.Mapping;
import net.talpidae.base.insect.message.payload.Metrics;
import net.talpidae.base.insect.state.InsectState;
import net.talpidae.base.server.ServerConfig;
import net.talpidae.centipede.event.DependenciesChanged;
import net.talpidae.centipede.event.NewMapping;
import net.talpidae.centipede.event.NewMetrics;

import java.net.InetSocketAddress;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class CentipedeSyncQueen extends SyncQueen
{
    private final EventBus eventBus;

    private final ServerConfig serverConfig;


    @Inject
    public CentipedeSyncQueen(QueenSettings settings, ServerConfig serverConfig, EventBus eventBus)
    {
        super(settings);

        this.eventBus = eventBus;
        this.serverConfig = serverConfig;
    }


    @Override
    public void run()
    {
        getSettings().setBindAddress(new InetSocketAddress(serverConfig.getHost(), serverConfig.getPort()));

        super.run();
    }


    @Override
    protected void postHandleMapping(InsectState state, Mapping mapping, boolean isNewMapping, boolean isDependencyMapping)
    {
        super.postHandleMapping(state, mapping, isNewMapping, isDependencyMapping);

        // update DB state
        if (isNewMapping)
        {
            eventBus.post(new NewMapping(mapping));
        }
    }


    @Override
    protected void handleDependenciesChanged(InsectState insectState)
    {
        eventBus.post(new DependenciesChanged(insectState.getName(), insectState.getDependencies()));
    }


    @Override
    protected void handleMetrics(Metrics metrics)
    {
        eventBus.post(new NewMetrics(metrics.getMetrics()));
    }
}