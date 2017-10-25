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

package net.talpidae.centipede.database.dao;

import net.talpidae.base.util.performance.Metric;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;


public interface MetricDao
{
    @SqlBatch("INSERT OR IGNORE INTO metric_path (path) VALUES (:metric.path)")
    void insertMetricPaths(@BindBean("metric") Iterable<Metric> metric);

    @SqlBatch("INSERT OR IGNORE INTO metric (ts, pathId, value)\n"
            + "  SELECT CAST(((:metric.ts * 1000000) + abs(random() % 1000000)) AS INTEGER) AS ts, id AS pathId, :metric.value AS value\n"
            + "  FROM metric_path\n"
            + "  WHERE path = :metric.path")
    void insertMetrics(@BindBean("metric") Iterable<Metric> metric);

    @SqlUpdate("DELETE FROM metric WHERE ts < (:newEpochMillies * 1000000)")
    int deleteMetricsBefore(@Bind("newEpochMillies") long newEpochMillies);

    @SqlUpdate("DELETE FROM metric_path WHERE NOT EXISTS (SELECT 1 FROM metric WHERE id = pathId)")
    int deleteOrphanedPaths();

    @SqlQuery("SELECT mp1.path, m1.ts, m1.value\n"
            + "FROM metric m1\n"
            + "INNER JOIN metric_path mp1 ON (mp1.id = m1.pathId)\n"
            + "WHERE mp1.path LIKE :pathPrefix || '%'\n"
            + "AND (m1.ts >= (:begin * 1000000) AND m1.ts <= (:end * 1000000))")
    List<Metric> findMetricsByPathPrefixAndRange(@Bind("pathPrefix") String prefix, @Bind("begin") long beginTimeMillies, @Bind("end") long endTimeMillies);
}