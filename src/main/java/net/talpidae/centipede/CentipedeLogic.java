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

import com.google.common.eventbus.Subscribe;
import lombok.val;
import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.bean.service.State;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.NewMapping;

import javax.inject.Inject;


public class CentipedeLogic
{
    private final CentipedeRepository repository;


    @Inject
    public CentipedeLogic(CentipedeRepository repository)
    {
        this.repository = repository;
    }


    /**
     * Received a mapping for a previously unknown service. Persist if not done so already.
     */
    @Subscribe
    public void onNewMapping(NewMapping newMapping)
    {
        val mapping = newMapping.getMapping();

        val service = repository.findServiceByName(mapping.getName());
        if (!service.isPresent())
        {
            val newService = Service.builder()
                    .name(mapping.getName())
                    .state(State.UP)
                    .targetState(State.UNKNOWN)
                    .route(mapping.getRoute())
                    .socketAddress(mapping.getSocketAddress())
                    .build();

            repository.createService(newService);
        }
    }
}
