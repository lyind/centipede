package net.talpidae.centipede.util.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.undertow.server.handlers.resource.ClassPathResourceManager;


@Singleton
public class CentipedeClassPathResourceManager extends ClassPathResourceManager
{
    @Inject
    public CentipedeClassPathResourceManager()
    {
        super(CentipedeClassPathResourceManager.class.getClassLoader(), "static/");
    }
}
