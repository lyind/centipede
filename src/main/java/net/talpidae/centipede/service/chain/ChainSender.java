package net.talpidae.centipede.service.chain;

import lombok.val;
import net.talpidae.base.util.queue.Enqueueable;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;


public class ChainSender implements SendHandler
{
    private final AtomicBoolean isFree = new AtomicBoolean(true);

    private final ChainSenderHandler sendHandler;

    private Iterator<? extends Enqueueable> events;


    public ChainSender(ChainSenderHandler sendHandler)
    {
        this.sendHandler = sendHandler;
    }


    @Override
    public void onResult(SendResult result)
    {
        val hadNext = events.hasNext();

        sendHandler.onElementResult(this, result);

        if (!hadNext || !result.isOK())
        {
            isFree.set(true);
        }
    }


    /**
     * Acquire this ChainSender. Call before adding new message via enqueue().
     */
    public boolean acquire()
    {
        return isFree.compareAndSet(true, false);
    }


    /**
     * Enqueue the events for sending and start the chain.
     */
    public void enqueue(Iterator<? extends Enqueueable> events)
    {
        this.events = events;

        onResult(null);
    }


    public Enqueueable<?> next()
    {
        return (events.hasNext()) ? events.next() : null;
    }
}
