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

package net.talpidae.centipede.service.transition;

import com.google.common.eventbus.EventBus;

import net.talpidae.base.insect.Queen;
import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.bean.service.State;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.ServicesModified;
import net.talpidae.centipede.util.process.ProcessUtil;

import java.net.InetSocketAddress;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static com.google.common.base.Strings.isNullOrEmpty;


@Slf4j
@Singleton
public class TransitionDown implements Transition
{
    private final EventBus eventBus;

    private final CentipedeRepository centipedeRepository;

    private final Queen queen;


    @Inject
    public TransitionDown(EventBus eventBus, CentipedeRepository centipedeRepository, Queen queen)
    {
        this.eventBus = eventBus;
        this.centipedeRepository = centipedeRepository;
        this.queen = queen;
    }

    public void apply(Service service, int transitionCount)
    {
        val pid = service.getPid();
        boolean havePid = service.getPid() != null && service.getPid() >= 0;

        if (transitionCount == 0)
        {
            // set insect out-of-service (to stop clients from connecting)
            queen.setIsOutOfService(service.getRoute(), InetSocketAddress.createUnresolved(service.getHost(), service.getPort()), true);

            setServiceStateChanging(service);
        }
        else if (transitionCount == 1)
        {
            // set out-of-service again, in case it was overwritten by a mapping
            queen.setIsOutOfService(service.getRoute(), InetSocketAddress.createUnresolved(service.getHost(), service.getPort()), true);
        }
        else if (transitionCount == 2)
        {
            // first try to shutdown process via insect message
            val host = service.getHost();
            val port = service.getPort();
            if (!isNullOrEmpty(host) && port != null && port > 0 && port < 65536)
            {
                log.debug("sending shutdown request to {}", service.getName());
                queen.sendShutdown(new InetSocketAddress(host, port));
            }
            else
            {
                // skip sending shutdown request if no port is known
                log.debug("can't send shutdown request to {}: host={}, port={}", service.getName(), host, port);
            }
        }
        else if (transitionCount == 6)
        {
            if (havePid)
            {
                // second try to shutdown gracefully
                log.debug("signal process about imminent shutdown {} ({})", service.getName(), pid);
                ProcessUtil.terminateProcess(pid, false);
            }
        }
        else if (transitionCount >= 9)
        {
            if (havePid)
            {
                // third, try to kill process
                log.debug("terminating process {} ({})", service.getName(), pid);
                ProcessUtil.terminateProcess(pid, true);
            }

            // we can't do anything else, we just assume the process is dead
            setServiceStateDown(service);
        }
    }


    private void setServiceStateChanging(Service service)
    {
        val updatedService = Service.builder()
                .name(service.getName())
                .state(State.CHANGING)
                .build();

        centipedeRepository.insertServiceState(updatedService);

        eventBus.post(new ServicesModified(Collections.singletonList(service.getName())));
    }


    private void setServiceStateDown(Service service)
    {
        val updatedService = Service.builder()
                .name(service.getName())
                .state(State.DOWN)
                .pid(-1L)
                .port(-1)
                .transition(0)
                .build();

        centipedeRepository.insertServiceState(updatedService);

        eventBus.post(new ServicesModified(Collections.singletonList(service.getName())));
    }
}