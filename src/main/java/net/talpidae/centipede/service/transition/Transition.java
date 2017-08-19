package net.talpidae.centipede.service.transition;

import net.talpidae.centipede.bean.service.Service;


public interface Transition
{
    boolean transition(Service service);
}
