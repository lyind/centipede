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
    @SqlQuery("SELECT *\n"
            + "FROM service\n"
            + "WHERE NOT retired\n"
            + "  AND generation = (\n"
            + "    SELECT MAX(s2.generation)\n"
            + "    FROM service s2\n"
            + "    WHERE s2.name = name\n"
            + "  )\n"
            + "ORDER BY name ASC")
    List<Service> findAll();

    @SqlQuery("SELECT name\n"
            + "FROM service\n"
            + "WHERE NOT retired\n"
            + "  AND generation = (\n"
            + "    SELECT MAX(s2.generation)\n"
            + "    FROM service s2\n"
            + "    WHERE s2.name = name\n"
            + "  )\n"
            + "ORDER BY name ASC")
    List<String> findAllNames();

    @RegisterConstructorMapper(Service.class)
    @SqlQuery("SELECT *\n"
            + "FROM service\n"
            + "WHERE NOT retired\n"
            + "  AND name = :name\n"
            + "  AND generation = (\n"
            + "    SELECT MAX(s2.generation)\n"
            + "    FROM service s2\n"
            + "    WHERE s2.name = name\n"
            + "  )\n")
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
            + "  port\n"
            + ")\n"
            + "  SELECT\n"
            + "    ifnull(s1.generation, 0) + 1 AS generation,\n"
            + "    ifnull(MAX(:name), :name) AS name,\n" // trick to always get a row returned
            + "    ifnull(:retired, s1.retired) AS retired,\n"
            + "    ifnull(state, 'UNKNOWN'),\n"
            + "    ifnull(ifnull(:targetState, s1.targetState), 'UNKNOWN') AS targetState,\n"
            + "    ifnull(:kind, s1.kind) AS kind,\n"
            + "    ifnull(:vmArguments, s1.vmArguments) AS vmArguments,\n"
            + "    ifnull(:image, s1.image) AS image,\n"
            + "    ifnull(:arguments, s1.arguments) AS arguments,\n"
            + "    route,\n"
            + "    ifnull(:proxyPathPrefix, s1.proxyPathPrefix) AS proxyPathPrefix,\n"
            + "    pid,\n"
            + "    host,\n"
            + "    port\n"
            + "  FROM\n"
            + "  service s1\n"
            + "  WHERE NOT retired\n"
            + "    AND name = :name\n"
            + "    AND generation = (\n"
            + "      SELECT MAX(s2.generation)\n"
            + "      FROM service s2\n"
            + "      WHERE s2.name = :name\n"
            + "  )\n")
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
            + "  port\n"
            + ")\n"
            + "  SELECT\n"
            + "    ifnull(s1.generation, 0) + 1 AS generation,\n"
            + "    ifnull(MAX(:name), :name) AS name,\n" // trick to always get a row returned
            + "    ifnull(ifnull(:retired, s1.retired), 0) AS retired,\n"
            + "    ifnull(ifnull(:state, s1.state), 'UNKNOWN') AS state,\n"
            + "    ifnull(s1.targetState, 'UNKNOWN') AS targetState,\n"
            + "    s1.kind,\n"
            + "    s1.vmArguments,\n"
            + "    s1.image,\n"
            + "    s1.arguments,\n"
            + "    ifnull(:route, s1.route) AS route,\n"
            + "    s1.proxyPathPrefix,\n"
            + "    ifnull(:pid, s1.pid) AS pid,\n"
            + "    ifnull(:host, s1.host) AS host,\n"
            + "    ifnull(:port, s1.port) AS port\n"
            + "  FROM\n"
            + "  service s1\n"
            + "  WHERE NOT retired\n"
            + "    AND name = :name\n"
            + "    AND generation = (\n"
            + "      SELECT MAX(s2.generation)\n"
            + "      FROM service s2\n"
            + "      WHERE s2.name = :name\n"
            + "  )\n")
    void insertServiceState(@BindBean Service service);
}
