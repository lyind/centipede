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
import net.talpidae.centipede.bean.metric.MetricStat;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
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
    void deleteOrphanedPaths();

    @SqlUpdate("DELETE FROM metric_stat WHERE \"begin\" < :newEpochMillies AND \"end\" <= :newEpochMillies")
    void deleteMetricStatsBefore(@Bind("newEpochMillies") long newEpochMillies);

    @RegisterConstructorMapper(Metric.class)
    @SqlQuery("SELECT mp1.path, m1.ts, m1.value\n"
            + "FROM metric m1\n"
            + "INNER JOIN metric_path mp1 ON (mp1.id = m1.pathId)\n"
            + "WHERE mp1.path LIKE :pathPrefix || '%'\n"
            + "  AND (m1.ts >= (:begin * 1000000) AND m1.ts < (:end * 1000000))")
    List<Metric> findMetricsByPathPrefixAndRange(@Bind("pathPrefix") String prefix, @Bind("begin") long beginTimeMillies, @Bind("end") long endTimeMillies);


    @SqlQuery("SELECT COALESCE(\n"
            + "	-- has a leading slash and following slash\n"
            + "	substr(mp1.path, COALESCE(nullif(instr(substr(mp1.path, 1, 1), '/'), 0), 1), nullif(instr(substr(mp1.path, 2), '/'), 0)),\n"
            + "	-- has a following slash\n"
            + "	substr(mp1.path, 1, nullif(instr(substr(mp1.path, 2), '/'), 0)),\n"
            + "	-- take whole path\n"
            + "	mp1.path\n"
            + "  ) AS pathPrefix\n"
            + "FROM metric_path mp1\n"
            + "GROUP BY pathPrefix")
    List<String> findPathPrefixes();


    @SqlQuery("SELECT MAX(ms1.end) FROM metric_stat ms1")
    Long findMaxMetricStatEnd();


    @SqlQuery("SELECT (MIN(ts) / 1000000) AS ts FROM metric")
    Long findMinMetricTs();


    @SqlQuery("SELECT (MAX(ts) / 1000000) AS ts FROM metric")
    Long findMaxMetricTs();


    @SqlBatch("WITH\n"
            + "    metric_period AS (\n"
            + "        SELECT path, ts, value\n"
            + "        FROM metric\n"
            + "        INNER JOIN metric_path ON (id = pathId)\n"
            + "        WHERE path LIKE :pathPrefix || '%'\n"
            + "            AND (ts >= (:begin * 1000000) AND ts < (:end * 1000000))\n"
            + "    ),\n"
            + "    metric_period_status AS (\n"
            + "        SELECT CAST(round(value) AS INT) AS status\n"
            + "        FROM metric_period\n"
            + "        WHERE path LIKE '%/status'\n"
            + "    ),\n"
            + "    metric_period_duration AS (\n"
            + "        SELECT round(value / 1000000000.0, 3) AS duration -- seconds with millies precision\n"
            + "        FROM metric_period\n"
            + "        WHERE path LIKE '%/duration'\n"
            + "    ),\n"
            + "    metric_period_max_heap AS (\n"
            + "        SELECT round(MAX(value), 1) AS maxHeap\n"
            + "        FROM metric_period\n"
            + "        WHERE path LIKE '%/heapCommitted'\n"
            + "    ),\n"
            + "    metric_period_max_non_heap AS (\n"
            + "        SELECT round(MAX(value), 1) AS maxNonHeap\n"
            + "        FROM metric_period\n"
            + "        WHERE path LIKE '%/nonHeapCommitted'\n"
            + "    ),\n"
            + "    metric_period_status_count AS (\n"
            + "        SELECT nullif(COUNT(1), 0) AS total\n"
            + "        FROM metric_period_status\n"
            + "    ),\n"
            + "    metric_period_status_count_2xx AS (\n"
            + "        SELECT CAST(COUNT(1) AS REAL) AS status2xxCount\n"
            + "        FROM metric_period_status\n"
            + "        WHERE status >= 200 AND status < 300\n"
            + "    ),\n"
            + "    metric_period_status_count_3xx AS (\n"
            + "        SELECT CAST(COUNT(1) AS REAL) AS status3xxCount\n"
            + "        FROM metric_period_status\n"
            + "        WHERE status >= 300 AND status < 400\n"
            + "    ),\n"
            + "    metric_period_status_count_4xx AS (\n"
            + "        SELECT CAST(COUNT(1) AS REAL) AS status4xxCount\n"
            + "        FROM metric_period_status\n"
            + "        WHERE status >= 400 AND status < 500\n"
            + "    ),\n"
            + "    metric_period_status_count_5xx AS (\n"
            + "        SELECT CAST(COUNT(1) AS REAL) AS status5xxCount\n"
            + "        FROM metric_period_status\n"
            + "        WHERE status >= 500 AND status < 600\n"
            + "    )\n"
            + "INSERT OR IGNORE INTO metric_stat (\n"
            + "    pathPrefix,\n"
            + "    begin,\n"
            + "    end,\n"
            + "    total,\n"
            + "    minTime,\n"
            + "    avgTime,\n"
            + "    maxTime,\n"
            + "    status2xx,\n"
            + "    status3xx,\n"
            + "    status4xx,\n"
            + "    status5xx,\n"
            + "    maxHeap,\n"
            + "    maxNonHeap\n"
            + ")\n"
            + "SELECT\n"
            + "    :pathPrefix AS pathPrefix,\n"
            + "    :begin AS \"begin\",\n"
            + "    :end AS \"end\",\n"
            + "    ifnull((SELECT total FROM metric_period_status_count), 0.0) AS total,\n"
            + "    ifnull((SELECT MIN(duration) FROM metric_period_duration), 0.0) AS minTime,\n"
            + "    ifnull((SELECT round(AVG(duration), 3) FROM metric_period_duration), 0.0) AS avgTime,\n"
            + "    ifnull((SELECT MAX(duration) FROM metric_period_duration), 0.0) AS maxTime,\n"
            + "    ifnull(round(((SELECT status2xxCount FROM metric_period_status_count_2xx) / (SELECT total FROM metric_period_status_count)), 4), 1.0) AS status2xx,\n"
            + "    ifnull(round(((SELECT status3xxCount FROM metric_period_status_count_3xx) / (SELECT total FROM metric_period_status_count)), 4), 0.0) AS status3xx,\n"
            + "    ifnull(round(((SELECT status4xxCount FROM metric_period_status_count_4xx) / (SELECT total FROM metric_period_status_count)), 4), 0.0) AS status4xx,\n"
            + "    ifnull(round(((SELECT status5xxCount FROM metric_period_status_count_5xx) / (SELECT total FROM metric_period_status_count)), 4), 0.0) AS status5xx,\n"
            + "    ifnull((SELECT maxHeap FROM metric_period_max_heap), 0.0) AS maxHeap,\n"
            + "    ifnull((SELECT maxNonHeap FROM metric_period_max_non_heap), 0.0) AS maxNonHeap")
    void accumulateMetricStatsByPrefixesAndRange(@Bind("pathPrefix") Iterable<String> pathPrefix, @Bind("begin") long beginTimeMillies, @Bind("end") long endTimeMillies);


    @RegisterConstructorMapper(MetricStat.class)
    @SqlQuery("SELECT\n"
            + "  ms1.pathPrefix,\n"
            + "  ms1.begin,\n"
            + "  ms1.end,\n"
            + "  ms1.total,\n"
            + "  ms1.minTime,\n"
            + "  ms1.avgTime,\n"
            + "  ms1.maxTime,\n"
            + "  ms1.status2xx,\n"
            + "  ms1.status3xx,\n"
            + "  ms1.status4xx,\n"
            + "  ms1.status5xx,\n"
            + "  ms1.maxHeap,\n"
            + "  ms1.maxNonHeap\n"
            + "FROM metric_stat ms1\n"
            + "WHERE (:pathPrefix IS NULL OR ms1.pathPrefix = :pathPrefix)\n"
            + "  AND (ms1.begin >= :begin AND ms1.end <= :end)\n"
            + "ORDER BY ms1.pathPrefix ASC")
    List<MetricStat> findMetricStatsByPathPrefixAndRange(@Bind("pathPrefix") String pathPrefix, @Bind("begin") long beginTimeMillies, @Bind("end") long endTimeMillies);


    @RegisterConstructorMapper(MetricStat.class)
    @SqlQuery("SELECT\n"
            + "    ms2.pathPrefix,\n"
            + "    ms2.begin,\n"
            + "    ms2.end,\n"
            + "    ms2.total,\n"
            + "    ms2.minTime,\n"
            + "    ms2.avgTime,\n"
            + "    ms2.maxTime,\n"
            + "    ms2.status2xx,\n"
            + "    ms2.status3xx,\n"
            + "    ms2.status4xx,\n"
            + "    ms2.status5xx,\n"
            + "    ms2.maxHeap,\n"
            + "    ms2.maxNonHeap\n"
            + "FROM metric_stat ms2\n"
            + "INNER JOIN (\n"
            + "    SELECT ms1.pathPrefix, MAX(ms1.begin) AS \"begin\"\n"
            + "    FROM metric_stat ms1\n"
            + "    WHERE :pathPrefix IS NULL OR ms1.pathPrefix = :pathPrefix\n"
            + "    GROUP BY ms1.pathPrefix\n"
            + "  ) AS ms3\n"
            + "  ON (ms2.pathPrefix = ms3.pathPrefix AND ms2.begin = ms3.begin)\n"
            + "ORDER BY ms2.pathPrefix ASC")
    List<MetricStat> findMostRecentMetricStatsByPathPrefix(@Bind("pathPrefix") String pathPrefix);
}