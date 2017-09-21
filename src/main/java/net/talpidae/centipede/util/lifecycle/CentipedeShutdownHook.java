package net.talpidae.centipede.util.lifecycle;

import com.google.common.eventbus.EventBus;

import net.talpidae.base.event.Shutdown;
import net.talpidae.base.util.lifecycle.ShutdownHook;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.Frozen;
import net.talpidae.centipede.service.transition.TransitionDown;
import net.talpidae.centipede.task.state.StateMachine;
import net.talpidae.centipede.util.service.ServiceUtil;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import lombok.val;


@Slf4j
@Singleton
public class CentipedeShutdownHook extends ShutdownHook
{
    /**
     * Handle VM shutdown (cleanup).
     */
    @Inject
    public CentipedeShutdownHook(CentipedeRepository repository,
                                 EventBus eventBus,
                                 TransitionDown down)
    {
        super(() ->
        {
            final long maximumShutdownDelay = TimeUnit.SECONDS.toMillis(8);
            final long step = maximumShutdownDelay / StateMachine.TIMEOUT_TRANSITIONS;

            // perform fast shutdown of all locally running services
            try
            {
                // cycle through states faster than usual
                // to match VM shutdown timeout restrictions < 10s
                for (int i = 0; i < StateMachine.TIMEOUT_TRANSITIONS; ++i)
                {
                    if (i == TransitionDown.TRANSITION_COUNT_NOTIFY_PHASE)
                    {
                        // we need to do this to prevent scheduler task from launching services again
                        eventBus.post(new Frozen(true));
                    }
                    else if (i == TransitionDown.TRANSITION_COUNT_KILLING_PHASE)
                    {
                        // first, issue shutdown request to the UndertowServer, MessageExchange, GeneralScheduler, ...
                        eventBus.post(new Shutdown());
                    }

                    final int transitionCount = i;
                    repository.findAll()
                            .stream()
                            .filter(ServiceUtil::hasValidPid)
                            .forEach(localService ->
                            {
                                down.apply(localService, transitionCount);
                            });

                    Thread.sleep(step);
                }
            }
            catch (InterruptedException e)
            {
                log.warn("shutdown of local services interrupted");
            }
        });
    }
}