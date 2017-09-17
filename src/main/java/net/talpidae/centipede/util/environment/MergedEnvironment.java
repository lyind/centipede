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

package net.talpidae.centipede.util.environment;

import com.google.common.net.HostAndPort;

import net.talpidae.base.insect.config.QueenSettings;
import net.talpidae.base.util.network.NetworkUtil;
import net.talpidae.centipede.bean.configuration.Configuration;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.val;


/**
 * Allow usage of environment variables merged with those defined by init.json plus some custom ones defined at runtime.
 * <p>
 * Custom variables:
 * <p>
 * QUEEN_REMOTE  - insect queen host:port, example: 127.0.0.1:13300
 */
@Singleton
public class MergedEnvironment
{
    private final static String QUEEN_REMOTE_KEY = "QUEEN_REMOTE";

    private final Map<String, String> environment;


    @Inject
    public MergedEnvironment(QueenSettings queenSettings, NetworkUtil networkUtil, Configuration config)
    {
        // we re-export all environment variables
        val environment = new HashMap<String, String>(System.getenv());

        // add some custom variables
        val bindAddress = queenSettings.getBindAddress();
        val queenAddress = networkUtil.getReachableLocalAddress(bindAddress.getAddress(), bindAddress.getAddress());

        environment.put(QUEEN_REMOTE_KEY, HostAndPort.fromParts(queenAddress.getHostAddress(), bindAddress.getPort()).toString());

        // add values from configuration (init.json)
        environment.putAll(config.getEnvironment());

        this.environment = environment;
    }


    /**
     * Return the combined environment, merged with @param additionalVariables.
     */
    public Map<String, String> getEnvironment(Map<String, String> additionalVariables)
    {
        final Map<String, String> merged;
        if (additionalVariables != null && !additionalVariables.isEmpty())
        {
            merged = new HashMap<>(environment);

            merged.putAll(additionalVariables);
        }
        else
        {
            merged = environment;
        }

        return merged;
    }
}