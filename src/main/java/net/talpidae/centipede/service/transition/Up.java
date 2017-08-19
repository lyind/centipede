package net.talpidae.centipede.service.transition;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.talpidae.centipede.bean.service.Kind;
import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.bean.service.State;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.ServicesModified;
import net.talpidae.centipede.util.cli.CommandLine;
import net.talpidae.centipede.util.process.ProcessUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;


@Slf4j
@Singleton
public class Up implements Transition
{
    private final EventBus eventBus;

    private final CentipedeRepository centipedeRepository;

    @Inject
    public Up(EventBus eventBus, CentipedeRepository centipedeRepository)
    {
        this.eventBus = eventBus;
        this.centipedeRepository = centipedeRepository;
    }


    @Override
    public boolean transition(Service service)
    {
        centipedeRepository.insertNextServiceTransition(service);

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

            return true;
        }
        catch (IOException e)
        {
            log.error("failed to launch service {}: {}", e.getMessage(), e);
        }

        return false;
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
