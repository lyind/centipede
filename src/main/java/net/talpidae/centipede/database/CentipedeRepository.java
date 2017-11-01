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

package net.talpidae.centipede.database;

import net.talpidae.base.util.performance.Metric;
import net.talpidae.centipede.bean.dependency.Dependency;
import net.talpidae.centipede.bean.metric.MetricStat;
import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.database.dao.DependencyDao;
import net.talpidae.centipede.database.dao.MetricDao;
import net.talpidae.centipede.database.dao.ServiceDao;

import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.List;
import java.util.Optional;

import lombok.val;


public interface CentipedeRepository
{
    @CreateSqlObject
    ServiceDao serviceDao();

    @CreateSqlObject
    MetricDao metricDao();

    @CreateSqlObject
    DependencyDao dependencyDao();


    default void insertServiceConfiguration(Service service)
    {
        serviceDao().insertServiceConfiguration(service);
    }


    default void insertServiceState(Service service)
    {
        serviceDao().insertServiceState(service);
    }


    default void insertNextServiceTransition(Service service)
    {
        val transition = service.getTransition();
        val updatedService = Service.builder()
                .name(service.getName())
                .transition(transition == null ? 0 : transition + 1)
                .build();

        serviceDao().insertServiceState(updatedService);
    }


    default Optional<Service> findServiceByName(String name)
    {
        return Optional.ofNullable(serviceDao().findByName(name));
    }

    default List<Service> findAll()
    {
        return serviceDao().findAll();
    }

    default List<String> findAllNames()
    {
        return serviceDao().findAllNames();
    }

    default List<String> findAllNamesIncludingRetired()
    {
        return serviceDao().findAllNamesIncludingRetired();
    }

    default List<Dependency> findAllDependencies()
    {
        return dependencyDao().findAll();
    }

    @Transaction
    default void setDependencies(String name, Iterable<String> targets)
    {
        val dependencyDao = dependencyDao();

        dependencyDao.deleteDependenciesForName(name);

        dependencyDao.insertChangedDependencies(name, targets);
    }


    @Transaction
    default void insertMetrics(Iterable<Metric> metrics)
    {
        metricDao().insertMetricPaths(metrics);
        metricDao().insertMetrics(metrics);
    }


    @Transaction
    default int deleteMetricsBefore(long newEpochMillies)
    {
        val metricDao = metricDao();

        // first, remove stats
        metricDao.deleteMetricStatsBefore(newEpochMillies);

        // second, remove raw metrics and connected paths
        val removedMetricCount = metricDao.deleteMetricsBefore(newEpochMillies);
        metricDao.deleteOrphanedPaths();

        return removedMetricCount;
    }


    @Transaction
    default int accumulateMetricStatsUpTo(long interval, long limit)
    {
        val metricDao = metricDao();

        Long begin = metricDao.findMaxMetricStatEnd();
        if (begin == null)
        {
            begin = metricDao.findMinMetricTs();
        }

        if (begin != null && begin < limit)
        {
            val end = begin + interval;
            if (end < limit)
            {
                val prefixes = metricDao.findPathPrefixes();
                metricDao.accumulateMetricStatsByPrefixesAndRange(prefixes, begin, end);
                return prefixes.size();
            }
        }

        return 0;
    }


    default List<MetricStat> findMetricStatsByPathPrefixAndRange(String pathPrefix, long begin, long end)
    {
        return metricDao().findMetricStatsByPathPrefixAndRange(pathPrefix, begin, end);
    }

    default List<MetricStat> findMostRecentMetricStatsByPathPrefix(String pathPrefix)
    {
        return metricDao().findMostRecentMetricStatsByPathPrefix(pathPrefix);
    }
}
