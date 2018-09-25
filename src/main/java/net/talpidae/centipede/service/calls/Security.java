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

import net.talpidae.base.resource.AuthenticationRequestFilter;
import net.talpidae.base.util.auth.AuthenticationSecurityContext;
import net.talpidae.base.util.auth.Authenticator;
import net.talpidae.base.util.auth.Credentials;
import net.talpidae.base.util.session.Session;
import net.talpidae.base.util.session.SessionService;
import net.talpidae.centipede.service.wrapper.Call;

import javax.inject.Inject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;


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


    private static void assertAuthenticated(Call call)
    {
        val securityContext = call.getSecurityContext();
        if (securityContext == null || securityContext.getUserPrincipal() == null)
        {
            throw new AuthenticationException("UNAUTHORIZED");
        }
    }


    private void validateToken(Call call)
    {
        val token = call.getRequest().getToken();
        if (!Strings.isNullOrEmpty(token))
        {
            call.setSecurityContext(createSecurityContext(token));
        }
    }


    private AuthenticationSecurityContext createSecurityContext(String token)
    {
        return authenticationRequestFilter.createSecurityContext(token);
    }


    private void signin(Call call)
    {
        if (call.getRequest().getToken() != null)
        {
            if (call.getSecurityContext() != null && call.getSecurityContext().getUserPrincipal() != null)
            {
                val session = call.getSecurityContext().getUserPrincipal().getSession();
                if (session != null)
                {
                    // just return a fresh token (expiry postponed) for the active session
                    call.getResponse().token(authenticator.createToken(session.getId()));
                    call.getResponse().credentials(Credentials.builder().name("ADMIN").build());
                    return;
                }
            }

            // TODO: validate against database
            val credentials = call.getRequest().getCredentials();
            if (credentials != null && "ADMIN".equals(credentials.getName()) && "ADMIN".contentEquals(credentials.getPassword()))
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
                val token = authenticator.createToken(session.getId());
                call.setSecurityContext(createSecurityContext(token));
                call.getResponse().token(token);
                return;
            }

            eventBus.post(new SignInFailedEvent((credentials != null) ? credentials.getName() : "null"));

            throw new AuthenticationException("UNAUTHORIZED");
        }
    }


    private void signout(Call call)
    {
        // authorized but credentials without name -> signout
        val securityContext = call.getSecurityContext();
        if (securityContext != null)
        {
            val credentials = call.getRequest().getCredentials();
            if (credentials != null && Strings.isNullOrEmpty(credentials.getName()))
            {
                val principal = securityContext.getUserPrincipal();
                if (principal != null)
                {
                    sessionService.remove(principal.getSessionId().toString());

                    eventBus.post(new SignOutEvent(principal.getName()));
                }

                call.setSecurityContext(null);

                throw new AuthenticationException("UNAUTHORIZED");
            }
        }
    }


    @Override
    public void accept(Call call)
    {
        validateToken(call);
        signin(call);
        assertAuthenticated(call); // everything after parsing the token/signin requires auth
        signout(call);
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
