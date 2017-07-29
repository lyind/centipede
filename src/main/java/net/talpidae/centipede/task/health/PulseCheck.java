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

package net.talpidae.centipede.task.health;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.talpidae.base.insect.SyncQueen;
import net.talpidae.base.insect.config.QueenSettings;
import net.talpidae.base.insect.state.InsectState;
import net.talpidae.centipede.bean.service.Service;
import net.talpidae.centipede.bean.service.State;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.ServicesModified;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


@Slf4j
@Singleton
public class PulseCheck implements Runnable
{
    private final SyncQueen syncQueen;

    private final QueenSettings queenSettings;

    private final CentipedeRepository centipedeRepository;

    private final Set<String> upServiceNames = new HashSet<>();

    private final EventBus eventBus;


    @Inject
    public PulseCheck(SyncQueen syncQueen, QueenSettings queenSettings, CentipedeRepository centipedeRepository, EventBus eventBus)
    {
        this.syncQueen = syncQueen;
        this.queenSettings = queenSettings;
        this.centipedeRepository = centipedeRepository;
        this.eventBus = eventBus;
    }


    /**
     * Is the service in a stable state?
     */
    private static boolean isStableAndNotDown(Service service)
    {
        val state = service.getState();

        return !State.CHANGING.equals(state) && !State.DOWN.equals(state);
    }


    /**
     * Is the service in a stable state?
     */
    private static boolean isNotUp(Service service)
    {
        return !State.UP.equals(service.getState());
    }


    /**
     * Compares current state with the database and updates it accordingly.
     */
    @Override
    public void run()
    {
        upServiceNames.clear();

        try
        {
            val modifiedNames = new ArrayList<String>();

            getUpInsectState()
                    .map(InsectState::getName)
                    .map(centipedeRepository::findServiceByName)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(service ->
                    {
                        upServiceNames.add(service.getName());

                        if (!State.UP.equals(service.getState()))
                        {
                            centipedeRepository.insertServiceState(Service.builder()
                                    .name(service.getName())
                                    .state(State.UP).build());

                            modifiedNames.add(service.getName());
                        }
                    });

            // set all other services to down (if they are not currently changing)
            centipedeRepository.findAllNames()
                    .stream()
                    .filter(name -> !upServiceNames.contains(name))
                    .map(centipedeRepository::findServiceByName)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(PulseCheck::isStableAndNotDown)
                    .map(service -> Service.builder().name(service.getName()).state(State.DOWN).build())
                    .forEach(service ->
                    {
                        centipedeRepository.insertServiceState(service);

                        modifiedNames.add(service.getName());
                    });

            if (!modifiedNames.isEmpty())
            {
                eventBus.post(new ServicesModified(Collections.unmodifiableList(modifiedNames)));
            }
        }
        catch (Throwable t)
        {
            log.error("failed to check service pulse: {}", t.getMessage(), t);
        }
    }


    private Stream<InsectState> getUpInsectState()
    {
        val noPulseDownThresholdNanos = TimeUnit.MILLISECONDS.toNanos(queenSettings.getPulseDelay() * 3L);

        // fill state cache with insect that recently showed a pulse
        val now = System.nanoTime();
        return syncQueen.getLiveInsectState()
                .filter(state -> (now - state.getTimestamp()) < noPulseDownThresholdNanos);
    }
}
