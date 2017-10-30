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

import net.talpidae.centipede.bean.dependency.Dependency;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;


public interface DependencyDao
{
    @RegisterConstructorMapper(Dependency.class)
    @SqlQuery("SELECT d1.source, s3.name AS target, s1.state AS sourceState, s3.state AS targetState\n"
            + "FROM dependency d1\n"
            + "INNER JOIN (\n"
            + "  SELECT MAX(generation), s2.name, state\n"
            + "  FROM service s2\n"
            + "  GROUP BY s2.name\n"
            + "  ) AS s1\n"
            + "  ON (d1.source = s1.name)\n"
            + "INNER JOIN (\n"
            + "  SELECT MAX(generation), s4.name, s4.route, state\n"
            + "  FROM service s4\n"
            + "  GROUP BY s4.name\n"
            + "  ) AS s3\n"
            + "  ON (d1.target = s3.route)\n"
            + "ORDER BY d1.source ASC")
    List<Dependency> findAll();

    @SqlBatch("INSERT INTO dependency (source, target)\n"
            + "  VALUES (:source, :target)")
    void insertChangedDependencies(@Bind("source") String source, @Bind("target") Iterable<String> targets);

    @SqlUpdate("DELETE FROM dependency\n"
            + "WHERE source = :source")
    void deleteDependenciesForName(@Bind("source") String source);
}