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
import lombok.Getter;
import lombok.val;
import net.talpidae.centipede.bean.service.Api;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.ServicesModified;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;


@Getter
@Singleton
public class Services implements CallHandler
{
    private final Phase phase = Phase.HANDLE;

    private final CentipedeRepository centipedeRepository;

    private final EventBus eventBus;


    @Inject
    public Services(CentipedeRepository centipedeRepository, EventBus eventBus)
    {
        this.centipedeRepository = centipedeRepository;
        this.eventBus = eventBus;
    }


    @Override
    public Api apply(Api request)
    {
        if (request != null && request.getServices() != null)
        {
            val serviceIterator = request.getServices().iterator();

            // accept changes if there are any
            if (serviceIterator.hasNext())
            {
                val names = new ArrayList<String>();
                while (serviceIterator.hasNext())
                {
                    val service = serviceIterator.next();

                    // TODO Add some validation here.

                    centipedeRepository.insertServiceConfiguration(service);
                    names.add(service.getName());
                }

                eventBus.post(new ServicesModified(Collections.unmodifiableList(names)));
            }
            else
            {
                // fulfill list request
                request.setServices(centipedeRepository.findAll());
            }
        }

        return request;
    }
}
