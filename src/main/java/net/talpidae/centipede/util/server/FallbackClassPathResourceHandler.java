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

package net.talpidae.centipede.util.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ResourceHandler;


@Singleton
public class FallbackClassPathResourceHandler extends ResourceHandler implements HttpHandler
{
    @Inject
    public FallbackClassPathResourceHandler(CentipedeClassPathResourceManager classPathResourceManager)
    {
        super(classPathResourceManager);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception
    {
        // rewrite to serve root/index.html instead of the (non-existing) relative path specified by the client
        exchange.setRelativePath("");
        exchange.setRequestPath("/");

        super.handleRequest(exchange);
    }
}
