package net.talpidae.centipede.service.chain;

import lombok.val;
import net.talpidae.base.util.queue.Enqueueable;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;


public class ChainSender implements SendHandler
{
    private final ChainSenderHandler sendHandler;

    private final AtomicReference<Iterator<? extends Enqueueable<?>>> eventsRef = new AtomicReference<>(null);

    private Enqueueable<?> current = null;

    public ChainSender(ChainSenderHandler sendHandler)
    {
        this.sendHandler = sendHandler;
    }


    @Override
    public void onResult(SendResult result)
    {
        val events = eventsRef.get();
        if (events != null)
        {
            val hadNext = events.hasNext();

            sendHandler.onElementResult(this, current, result);

            if (!hadNext || (result != null && !result.isOK()))
            {
                eventsRef.set(null);
            }
        }
    }


    /**
     * Return true if this sender WAS free at the time of the call, false otherwise.
     */
    public boolean isProbablyFree()
    {
        return eventsRef.get() == null;
    }


    /**
     * Enqueue the events for sending and start the chain.
     */
    public boolean enqueue(Iterator<? extends Enqueueable<?>> events)
    {
        if (eventsRef.compareAndSet(null, events))
        {
            onResult(null);

            return true;
        }

        return false;
    }


    public Enqueueable<?> next()
    {
        val events = eventsRef.get();
        return current = (events != null && events.hasNext())
                ? events.next()
                : null;
    }
}
