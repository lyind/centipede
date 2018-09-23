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
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.OptionalBinder;

import net.talpidae.base.Base;
import net.talpidae.base.database.DefaultDataBaseConfig;
import net.talpidae.base.insect.AsyncQueen;
import net.talpidae.base.insect.Queen;
import net.talpidae.base.insect.SyncQueen;
import net.talpidae.base.server.WebSocketEndpoint;
import net.talpidae.base.util.Application;
import net.talpidae.base.util.auth.Authenticator;
import net.talpidae.base.util.lifecycle.ShutdownHook;
import net.talpidae.base.util.session.SessionHolder;
import net.talpidae.base.util.session.SessionService;
import net.talpidae.centipede.bean.configuration.Configuration;
import net.talpidae.centipede.database.CentipedeDefaultDataBaseConfig;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.resource.CentipedeWebSocketEndPoint;
import net.talpidae.centipede.service.EventForwarder;
import net.talpidae.centipede.service.ServiceModule;
import net.talpidae.centipede.util.auth.LocalAuthenticator;
import net.talpidae.centipede.util.configuration.ConfigurationLoader;
import net.talpidae.centipede.util.lifecycle.CentipedeShutdownHook;
import net.talpidae.centipede.util.session.LocalSessionService;
import net.talpidae.centipede.util.session.WebSocketSessionHolder;

import org.jdbi.v3.core.Jdbi;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class CentipedeApplicationModule extends AbstractModule
{
    public static void main(String[] args)
    {
        Base.initializeApp(args, new CentipedeApplicationModule()).run();
    }


    @Override
    protected void configure()
    {
        bind(SyncQueen.class).to(CentipedeSyncQueen.class).asEagerSingleton();
        OptionalBinder.newOptionalBinder(binder(), Queen.class).setBinding().to(AsyncQueen.class).asEagerSingleton();

        bind(Application.class).to(CentipedeApplication.class);
        bind(DefaultDataBaseConfig.class).to(CentipedeDefaultDataBaseConfig.class);

        bind(CentipedeWebSocketEndPoint.class);

        bind(Authenticator.class).to(LocalAuthenticator.class);
        bind(SessionService.class).to(LocalSessionService.class);

        bind(SessionHolder.class).to(WebSocketSessionHolder.class);

        bind(CentipedeLogic.class);

        OptionalBinder.newOptionalBinder(binder(), new TypeLiteral<Class<? extends WebSocketEndpoint>>() {}).setBinding().toInstance(CentipedeWebSocketEndPoint.class);

        install(new ServiceModule());

        OptionalBinder.newOptionalBinder(binder(), ShutdownHook.class).setBinding().to(CentipedeShutdownHook.class);

        bind(EventForwarder.class).asEagerSingleton();
        bind(CentipedeLogic.class).asEagerSingleton();
    }


    @Provides
    @Singleton
    public CentipedeRepository centipedeRepositoryProvider(Jdbi jdbi)
    {
        return jdbi.onDemand(CentipedeRepository.class);
    }


    /**
     * Loads the applications optional configuration file or terminates the application.
     */
    @Provides
    @Singleton
    public Configuration configurationProvider(ConfigurationLoader loader)
    {
        return loader.load();
    }
}
