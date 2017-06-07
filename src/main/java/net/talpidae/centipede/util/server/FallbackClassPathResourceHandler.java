package net.talpidae.centipede.util.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ResourceHandler;


@Singleton
public class FallbackClassPathResourceHandler extends ResourceHandler implements HttpHandler
{
    @Inject
    public FallbackClassPathResourceHandler(CentipedeClassPathResourceManager classPathResourceManager)
    {
        super(classPathResourceManager);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception
    {
        // rewrite to serve root/index.html instead of the (non-existing) relative path specified by the client
        exchange.setRelativePath("");
        exchange.setRequestPath("/");

        super.handleRequest(exchange);
    }
}
