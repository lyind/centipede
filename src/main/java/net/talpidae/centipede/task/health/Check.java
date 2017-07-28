package net.talpidae.centipede.task.health;

import net.talpidae.centipede.CentipedeSyncQueen;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class Check implements Runnable
{
    private final CentipedeSyncQueen syncQueen;

    @Inject
    public Check(CentipedeSyncQueen syncQueen) {this.syncQueen = syncQueen;}

    @Override
    public void run()
    {

    }
}
