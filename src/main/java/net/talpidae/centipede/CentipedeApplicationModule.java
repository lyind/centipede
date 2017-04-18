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
import lombok.extern.slf4j.Slf4j;
import net.talpidae.base.Base;
import net.talpidae.base.database.DefaultDataBaseConfig;
import net.talpidae.base.insect.SyncQueen;
import net.talpidae.base.util.Application;
import net.talpidae.base.util.auth.Authenticator;
import net.talpidae.base.util.session.SessionService;
import net.talpidae.centipede.database.CentipedeDefaultDataBaseConfig;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.util.auth.LocalAuthenticator;
import net.talpidae.centipede.util.session.LocalSessionService;
import org.jdbi.v3.core.Jdbi;


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
        bind(DefaultDataBaseConfig.class).to(CentipedeDefaultDataBaseConfig.class);
        bind(Application.class).to(CentipedeApplication.class);
        bind(SyncQueen.class).to(CentipedeSyncQueen.class);

        bind(Authenticator.class).to(LocalAuthenticator.class);
        bind(SessionService.class).to(LocalSessionService.class);

        bind(CentipedeLogic.class);
    }


    @Provides
    @Singleton
    public CentipedeRepository centipedeRepositoryProvider(Jdbi jdbi)
    {
        return jdbi.onDemand(CentipedeRepository.class);
    }
}
