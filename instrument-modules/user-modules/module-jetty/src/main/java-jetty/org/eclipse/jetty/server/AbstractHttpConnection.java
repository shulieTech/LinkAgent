package org.eclipse.jetty.server;

public abstract class AbstractHttpConnection {
    public Request getRequest() {
        return null;
    }

    public Response getResponse() {
        return null;
    }
}