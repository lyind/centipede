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

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.talpidae.base.util.auth.Credentials;
import net.talpidae.centipede.service.wrapper.RequestContext;

import java.util.List;


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
    private final transient RequestContext context = new RequestContext();

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
    private List<Service> services;

    /**
     * Error message from the server to the client.
     */
    private String error;

    /**
     * Indicates broadcast queue overflow.
     */
    private Boolean overflow;
}
