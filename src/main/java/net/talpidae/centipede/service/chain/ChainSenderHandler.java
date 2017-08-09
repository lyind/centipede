package net.talpidae.centipede.service.chain;

import net.talpidae.base.util.queue.Enqueueable;

import javax.websocket.SendResult;


public interface ChainSenderHandler
{
    /**
     * The onElementResult() method must evaluate the SendResult, call next() and send().
     */
    void onElementResult(ChainSender chainSender, Enqueueable<?> current, SendResult result);
}
