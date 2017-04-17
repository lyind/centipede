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


@RegisterConstructorMapper(Service.class)
public interface ServiceDao
{
    @SqlQuery("SELECT *\n"
            + "FROM service s1\n"
            + "WHERE NOT retired\n"
            + "  AND s1.generation = (\n"
            + "    SELECT MAX(s2.generation)\n"
            + "    FROM service s2\n"
            + "    WHERE s2.name = s1.name\n"
            + "  )\n"
            + "ORDER BY name ASC")
    Iterable<Service> findAll();

    @SqlQuery("SELECT *\n"
            + "FROM service s1\n"
            + "WHERE NOT retired\n"
            + "  AND s1.name = :name\n"
            + "  AND s1.generation = (\n"
            + "    SELECT MAX(s2.generation)\n"
            + "    FROM service s2\n"
            + "    WHERE s2.name = s1.name\n"
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
            + "  VALUES (\n"
            + "    :generation + 1,\n"
            + "    :name,\n"
            + "    :retired,\n"
            + "    :state,\n"
            + "    :targetState,\n"
            + "    :kind,\n"
            + "    :vmArguments,\n"
            + "    :image,\n"
            + "    :arguments,\n"
            + "    :route,\n"
            + "    :proxyPathPrefix,\n"
            + "    :pid,\n"
            + "    :host,\n"
            + "    :port\n"
            + "  )\n")
    void insert(@BindBean Service service);
}
