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

package net.talpidae.centipede.task.init;

import com.google.common.collect.Sets;

import net.talpidae.centipede.bean.configuration.Configuration;
import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.bean.service.State;
import net.talpidae.centipede.database.CentipedeRepository;

import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import lombok.val;


/**
 * Initialize service database from init.json in the working directory.
 */
@Slf4j
public class InitTask implements Runnable
{
    private final CentipedeRepository centipedeRepository;

    private final Configuration config;


    @Inject
    public InitTask(CentipedeRepository centipedeRepository, Configuration config)
    {
        this.centipedeRepository = centipedeRepository;
        this.config = config;
    }

    @Override
    public void run()
    {
        final List<Service> initialServices = config.getInitialServices();

        val existingServiceNames = Sets.newHashSet(centipedeRepository.findAllNamesIncludingRetired());

        log.debug("found {} initial service definitions", initialServices.size());

        int skippedCount = 0;
        for (Service service : initialServices)
        {
            if (existingServiceNames.contains(service.getName()))
            {
                ++skippedCount;
                continue;
            }

            val sanitizedService = sanitizeService(service);
            try
            {
                centipedeRepository.insertServiceConfiguration(sanitizedService);
                centipedeRepository.insertServiceState(Service.builder().name(sanitizedService.getName()).state(State.DOWN).build());
                log.debug("added service: {}", sanitizedService.getName());
            }
            catch (Exception e)
            {
                log.error("failed to add initial service: {}: {}", service.getName(), e.getMessage());
                ++skippedCount;
            }
        }

        log.debug("addded {} services, skipped {}", initialServices.size() - skippedCount, skippedCount);
    }


    private Service sanitizeService(Service service)
    {
        return Service.builder()
                .name(service.getName())
                .kind(service.getKind())
                .targetState(service.getTargetState())
                .image(service.getImage())
                .arguments(service.getArguments())
                .vmArguments(service.getVmArguments())
                .proxyPathPrefix(service.getProxyPathPrefix())
                .build();
    }
}
