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

import com.google.inject.Singleton;

import net.talpidae.base.server.Server;
import net.talpidae.base.server.ServerConfig;
import net.talpidae.base.util.Application;
import net.talpidae.centipede.util.server.CentipedeRootHandlerWrapper;

import java.net.InetSocketAddress;

import javax.inject.Inject;
import javax.servlet.ServletException;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static java.lang.System.exit;


@Singleton
@Slf4j
public class CentipedeApplication implements Application
{
    private final ServerConfig serverConfig;

    private final Server server;

    private final CentipedeRootHandlerWrapper rootHandlerWrapper;


    @Inject
    public CentipedeApplication(ServerConfig serverConfig,
                                Server server,
                                CentipedeRootHandlerWrapper rootHandlerWrapper)
    {
        this.serverConfig = serverConfig;
        this.server = server;
        this.rootHandlerWrapper = rootHandlerWrapper;
    }


    @Override
    public void run()
    {
        // add http handler that serves static files and falls back to serving "/" on unknown objects
        // (enables browser history support)
        serverConfig.setRootHandlerWrapper(rootHandlerWrapper);

        try
        {
            server.start();
            val bindAddress = new InetSocketAddress(serverConfig.getHost(), serverConfig.getPort());
            log.info("server started on {}", bindAddress.toString());

            server.waitForShutdown();
        }
        catch (ServletException e)
        {
            log.error("failed to start server: {}", e.getMessage());
            exit(1);
        }
    }
}
