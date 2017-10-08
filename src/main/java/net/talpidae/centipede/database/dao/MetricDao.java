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

import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;


public interface MetricDao
{
    @SqlBatch("INSERT OR IGNORE INTO metric_path (path) VALUES (:path);\n"
            + "INSERT INTO metric (pathId, ts, value) (\n"
            + "  SELECT id, :ts, :value\n"
            + "  FROM metric_path\n"
            + "  WHERE path = :path"
            + ")")
    void insertMetrics(@BindBean("metric") Iterable<Metric> metric);
}