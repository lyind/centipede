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

package net.talpidae.centipede.resource;

import com.google.common.eventbus.EventBus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import net.talpidae.base.util.auth.AuthRequired;
import net.talpidae.base.util.auth.AuthenticationSecurityContext;
import net.talpidae.base.util.auth.Authenticator;
import net.talpidae.base.util.auth.Credentials;
import net.talpidae.base.util.session.Session;
import net.talpidae.base.util.session.SessionService;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;


@Singleton
@Resource
@Path("/auth")
public class Authentication
{
    private final EventBus eventBus;

    private final Authenticator authenticator;

    private final SessionService sessionService;


    @Inject
    public Authentication(EventBus eventBus, Authenticator authenticator, SessionService sessionService)
    {
        this.eventBus = eventBus;
        this.authenticator = authenticator;
        this.sessionService = sessionService;
    }

    @POST
    @Path("/signin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response signin(@Context SecurityContext securityContext, Credentials credentials)
    {
        // still logged in with active session?
        if (securityContext instanceof AuthenticationSecurityContext)
        {
            // just return a fresh token (expiry postponed) for the active session
            return Response.accepted(authenticator.createToken(((AuthenticationSecurityContext) securityContext).getSessionId())).build();
        }

        // TODO: validate against database
        if (credentials != null && "ADMIN".equals(credentials.getName()) && "ADMIN".equals(credentials.getPassword()))
        {
            credentials.clear(); // erase PW

            val session = sessionService.get(null);
            val attributes = session.getAttributes();
            attributes.put(Session.ATTRIBUTE_PRINCIPAL, credentials.getName());
            attributes.put(Session.ATTRIBUTE_ACCOUNT, "");
            attributes.put(Session.ATTRIBUTE_ROLES, "admin");

            sessionService.save(session);

            eventBus.post(new SignInEvent(credentials.getName()));

            // return a signed token, which used by AuthenticationRequestFilter to authenticate users
            return Response.accepted(authenticator.createToken(session.getId())).build();
        }

        eventBus.post(new SignInFailedEvent((credentials != null) ? credentials.getName() : "null"));

        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    @AuthRequired
    @POST
    @Path("/signout")
    public Response signout(@Context SecurityContext securityContext)
    {
        if (securityContext instanceof AuthenticationSecurityContext)
        {
            sessionService.remove(((AuthenticationSecurityContext) securityContext).getSession().getId());
        }

        eventBus.post(new SignOutEvent(securityContext.getUserPrincipal().getName()));

        return Response.status(Response.Status.UNAUTHORIZED).build();
    }


    @AllArgsConstructor
    public class SignInFailedEvent
    {
        @Getter
        private final String name;
    }


    @AllArgsConstructor
    public class SignInEvent
    {
        @Getter
        private final String name;
    }


    @AllArgsConstructor
    public class SignOutEvent
    {
        @Getter
        private final String name;
    }
}
