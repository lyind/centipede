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

import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.service.wrapper.Call;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.Getter;


@Getter
@Singleton
public class Dependencies implements CallHandler
{
    private final Phase phase = Phase.HANDLE;

    private final CentipedeRepository centipedeRepository;

    @Inject
    public Dependencies(CentipedeRepository centipedeRepository)
    {
        this.centipedeRepository = centipedeRepository;
    }


    @Override
    public void accept(Call call)
    {
        if (call.getRequest().getDependencies() != null)
        {
            // fulfill dependencies request
            call.getResponse().dependencies(centipedeRepository.findAllDependencies());
        }
    }
}