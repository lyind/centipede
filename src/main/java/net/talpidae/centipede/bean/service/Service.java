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

import lombok.*;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.OffsetDateTime;


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
     * This service instances generation (increased whenever something changed).
     */
    @Builder.Default
    private final int generation;

    /**
     * Is this service instance deleted (ie. set end-of-life)?
     */
    @Builder.Default
    private final boolean retired;

    /**
     * Creation timestamp of this record (ie. last modified timestamp).
     */
    @Builder.Default
    private final OffsetDateTime ts;

    /**
     * This service instances, unique name. Choose whatever makes sense.
     */
    @NonNull
    private final String name;

    @Builder.Default
    @NonNull
    private final State state;

    @Builder.Default
    @NonNull
    private final State targetState;

    /**
     * Service implementation type.
     */
    @Builder.Default
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


    public Service(@ColumnName("generation") int generation,
                   @ColumnName("retired") boolean retired,
                   @ColumnName("ts") OffsetDateTime ts,
                   @ColumnName("name") String name,
                   @ColumnName("state") State state,
                   @ColumnName("targetState") State targetState,
                   @ColumnName("kind") Kind kind,
                   @ColumnName("vmArguments") String vmArguments,
                   @ColumnName("image") String image,
                   @ColumnName("arguments") String arguments,
                   @ColumnName("route") String route,
                   @ColumnName("proxyPathPrefix") String proxyPathPrefix,
                   @ColumnName("pid") Long pid,
                   @ColumnName("host") String host,
                   @ColumnName("port") Integer port)
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
    }
}
