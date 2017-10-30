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

import net.talpidae.base.util.auth.Credentials;
import net.talpidae.centipede.bean.dependency.Dependency;
import net.talpidae.centipede.bean.metric.MetricStatView;
import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.service.wrapper.CallContext;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


/**
 * Main communication bean. This is used in all communication between server and client.
 * <p>
 * Think of top-level members of Api as subjects.
 */
@Getter
@Setter
@Builder
public class Api
{
    /**
     * Request context. Only interesting while an event is processed.
     */
    //@Builder.Default
    private final transient CallContext context = new CallContext();

    /**
     * Authentication token.
     */
    private String token;

    /**
     * User credentials, ask server to perform authentication and return a valid token.
     */
    private Credentials credentials;

    /**
     * Main topic, manage services.
     */
    private Iterable<Service> services;

    /**
     * List dependencies between services (by service name).
     */
    private Iterable<Dependency> dependencies;

    /**
     * Metrics query and result.
     */
    private MetricStatView metricStats;

    /**
     * Error message from the server to the client.
     */
    private String error;

    /**
     * Indicates broadcast queue overflow.
     */
    private Boolean overflow;
}
