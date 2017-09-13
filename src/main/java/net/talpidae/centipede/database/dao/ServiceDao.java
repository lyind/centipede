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

import net.talpidae.centipede.bean.service.Service;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;


public interface ServiceDao
{
    @RegisterConstructorMapper(Service.class)
    @SqlQuery("SELECT generation,\n"
            + "  name,\n"
            + "  ts,\n"
            + "  retired,\n"
            + "  state,\n"
            + "  targetState,\n"
            + "  kind,\n"
            + "  vmArguments,\n"
            + "  image,\n"
            + "  arguments,\n"
            + "  route,\n"
            + "  proxyPathPrefix,\n"
            + "  pid,\n"
            + "  host,\n"
            + "  port,\n"
            + "  transition\n"
            + "FROM (\n"
            + "  SELECT *, MAX(generation)\n"
            + "  FROM service\n"
            + "  GROUP BY name\n"
            + ") AS snapshot\n"
            + "WHERE NOT retired\n"
            + "ORDER BY name ASC")
    List<Service> findAll();

    @SqlQuery("SELECT name\n"
            + "FROM (\n"
            + "  SELECT name, retired, MAX(generation)\n"
            + "  FROM service\n"
            + "  GROUP BY name\n"
            + ") AS snapshot\n"
            + "WHERE NOT retired\n"
            + "ORDER BY name ASC")
    List<String> findAllNames();

    @SqlQuery("SELECT DISTINCT name\n"
            + "FROM service\n"
            + "ORDER BY name ASC")
    List<String> findAllNamesIncludingRetired();

    @RegisterConstructorMapper(Service.class)
    @SqlQuery("SELECT generation,\n"
            + "  name,\n"
            + "  ts,\n"
            + "  retired,\n"
            + "  state,\n"
            + "  targetState,\n"
            + "  kind,\n"
            + "  vmArguments,\n"
            + "  image,\n"
            + "  arguments,\n"
            + "  route,\n"
            + "  proxyPathPrefix,\n"
            + "  pid,\n"
            + "  host,\n"
            + "  port,\n"
            + "  transition\n"
            + "FROM (\n"
            + "  SELECT *, MAX(generation)\n"
            + "  FROM service\n"
            + "  WHERE name = :name\n"
            + "  GROUP BY name\n"
            + ") AS snapshot\n"
            + "WHERE NOT retired")
    Service findByName(@Bind("name") String name);

    @SqlUpdate("INSERT INTO service (\n"
            + "  generation,\n"
            + "  name,\n"
            + "  retired,\n"
            + "  state,\n"
            + "  targetState,\n"
            + "  kind,\n"
            + "  vmArguments,\n"
            + "  image,\n"
            + "  arguments,\n"
            + "  route,\n"
            + "  proxyPathPrefix,\n"
            + "  pid,\n"
            + "  host,\n"
            + "  port,\n"
            + "  transition\n"
            + ")\n"
            + "  SELECT\n"
            + "    ifnull(maxGeneration, 0) + 1 AS generation,\n"
            + "    ifnull(MAX(:name), :name) AS name,\n" // trick to always get a row returned
            + "    ifnull(:retired, retired) AS retired,\n"
            + "    ifnull(state, 'UNKNOWN'),\n"
            + "    ifnull(ifnull(:targetState, targetState), 'UNKNOWN') AS targetState,\n"
            + "    ifnull(:kind, kind) AS kind,\n"
            + "    ifnull(:vmArguments, vmArguments) AS vmArguments,\n"
            + "    ifnull(:image, image) AS image,\n"
            + "    ifnull(:arguments, arguments) AS arguments,\n"
            + "    route,\n"
            + "    ifnull(:proxyPathPrefix, proxyPathPrefix) AS proxyPathPrefix,\n"
            + "    pid,\n"
            + "    host,\n"
            + "    port,\n"
            + "    transition\n"
            + "  FROM (\n"
            + "    SELECT *, MAX(generation)\n"
            + "    FROM service\n"
            + "    WHERE name = :name\n"
            + "      AND NOT retired\n"
            + "    GROUP BY name\n"
            + "  ) AS snapshot,\n"
            + "  (\n"
            + "    SELECT MAX(generation) AS maxGeneration\n"
            + "    FROM service\n"
            + "    WHERE name = :name\n"
            + "    GROUP BY name\n"
            + "  ) AS maxHistoryGeneration")
    void insertServiceConfiguration(@BindBean Service service);

    @SqlUpdate("INSERT INTO service (\n"
            + "  generation,\n"
            + "  name,\n"
            + "  retired,\n"
            + "  state,\n"
            + "  targetState,\n"
            + "  kind,\n"
            + "  vmArguments,\n"
            + "  image,\n"
            + "  arguments,\n"
            + "  route,\n"
            + "  proxyPathPrefix,\n"
            + "  pid,\n"
            + "  host,\n"
            + "  port,\n"
            + "  transition\n"
            + ")\n"
            + "  SELECT\n"
            + "    ifnull(generation, 0) + 1 AS generation,\n"
            + "    ifnull(MAX(:name), :name) AS name,\n" // trick to always get a row returned
            + "    ifnull(ifnull(:retired, retired), 0) AS retired,\n"
            + "    ifnull(ifnull(:state, state), 'UNKNOWN') AS state,\n"
            + "    ifnull(targetState, 'UNKNOWN') AS targetState,\n"
            + "    kind,\n"
            + "    vmArguments,\n"
            + "    image,\n"
            + "    arguments,\n"
            + "    ifnull(:route, route) AS route,\n"
            + "    proxyPathPrefix,\n"
            + "    ifnull(:pid, pid) AS pid,\n"
            + "    ifnull(:host, host) AS host,\n"
            + "    ifnull(:port, port) AS port,\n"
            + "    ifnull(:transition, transition) AS transition\n"
            + "  FROM (\n"
            + "    SELECT *, MAX(generation)\n"
            + "    FROM service\n"
            + "    WHERE name = :name\n"
            + "    GROUP BY name\n"
            + "  ) AS snapshot")
    void insertServiceState(@BindBean Service service);
}
