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
import net.talpidae.centipede.bean.service.Kind;
import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.bean.service.State;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.ServicesModified;
import net.talpidae.centipede.util.cli.CommandLine;
import net.talpidae.centipede.util.process.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import lombok.val;


@Slf4j
@Singleton
public class TransitionUp implements Transition
{
    private final Queen queen;

    private final EventBus eventBus;

    private final CentipedeRepository centipedeRepository;

    @Inject
    public TransitionUp(Queen queen, EventBus eventBus, CentipedeRepository centipedeRepository)
    {
        this.queen = queen;
        this.eventBus = eventBus;
        this.centipedeRepository = centipedeRepository;
    }


    @Override
    public void apply(Service service, int transitionCount)
    {
        if (transitionCount == 0)
        {
            val arguments = new ArrayList<String>();
            if (service.getKind() == Kind.JAVA)
            {
                arguments.add("java");
                arguments.addAll(CommandLine.split(service.getVmArguments()));
                arguments.add("-jar");
                arguments.add(service.getImage());
                arguments.addAll(CommandLine.split(service.getArguments()));
            }
            else
            {
                arguments.add(service.getImage());
                arguments.addAll(CommandLine.split(service.getArguments()));
            }

            try
            {
                val process = new ProcessBuilder(arguments)
                        .inheritIO()
                        .redirectInput(ProcessBuilder.Redirect.PIPE)
                        .directory(new File(service.getName()))
                        .start();

                val pid = ProcessUtil.getProcessID(process);

                setServiceStateChanging(service, pid);
            }
            catch (IOException e)
            {
                log.error("failed to launch service {}: {}", e.getMessage(), e);
            }
        }
        else
        {
            // disable out-of-service state, repeat until the service is started to ensure proper operation
            queen.setIsOutOfService(service.getRoute(), InetSocketAddress.createUnresolved(service.getHost(), service.getPort()), false);
        }
    }


    private void setServiceStateChanging(Service service, long pid)
    {
        val updatedService = Service.builder()
                .name(service.getName())
                .state(State.CHANGING)
                .pid(pid)
                .transition(0)
                .build();

        centipedeRepository.insertServiceState(updatedService);

        eventBus.post(new ServicesModified(Collections.singletonList(service.getName())));
    }
}
