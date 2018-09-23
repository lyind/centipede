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

package net.talpidae.centipede.util.session;

import net.talpidae.base.util.session.Session;
import net.talpidae.base.util.session.SessionService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;


public class LocalSessionService implements SessionService
{
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public Session get(String id)
    {
        Session session = (id != null) ? sessions.get(id) : null;
        if (session == null)
        {
            session = new LocalSession();
            sessions.put(session.getId(), session);
        }

        return session;
    }

    @Override
    public void save(Session session)
    {
        sessions.put(session.getId(), session);
    }

    @Override
    public void remove(String id)
    {
        sessions.remove(id);
    }


    private static class LocalSession implements Session
    {
        @Getter
        private final String id;

        @Getter
        private final Map<String, String> attributes = new HashMap<>(3);


        LocalSession()
        {
            id = UUID.randomUUID().toString();
        }
    }
}
