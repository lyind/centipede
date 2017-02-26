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

package net.talpidae.centipede.util.auth;

import net.talpidae.base.util.auth.AbstractAuthenticator;

import java.time.Duration;
import java.time.temporal.TemporalAmount;


public class LocalAuthenticator extends AbstractAuthenticator
{
    private static final String[] keys = new String[]{"qFz4NO7sw0DCJDUu0BqmI7nEn4x6/IfwG95taoyTqT0="};

    private static final TemporalAmount TOKEN_VALID_DURATION = Duration.ofHours(25);


    public LocalAuthenticator()
    {
        super(keys);
    }

    @Override
    protected TemporalAmount getExpirationTime()
    {
        return TOKEN_VALID_DURATION;
    }
}
