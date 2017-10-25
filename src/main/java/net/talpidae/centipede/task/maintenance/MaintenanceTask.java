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

package net.talpidae.centipede.task.maintenance;

import net.talpidae.centipede.bean.configuration.Configuration;
import net.talpidae.centipede.database.CentipedeRepository;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import lombok.val;


/**
 * Perform certain maintenance operation to keep our database healthy.
 */
@Slf4j
public class MaintenanceTask implements Runnable
{
    private final CentipedeRepository centipedeRepository;

    private final Configuration config;


    @Inject
    public MaintenanceTask(CentipedeRepository centipedeRepository, Configuration config)
    {
        this.centipedeRepository = centipedeRepository;
        this.config = config;
    }


    @Override
    public void run()
    {
        // purge old metrics
        val newEpoch = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(config.getKeepMetricsMinutes());
        log.debug("removing all metric before: {}", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(newEpoch)));
        try
        {
            val deletedMetrics = centipedeRepository.deleteMetricsBefore(newEpoch);
            log.debug("removed {} metrics", deletedMetrics);
        }
        catch (Throwable t)
        {
            log.error("failed to delete metrics", t);
        }

        // delete orphaned paths
        try
        {
            val deletedPaths = centipedeRepository.deleteOrphanedPaths();
            log.debug("removed {} orphaned paths", deletedPaths);
        }
        catch (Throwable t)
        {
            log.error("failed to delete orphaned paths", t);
        }
    }
}