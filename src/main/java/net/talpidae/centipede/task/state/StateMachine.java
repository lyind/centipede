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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static com.google.common.base.Objects.firstNonNull;
import static net.talpidae.centipede.bean.service.State.CHANGING;
import static net.talpidae.centipede.bean.service.State.DOWN;
import static net.talpidae.centipede.bean.service.State.UP;


@Slf4j
@Singleton
public class StateMachine implements Runnable
{
    private static final Set<State> STABLE_STATES = EnumSet.of(DOWN, UP);

    /**
     * Unsuccessful transition attempts after which we enter unknown state.
     */
    private static final int TIMEOUT_TRANSITIONS = 30;

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
            // we temporarily cache the target state to prevent flukes
            val txnTargetState = transactionTargetState.get(service.getName());
            val targetState = firstNonNull(txnTargetState, service.getTargetState());
            if (STABLE_STATES.contains(targetState))
            {
                val state = firstNonNull(service.getState(), State.UNKNOWN);
                switch (state)
                {
                    case UP:
                        if (targetState == State.DOWN)
                            undergoTransition(service, targetState, down);

                        break;

                    case DOWN:
                        if (targetState == State.UP)
                            undergoTransition(service, targetState, up);

                        break;

                    case CHANGING:
                        // resume startup/shutdown
                        undergoTransition(service, targetState, (targetState == State.UP) ? up : down);
                        break;

                    default:
                        if (txnTargetState != null)
                        {
                            // remove pinned target state (now changes are accepted, again)
                            transactionTargetState.remove(service.getName());
                        }
                        else if (targetState == State.DOWN)
                        {
                            undergoTransition(service, targetState, down);
                        }

                        break;
                }
            }
        }
    }


    private void undergoTransition(Service service, State targetState, Transition transition)
    {
        final int transitionCount;
        if (service.getState() != CHANGING)
        {
            setServiceStateChanging(service, targetState);
            transitionCount = 0;
        }
        else if (service.getTransition() >= TIMEOUT_TRANSITIONS)
        {
            // timeout transition
            centipedeRepository.insertServiceState(Service.builder()
                    .name(service.getName())
                    .state(State.UNKNOWN)
                    .build());

            log.warn("{} failed to transition from {} to {}", service.getName(), service.getState().name(), targetState.name());
            return;
        }
        else
        {
            centipedeRepository.insertNextServiceTransition(service);
            transitionCount = firstNonNull(service.getTransition(), 1);
        }

        transition.apply(service, transitionCount);
    }


    private void setServiceStateChanging(Service service, State targetState)
    {
        // save target state to prevent rapid modification while the service is in transition
        transactionTargetState.put(service.getName(), targetState);

        centipedeRepository.insertServiceState(Service.builder()
                .name(service.getName())
                .state(State.CHANGING)
                .transition(0)
                .build());

        eventBus.post(new ServicesModified(Collections.singletonList(service.getName())));
    }
}
