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

package net.talpidae.centipede;


import com.google.inject.AbstractModule;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.talpidae.base.Base;
import net.talpidae.base.server.Server;
import net.talpidae.base.server.ServerConfig;
import net.talpidae.base.util.auth.Authenticator;
import net.talpidae.base.util.session.SessionService;
import net.talpidae.centipede.util.auth.LocalAuthenticator;
import net.talpidae.centipede.util.session.LocalSessionService;

import javax.servlet.ServletException;

import static java.lang.System.exit;

@Slf4j
public class CentipedeApplication extends AbstractModule
{
    private static final String[] resourcePackages = new String[] { net.talpidae.centipede.resource.Hello.class.getPackage().getName() };

    public static void main(String[] args)
    {
        val injector = Base.initializeApp(new CentipedeApplication());

        val serverConfig = injector.getInstance(ServerConfig.class);
        serverConfig.setJerseyResourcePackages(resourcePackages);

        val server = injector.getInstance(Server.class);
        try
        {
            server.start();
            log.info("server started on {}:{}", serverConfig.getHost(), serverConfig.getPort());

            log.info("POST /shutdown to stop server");
            server.waitForStop();
        }
        catch (ServletException e)
        {
            log.error("failed to start server: {}", e.getMessage());
            exit(1);
        }
    }


    @Override
    protected void configure()
    {
        // add additional Guice bindings here

        bind(Authenticator.class).to(LocalAuthenticator.class);
        bind(SessionService.class).to(LocalSessionService.class);
    }
}
