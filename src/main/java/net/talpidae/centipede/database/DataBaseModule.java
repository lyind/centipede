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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.val;
import org.jdbi.v3.core.Jdbi;

import javax.sql.DataSource;


/**
 * Provides centipede database functionality.
 */
public class DataBaseModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(DataSource.class).to(HikariDataSource.class);
        bind(ManagedSchema.class).to(FlywayManagedSchema.class);

        requireBinding(DataBaseConfig.class);
    }


    @Provides
    @Singleton
    public CentipedeRepository centipedeRepositoryProvider(Jdbi jdbi)
    {
        return jdbi.onDemand(CentipedeRepository.class);
    }


    @Provides
    @Singleton
    public Jdbi jdbiProvider(ManagedSchema managedSchema)
    {
        return Jdbi.create(managedSchema.migrate());
    }


    @Provides
    @Singleton
    public HikariDataSource hikariDataSourceProvider(HikariConfig hikariConfig)
    {
        final HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        return dataSource;
    }


    @Provides
    @Singleton
    public HikariConfig hikariConfigProvider(DataBaseConfig dataBaseConfig)
    {
        final HikariConfig config = new HikariConfig();

        config.setPoolName("CentipedeDatabasePool");
        config.setDriverClassName(org.sqlite.JDBC.class.getName());
        config.setJdbcUrl(dataBaseConfig.getJdbcUrl());
        config.setConnectionTestQuery("SELECT 1");
        config.setMaxLifetime(72000); // 72s
        config.setIdleTimeout(45000); // 45s Sec
        config.setMaximumPoolSize(dataBaseConfig.getMaximumPoolSize());

        config.setUsername(dataBaseConfig.getUserName());
        config.setPassword(dataBaseConfig.getPassword());

        for (val propertyEntry : dataBaseConfig.getDataSourceProperties().entrySet())
        {
            config.addDataSourceProperty(propertyEntry.getKey(), propertyEntry.getValue());
        }

        return config;
    }
}
