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
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.ServicesModified;
import net.talpidae.centipede.util.process.ProcessUtil;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static net.talpidae.centipede.util.service.ServiceUtil.fromService;
import static net.talpidae.centipede.util.service.ServiceUtil.isValidPid;
import static net.talpidae.centipede.util.service.ServiceUtil.setOutOfService;


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
        val name = service.getName();
        val pid = service.getPid();

        if (transitionCount < 10)
        {
            // force insect out-of-service (to stop clients from connecting)
            setOutOfService(queen, service, true);

            if (transitionCount == 3)
            {
                // first try to shutdown process via insect message
                val socketAddress = fromService(service);
                if (socketAddress != null)
                {
                    log.debug("sending shutdown request to {}", name);
                    queen.sendShutdown(socketAddress);
                }
                else
                {
                    // skip sending shutdown request if no port is known
                    log.debug("can't send shutdown request to {}: host={}, port={}", name, service.getHost(), service.getPort());
                }
            }
            else if (transitionCount == 7)
            {
                if (isValidPid(pid))
                {
                    // second try to shutdown gracefully
                    log.debug("signal process about imminent shutdown {} ({})", name, pid);
                    ProcessUtil.terminateProcess(pid, false);
                }
            }
        }
        else
        {
            if (isValidPid(pid))
            {
                // third, try to kill process
                log.debug("terminating process {} ({})", name, pid);
                ProcessUtil.terminateProcess(pid, true);
            }

            // we can't do anything else, we just assume the process is dead
            setServiceStateDown(service);
        }
    }


    private void setServiceStateDown(Service service)
    {
        val updatedService = Service.builder()
                .name(service.getName())
                .pid(-1L)
                .port(-1)
                .build();

        centipedeRepository.insertServiceState(updatedService);

        eventBus.post(new ServicesModified(Collections.singletonList(service.getName())));
    }
}
