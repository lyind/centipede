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

package net.talpidae.centipede.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.talpidae.base.util.auth.Credentials;
import net.talpidae.centipede.bean.dependency.Dependency;
import net.talpidae.centipede.bean.frozen.FrozenState;
import net.talpidae.centipede.bean.metric.MetricStatView;
import net.talpidae.centipede.bean.service.Service;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


/**
 * Main communication bean. This is used in all communication between server and client.
 * <p>
 * Think of top-level members of Api as subjects.
 */
@Getter
@EqualsAndHashCode
@ToString
public class Api
{
    /**
     * Authentication token.
     */
    private final String token;

    /**
     * User credentials, ask server to perform authentication and return a valid token.
     */
    private final Credentials credentials;

    /**
     * Main topic, manage services.
     */
    private final Iterable<Service> services;

    /**
     * List dependencies between services (by service name).
     */
    private final Iterable<Dependency> dependencies;

    /**
     * Metrics query and result.
     */
    private final MetricStatView metricStats;

    /**
     * Error message from the server to the client.
     */
    private final String error;

    /**
     * Indicates broadcast queue overflow.
     */
    private final Boolean overflow;

    /**
     * Is the state machine frozen (no services start/stop allowed)?
     */
    private final FrozenState frozen;


    @Builder(toBuilder = true)
    @JsonCreator
    private Api(@JsonProperty("token") String token,
                @JsonProperty("credentials") Credentials credentials,
                @JsonProperty("services") Iterable<Service> services,
                @JsonProperty("dependencies") Iterable<Dependency> dependencies,
                @JsonProperty("metricStats") MetricStatView metricStats,
                @JsonProperty("error") String error,
                @JsonProperty("overflow") Boolean overflow,
                @JsonProperty("frozen") FrozenState frozen)
    {
        this.token = token;
        this.credentials = credentials;
        this.services = services;
        this.dependencies = dependencies;
        this.metricStats = metricStats;
        this.error = error;
        this.overflow = overflow;
        this.frozen = frozen;
    }
}