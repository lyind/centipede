package net.talpidae.centipede.util.server;

import io.undertow.predicate.Predicate;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.resource.ResourceHandler;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class CentipedeRootHandlerWrapper implements HandlerWrapper
{
    private final HttpHandler resourceHandler;

    @Inject
    public CentipedeRootHandlerWrapper(CentipedeClassPathResourceManager classPathResourceManager, FallbackClassPathResourceHandler fallbackClassPathResourceHandler)
    {
        resourceHandler = new ResourceHandler(classPathResourceManager, fallbackClassPathResourceHandler);
    }


    @Override
    public HttpHandler wrap(HttpHandler handler)
    {
        return new PredicateHandler(new IsUpgradePredicate(), handler, resourceHandler);
    }


    /**
     * Tests if an HTTP upgrade has been requested.
     */
    private static class IsUpgradePredicate implements Predicate
    {
        @Override
        public boolean resolve(HttpServerExchange value)
        {
            val upgradeHeader = value.getRequestHeaders().get("UPGRADE");
            return (upgradeHeader != null && !upgradeHeader.isEmpty());
        }
    }
}
