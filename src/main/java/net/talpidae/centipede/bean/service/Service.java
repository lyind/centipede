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

package net.talpidae.centipede.bean.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;


/**
 * Describes a service under Centipede's management. Deliberately kept simple.
 */
@Getter
@EqualsAndHashCode
@ToString
@Builder
public class Service
{
    /**
     * This service instances generation (increased whenever some property changes).
     */
    private final int generation;

    /**
     * Is this service instance deleted (ie. set end-of-life)?
     */
    private final boolean retired;

    /**
     * Creation timestamp of this record (ie. last modified timestamp).
     */
    private final OffsetDateTime ts;

    /**
     * This service instances, unique name. Choose whatever makes sense.
     */
    @NonNull
    private final String name;

    @NonNull
    private final State state;

    @NonNull
    private final State targetState;

    /**
     * Service implementation type.
     */
    private final Kind kind;

    /**
     * Service VM arguments.
     */
    private final String vmArguments;

    /**
     * Service program image file name (including extension, ie. ".jar").
     */
    private final String image;

    /**
     * Service arguments.
     */
    private final String arguments;

    /**
     * The route that other services may use to find this service.
     * <p>
     * Service interfaces (ie. routes) that differ in functionality (are not compatible) should be named accordingly.
     * <p>
     * Example: net.talpidae.authentication.Authentication vs net.talpidae.authentication.Authentication2
     */
    private final String route;

    /**
     * Optional path prefix for the reverse proxy mapping.
     */
    private final String proxyPathPrefix;

    private final Long pid;

    private final String host;

    private final Integer port;

    /**
     * The apply count (how many times centipede tried to reach targetState).
     */
    private final Integer transition;


    @JsonCreator
    public Service(@JsonProperty("generation") @ColumnName("generation") int generation,
                   @JsonProperty("retired") @ColumnName("retired") boolean retired,
                   @JsonProperty("ts") @ColumnName("ts") OffsetDateTime ts,
                   @JsonProperty("name") @ColumnName("name") String name,
                   @JsonProperty("state") @ColumnName("state") State state,
                   @JsonProperty("targetState") @ColumnName("targetState") State targetState,
                   @JsonProperty("kind") @ColumnName("kind") Kind kind,
                   @JsonProperty("vmArguments") @ColumnName("vmArguments") String vmArguments,
                   @JsonProperty("image") @ColumnName("image") String image,
                   @JsonProperty("arguments") @ColumnName("arguments") String arguments,
                   @JsonProperty("route") @ColumnName("route") String route,
                   @JsonProperty("proxyPathPrefix") @ColumnName("proxyPathPrefix") String proxyPathPrefix,
                   @JsonProperty("pid") @ColumnName("pid") Long pid,
                   @JsonProperty("host") @ColumnName("host") String host,
                   @JsonProperty("port") @ColumnName("port") Integer port,
                   @JsonProperty("transition") @ColumnName("transition") Integer transition)
    {
        this.generation = generation;
        this.retired = retired;
        this.ts = ts;
        this.name = name;
        this.state = state;
        this.targetState = targetState;
        this.kind = kind;
        this.vmArguments = vmArguments;
        this.image = image;
        this.arguments = arguments;
        this.route = route;
        this.proxyPathPrefix = proxyPathPrefix;
        this.pid = pid;
        this.host = host;
        this.port = port;
        this.transition = transition;
    }
}
