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

import com.google.common.eventbus.EventBus;

import net.talpidae.centipede.bean.configuration.Configuration;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.NewMetricStats;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static com.google.common.base.Objects.firstNonNull;


/**
 * Perform certain maintenance operation to keep our database healthy.
 */
@Slf4j
public class MaintenanceTask implements Runnable
{
    /**
     * How close to approach "now" when accumulating (we may drop late metrics if we are to close).
     */
    private static final long ACCUMULATION_SAFETY_MARGIN_MILLIES = TimeUnit.SECONDS.toMillis(10);

    private final CentipedeRepository centipedeRepository;

    private final Configuration config;

    private final EventBus eventBus;


    @Inject
    public MaintenanceTask(CentipedeRepository centipedeRepository, Configuration config, EventBus eventBus)
    {
        this.centipedeRepository = centipedeRepository;
        this.config = config;
        this.eventBus = eventBus;
    }

    /**
     * Metrics accumulation approach:
     * <p>
     * 1. Get path prefixes in range
     * 2. Accumulate by prefix
     * 3. Persist
     */
    @Override
    public void run()
    {
        accumulateMetrics();

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


    private void accumulateMetrics()
    {
        // NOTE: we do NOT run this inside a single transaction because we know our records are immutable
        //       and we want to avoid long running transactions
        final long begin = firstNonNull(centipedeRepository.findMaxMetricStatEnd(), 0L);
        val end = System.currentTimeMillis() - ACCUMULATION_SAFETY_MARGIN_MILLIES;

        centipedeRepository.accumulateMetricStatsByRange(begin, end);

        eventBus.post(new NewMetricStats());
    }
}