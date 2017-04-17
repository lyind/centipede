package net.talpidae.centipede;

import com.google.common.eventbus.EventBus;
import net.talpidae.base.insect.SyncQueen;
import net.talpidae.base.insect.config.QueenSettings;
import net.talpidae.base.insect.message.payload.Mapping;
import net.talpidae.centipede.event.NewMapping;

import javax.inject.Inject;


public class CentipedeSyncQueen extends SyncQueen
{
    private final EventBus eventBus;

    @Inject
    public CentipedeSyncQueen(QueenSettings settings, EventBus eventBus)
    {
        super(settings);

        this.eventBus = eventBus;
    }


    @Override
    protected void postHandleMapping(Mapping mapping, boolean isNewMapping)
    {
        super.postHandleMapping(mapping, isNewMapping);

        // update DB state
        if (isNewMapping)
        {
            eventBus.post(new NewMapping(mapping));
        }
    }
}
