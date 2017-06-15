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

package net.talpidae.centipede.service.calls;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import net.talpidae.base.resource.AuthenticationRequestFilter;
import net.talpidae.base.util.auth.Authenticator;
import net.talpidae.base.util.auth.Credentials;
import net.talpidae.base.util.session.Session;
import net.talpidae.base.util.session.SessionService;
import net.talpidae.centipede.bean.service.Api;

import javax.inject.Inject;
import java.util.Optional;


@Getter
public class Security implements CallHandler
{
    private final Phase phase = Phase.PRE_HANDLE;

    private final EventBus eventBus;

    private final Authenticator authenticator;

    private final SessionService sessionService;

    private final AuthenticationRequestFilter authenticationRequestFilter;

    @Inject
    public Security(EventBus eventBus, Authenticator authenticator, SessionService sessionService, AuthenticationRequestFilter authenticationRequestFilter)
    {
        this.eventBus = eventBus;
        this.authenticator = authenticator;
        this.sessionService = sessionService;
        this.authenticationRequestFilter = authenticationRequestFilter;
    }


    private static Api assertAuthenticated(Api request)
    {
        val securityContext = request.getContext().getSecurityContext();
        if (securityContext != null && securityContext.getSession() != null)
        {
            return request;
        }

        throw new AuthenticationException("UNAUTHORIZED");
    }


    private Api validateToken(Api request)
    {
        val token = request.getToken();
        if (!Strings.isNullOrEmpty(token))
        {
            val securityContext = authenticationRequestFilter.createSecurityContext(token);
            if (securityContext != null)
            {
                request.getContext().setSecurityContext(securityContext);
            }
        }

        return request;
    }


    private Api signin(Api request)
    {
        if (request.getToken() == null)
        {
            return request;
        }

        val context = request.getContext();
        if (context.getSecurityContext() != null)
        {
            val session = context.getSecurityContext().getSession();
            if (session != null)
            {
                // just return a fresh token (expiry postponed) for the active session
                request.setToken(authenticator.createToken(session.getId()));
                request.setCredentials(Credentials.builder().name("ADMIN").build());
                return request;
            }
        }

        // TODO: validate against database
        val credentials = request.getCredentials();
        if (credentials != null && "ADMIN".equals(credentials.getName()) && "ADMIN".equals(credentials.getPassword()))
        {
            credentials.clear(); // erase PW

            val name = credentials.getName();
            val session = sessionService.get(null);
            val attributes = session.getAttributes();
            attributes.put(Session.ATTRIBUTE_PRINCIPAL, name);
            attributes.put(Session.ATTRIBUTE_ACCOUNT, "");
            attributes.put(Session.ATTRIBUTE_ROLES, "admin");

            sessionService.save(session);

            eventBus.post(new SignInEvent(name));

            // return a signed token, which used by AuthenticationRequestFilter to authenticate users
            request.setToken(authenticator.createToken(session.getId()));

            return validateToken(request);
        }

        eventBus.post(new SignInFailedEvent((credentials != null) ? credentials.getName() : "null"));

        throw new AuthenticationException("UNAUTHORIZED");
    }


    private Api signout(Api request)
    {
        // authorized but credentials without name -> signout
        val securityContext = request.getContext().getSecurityContext();
        if (securityContext != null)
        {
            val credentials = request.getCredentials();
            if (credentials != null && Strings.isNullOrEmpty(credentials.getName()))
            {
                sessionService.remove(securityContext.getSessionId());
                request.getContext().setSecurityContext(null);

                eventBus.post(new SignOutEvent(securityContext.getUserPrincipal().getName()));

                return null;
            }
        }

        return request;
    }


    @Override
    public Api apply(Api request)
    {
        return Optional.ofNullable(request)
                .map(this::validateToken)
                .map(this::signin)
                .map(Security::assertAuthenticated) // everything after parsing the token/signin requires auth
                .map(this::signout)
                .orElse(null);
    }


    public static class AuthenticationException extends CallException
    {
        public AuthenticationException(String message)
        {
            super(message);
        }
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
