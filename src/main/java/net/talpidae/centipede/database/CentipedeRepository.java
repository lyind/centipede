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

import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.database.dao.ServiceDao;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.List;
import java.util.Optional;


public interface CentipedeRepository
{
    @CreateSqlObject
    ServiceDao serviceDao();

    @Transaction
    default void insertServiceConfiguration(Service service)
    {
        serviceDao().insertServiceConfiguration(service);
    }


    @Transaction
    default void insertServiceState(Service service)
    {
        serviceDao().insertServiceState(service);
    }


    @Transaction
    default Optional<Service> findServiceByName(String name)
    {
        return Optional.ofNullable(serviceDao().findByName(name));
    }

    @Transaction
    default List<Service> findAll()
    {
        return serviceDao().findAll();
    }
}
