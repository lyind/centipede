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

import com.google.inject.Singleton;

import net.talpidae.base.database.DefaultDataBaseConfig;
import net.talpidae.base.database.ProxyDataSourceConfigurer;

import org.jdbi.v3.core.spi.JdbiPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.Getter;


@Singleton
@Getter
public class CentipedeDefaultDataBaseConfig extends DefaultDataBaseConfig
{
    private final int maximumPoolSize = Math.max(3, Runtime.getRuntime().availableProcessors() / 4);

    private final String jdbcUrl = "jdbc:sqlite:centipede.db";

    private final String userName = "root";

    private final String password = "bar";

    private final String poolName = "CentipedeDatabasePool";

    private final String driverClassName = org.sqlite.JDBC.class.getName();

    private final String connectionTestQuery = "SELECT 1";

    private final int maxLifetime = (int) TimeUnit.MINUTES.toMillis(8);

    private final int idleTimeout = (int) TimeUnit.SECONDS.toMillis(45);

    private final Map<String, String> dataSourceProperties = new HashMap<>();

    private final Collection<JdbiPlugin> extraPlugins = Collections.emptyList();

    private final ProxyDataSourceConfigurer proxyDataSourceConfigurer = null;


    public CentipedeDefaultDataBaseConfig()
    {
        dataSourceProperties.put("cachePrepStmts", "true");
        dataSourceProperties.put("prepStmtCacheSize", "250");
        dataSourceProperties.put("prepStmtCacheSqlLimit", "2048");
    }
}
