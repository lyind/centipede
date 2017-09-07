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

package net.talpidae.centipede.task.state;

import com.google.common.eventbus.EventBus;

import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.bean.service.State;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.ServicesModified;
import net.talpidae.centipede.service.transition.Transition;
import net.talpidae.centipede.service.transition.TransitionDown;
import net.talpidae.centipede.service.transition.TransitionUp;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static com.google.common.base.Objects.firstNonNull;


@Slf4j
@Singleton
public class StateMachine implements Runnable
{
    /**
     * Unsuccessful transition attempts after which we enter unknown state.
     */
    public static final int TIMEOUT_TRANSITIONS = 30;

    public static final long AFTER_TIMEOUT_COOLDOWN_DELAY_MS = TimeUnit.SECONDS.toMillis(45);

    private final CentipedeRepository centipedeRepository;

    private final EventBus eventBus;

    private final TransitionUp up;

    private final TransitionDown down;

    private final Map<String, State> transactionTargetState = new HashMap<>();


    @Inject
    public StateMachine(CentipedeRepository centipedeRepository, EventBus eventBus, TransitionUp up, TransitionDown down)
    {
        this.centipedeRepository = centipedeRepository;
        this.eventBus = eventBus;
        this.up = up;
        this.down = down;
    }

    @Override
    public void run()
    {
        for (val service : centipedeRepository.findAll())
        {
            try
            {
                // we temporarily cache the target state to prevent flukes
                val txnTargetState = transactionTargetState.get(service.getName());
                val targetState = firstNonNull(txnTargetState, service.getTargetState());
                val state = firstNonNull(service.getState(), State.UNKNOWN);

                if (state == txnTargetState)
                {
                    transactionTargetState.remove(service.getName());
                }

                switch (state)
                {
                    case UP:
                        if (targetState == State.DOWN)
                            undergoTransition(service, targetState, txnTargetState, down);

                        break;

                    case DOWN:
                        if (targetState == State.UP || targetState == State.OUT_OF_SERVICE)
                            undergoTransition(service, targetState, txnTargetState, up);

                        break;

                    case CHANGING:
                        if (targetState == State.OUT_OF_SERVICE)
                        {
                            undergoTransition(service, targetState, txnTargetState, up);
                            break;
                        }
                        // intended fall-through

                    case OUT_OF_SERVICE:
                        // resume startup/shutdown
                        switch (targetState)
                        {
                            case UP:
                                undergoTransition(service, targetState, txnTargetState, up);
                                break;

                            case DOWN:
                                undergoTransition(service, targetState, txnTargetState, down);
                                break;

                            default:
                                break;
                        }

                        break;

                    default:
                        if (targetState == State.DOWN)
                        {
                            // allow to bring down service from unknown state
                            undergoTransition(service, targetState, txnTargetState, down);
                        }
                        else if (txnTargetState != null)
                        {
                            // remove pinned target state (now changes are accepted, again)
                            transactionTargetState.remove(service.getName());
                        }

                        break;
                }
            }
            catch (Throwable e)
            {
                log.error("state machine error handling service: {}: {}", service.getName(), e.getMessage(), e);
            }
        }
    }


    private void undergoTransition(Service service, State targetState, State txnTargetState, Transition transition)
    {
        val name = service.getName();
        val currentTransition = firstNonNull(service.getTransition(), 0);

        if (currentTransition >= TIMEOUT_TRANSITIONS)
        {
            // timeout transition
            val coolDownEnd = service.getTs().plus(AFTER_TIMEOUT_COOLDOWN_DELAY_MS, ChronoUnit.MILLIS);
            if (OffsetDateTime.now().isAfter(coolDownEnd))
            {
                log.debug("cooldown phase for {} ended, current state: {}", name, service.getState().name());

                centipedeRepository.insertServiceState(Service.builder()
                        .name(name)
                        .state(State.UNKNOWN)
                        .transition(0)
                        .build());

                transactionTargetState.remove(name);
            }
            else if (currentTransition == TIMEOUT_TRANSITIONS)
            {
                log.warn("{} failed to transition to target state {}", name, targetState.name());
            }
        }
        else
        {
            if (txnTargetState == null)
            {
                // save target state to prevent rapid modification while the service is in transition
                transactionTargetState.put(name, targetState);

                setServiceStateChanging(service);
            }
            else
            {
                transition.apply(service, currentTransition);

                centipedeRepository.insertNextServiceTransition(service);
            }
        }
    }


    private void setServiceStateChanging(Service service)
    {
        val name = service.getName();

        centipedeRepository.insertServiceState(Service.builder()
                .name(name)
                .state(State.CHANGING)
                .transition(0)
                .build());

        eventBus.post(new ServicesModified(Collections.singletonList(name)));
    }
}
