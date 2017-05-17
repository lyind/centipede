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

package net.talpidae.centipede.service;

import com.google.inject.AbstractModule;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import net.talpidae.centipede.service.calls.CallHandler;
import net.talpidae.centipede.service.calls.Security;
import net.talpidae.centipede.service.calls.Services;


public class ServiceModule extends AbstractModule
{
    private Multibinder<CallHandler> callBinder;

    @Override
    protected void configure()
    {
        callBinder = Multibinder.newSetBinder(binder(), CallHandler.class);

        bindCall().to(Security.class);
        bindCall().to(Services.class);

        bind(ApiRunnableFactory.class);
    }


    /**
     * Override this method to call {@link #bindCall}.
     */
    protected void configureActions()
    {

    }


    protected final LinkedBindingBuilder<CallHandler> bindCall()
    {
        return callBinder.addBinding();
    }
}
