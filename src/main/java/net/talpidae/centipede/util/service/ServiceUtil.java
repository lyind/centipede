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

package net.talpidae.centipede.util.service;

import net.talpidae.base.insect.Queen;
import net.talpidae.centipede.bean.service.Service;

import java.net.InetSocketAddress;

import lombok.val;

import static com.google.common.base.Strings.isNullOrEmpty;


public class ServiceUtil
{
    private ServiceUtil()
    {

    }


    public static InetSocketAddress fromService(Service service)
    {
        val host = service.getHost();
        val port = service.getPort();
        return (!isNullOrEmpty(host) && port != null && port > 0 && port < 65536)
                ? new InetSocketAddress(host, port)
                : null;
    }


    public static InetSocketAddress fromServiceUnresolved(Service service)
    {
        val host = service.getHost();
        val port = service.getPort();
        return (!isNullOrEmpty(host) && port != null && port > 0 && port < 65536)
                ? InetSocketAddress.createUnresolved(host, port)
                : null;
    }


    public static boolean setOutOfService(Queen queen, Service service, boolean isOutOfService)
    {
        val socketAddress = fromServiceUnresolved(service);
        val route = service.getRoute();
        if (socketAddress != null && route != null)
        {
            queen.setIsOutOfService(service.getRoute(), socketAddress, isOutOfService);

            return true;
        }

        return false;
    }


    public static boolean hasValidPid(Service service)
    {
        return service.getPid() != null && service.getPid() >= 0;
    }
}
