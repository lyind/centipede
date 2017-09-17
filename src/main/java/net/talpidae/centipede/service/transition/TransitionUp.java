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

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;

import net.talpidae.base.insect.Queen;
import net.talpidae.centipede.bean.service.Kind;
import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.bean.service.State;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.ServicesModified;
import net.talpidae.centipede.task.state.StateMachine;
import net.talpidae.centipede.util.cli.CommandLine;
import net.talpidae.centipede.util.environment.MergedEnvironment;
import net.talpidae.centipede.util.interpolation.CommandLineInterpolator;
import net.talpidae.centipede.util.process.ProcessUtil;
import net.talpidae.centipede.util.service.ServiceUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static com.google.common.base.Strings.isNullOrEmpty;
import static net.talpidae.centipede.util.service.ServiceUtil.setOutOfService;


@Slf4j
@Singleton
public class TransitionUp implements Transition
{
    private final static String INSECT_NAME_KEY = "INSECT_NAME";

    private final Queen queen;

    private final EventBus eventBus;

    private final CentipedeRepository centipedeRepository;

    private final CommandLineInterpolator commandLineInterpolator;

    private final MergedEnvironment mergedEnvironment;


    @Inject
    public TransitionUp(Queen queen,
                        EventBus eventBus,
                        CentipedeRepository centipedeRepository,
                        CommandLineInterpolator commandLineInterpolator,
                        MergedEnvironment mergedEnvironment)
    {
        this.queen = queen;
        this.eventBus = eventBus;
        this.centipedeRepository = centipedeRepository;
        this.commandLineInterpolator = commandLineInterpolator;
        this.mergedEnvironment = mergedEnvironment;
    }


    @Override
    public void apply(Service service, int transitionCount)
    {
        if (transitionCount == 0)
        {
            try
            {
                val serviceEnvironment = getServiceEnvironment(service);
                val image = commandLineInterpolator.interpolate(service.getImage(), serviceEnvironment);
                if (isNullOrEmpty(image))
                    throw new IllegalArgumentException("image path not specified");

                val imagePath = Paths.get(image);
                val canonicalImagePath = imagePath.toAbsolutePath().normalize();
                val arguments = new ArrayList<String>();
                if (service.getKind() == Kind.JAVA)
                {
                    if (!Files.isReadable(imagePath) || !Files.isRegularFile(imagePath))
                        throw new IllegalArgumentException("invalid path or unreadable JAR file: " + image);

                    arguments.add("java");
                    arguments.addAll(interpolateAndSplit(service.getVmArguments(), serviceEnvironment));
                    arguments.add("-jar");
                    arguments.add(canonicalImagePath.toString());
                    arguments.addAll(interpolateAndSplit(service.getArguments(), serviceEnvironment));

                    // TODO Use fork library on linux
                }
                else
                {
                    if (!Files.isExecutable(imagePath) || !Files.isRegularFile(imagePath))
                        throw new IllegalArgumentException("invalid path or not an executable file: " + image);

                    arguments.add(canonicalImagePath.toString());
                    arguments.addAll(interpolateAndSplit(service.getArguments(), serviceEnvironment));
                }

                val workingDir = Files.createDirectories(Paths.get(service.getName()));

                log.debug("launching in directory {}: {} {}", workingDir, service.getName(), arguments);
                val process = new ProcessBuilder(arguments)
                        .inheritIO()
                        .redirectInput(ProcessBuilder.Redirect.PIPE)
                        .directory(workingDir.toFile())
                        .start();

                final Long pid = ProcessUtil.getProcessID(process);
                if (ServiceUtil.isValidPid(pid))
                {
                    log.info("successfully launched {}, pid: {}", service.getName(), pid);
                }
                else
                {
                    log.error("failed to retrieve pid for {}", service.getName());
                }

                setServicePid(service, pid);
            }
            catch (IOException e)
            {
                log.error("failed to launch service {}: {}", service.getName(), e.getMessage());
            }
            catch (IllegalArgumentException e)
            {
                log.error("invalid configuration for service {}: {}", service.getName(), e.getMessage());

                // permanent error, don't know how to reach state "UP"
                setServiceTargetStateUnknown(service);
            }
        }
        else
        {
            // set out-of-service state, repeat until the service is started to ensure proper operation
            setOutOfService(queen, service, State.OUT_OF_SERVICE == service.getTargetState());
        }
    }


    private Map<String, String> getServiceEnvironment(Service service)
    {
        return mergedEnvironment.getEnvironment(ImmutableMap.of(
                INSECT_NAME_KEY, service.getName()
        ));
    }


    private List<String> interpolateAndSplit(String commandLine, Map<String, String> environment)
    {
        return CommandLine.split(commandLineInterpolator.interpolate(commandLine, environment));
    }


    // set target state to UNKNOWN in case the user needs to fix the service configuration
    private void setServiceTargetStateUnknown(Service service)
    {
        val updatedService = Service.builder()
                .name(service.getName())
                .targetState(State.UNKNOWN)
                .transition(StateMachine.TIMEOUT_TRANSITIONS)
                .build();

        centipedeRepository.insertServiceConfiguration(updatedService);
        centipedeRepository.insertServiceState(updatedService);

        eventBus.post(new ServicesModified(Collections.singletonList(service.getName())));
    }


    private void setServicePid(Service service, Long pid)
    {
        val updatedService = Service.builder()
                .name(service.getName())
                .pid(pid)
                .build();

        centipedeRepository.insertServiceState(updatedService);

        eventBus.post(new ServicesModified(Collections.singletonList(service.getName())));
    }
}
