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
            + "    ifnull(generation, 0) + 1 AS generation,\n"
            + "    :name,\n"
            + "    :retired,\n"
            + "    ifnull(state, 'UNKNOWN') AS state,\n"
            + "    :targetState,\n"
            + "    :kind,\n"
            + "    :vmArguments,\n"
            + "    :image,\n"
            + "    :arguments,\n"
            + "    route,\n"
            + "    :proxyPathPrefix,\n"
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
    void insertExposedFields(@BindBean Service service);

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
            + "    ifnull(generation, 0) + 1 AS generation,\n"
            + "    :name,\n"
            + "    :retired,\n"
            + "    :state,\n"
            + "    ifnull(targetState, 'UNKNOWN') AS targetState,\n"
            + "    kind,\n"
            + "    vmArguments,\n"
            + "    image,\n"
            + "    arguments,\n"
            + "    :route,\n"
            + "    proxyPathPrefix,\n"
            + "    :pid,\n"
            + "    :host,\n"
            + "    :port\n"
            + "  FROM\n"
            + "  service s1\n"
            + "  WHERE NOT retired\n"
            + "    AND name = :name\n"
            + "    AND generation = (\n"
            + "      SELECT MAX(s2.generation)\n"
            + "      FROM service s2\n"
            + "      WHERE s2.name = :name\n"
            + "  )\n")
    void insertOrUpdateServiceState(@BindBean Service service);
}
