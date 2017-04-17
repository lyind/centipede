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

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.val;
import net.talpidae.base.util.BaseArguments;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;


@Getter
public class DefaultDataBaseConfig implements DataBaseConfig
{
    private static final Integer DEFAULT_MAXIMUM_POOL_SIZE = Math.max(8, Runtime.getRuntime().availableProcessors());

    private final int maximumPoolSize;

    private final String jdbcUrl;

    private final String userName;

    private final String password;

    private final Map<String, String> dataSourceProperties = new HashMap<>();


    @Inject
    public DefaultDataBaseConfig(BaseArguments baseArguments)
    {
        val parser = baseArguments.getOptionParser();
        val maximumPoolSizeOption = parser.accepts("db.maximumPoolSize").withRequiredArg().ofType(Integer.class).defaultsTo(DEFAULT_MAXIMUM_POOL_SIZE);
        val jdbcUrlOption = parser.accepts("db.jdbc.url").withRequiredArg().defaultsTo("jdbc:sqlite:centipede.db");
        val userNameOption = parser.accepts("db.username").withRequiredArg().defaultsTo("root");
        val passwordOption = parser.accepts("db.password").withRequiredArg().defaultsTo("bar");
        val dataSourcePropertiesOption = parser.accepts("db.dataSourceProperty").withRequiredArg();
        val options = baseArguments.parse();

        maximumPoolSize = options.valueOf(maximumPoolSizeOption);
        jdbcUrl = options.valueOf(jdbcUrlOption);
        userName = options.valueOf(userNameOption);
        password = options.valueOf(passwordOption);

        // put some default properties (
        dataSourceProperties.put("cachePrepStmts", "true");
        dataSourceProperties.put("prepStmtCacheSize", "250");
        dataSourceProperties.put("prepStmtCacheSqlLimit", "2048");
        for (val dataSourceProperty : options.valuesOf(dataSourcePropertiesOption))
        {
            val propertyParts = dataSourceProperty.split("=");
            try
            {
                val key = propertyParts[0];
                val value = propertyParts[1];
                if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(value))
                {
                    dataSourceProperties.put(key, value);
                    continue;
                }
            }
            catch (ArrayIndexOutOfBoundsException | NumberFormatException e)
            {
                // throw below
            }

            throw new IllegalArgumentException("invalid key=value pair specified for db.dataSourceProperty: " + dataSourceProperty);
        }
    }
}
