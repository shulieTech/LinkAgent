package com.pamirs.attach.plugin.async.http.client.plugin.utils;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class DefaultListenableFuture implements ListenableFuture {

    private final static Logger logger = LoggerFactory.getLogger(DefaultListenableFuture.class);

    private Response result;
    private AsyncCompletionHandler<Response> handler;
    private Map<Runnable, Executor> listeners = new HashMap<Runnable, Executor>();

    private static Executor executor = Executors.newFixedThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("[agent-async-httpclient-thread]");
            return thread;
        }
    });

    public DefaultListenableFuture(Object result, AsyncCompletionHandler<Response> handler) {
        this.result = new DefaultAsyncResponse(result);
        this.handler = handler;
    }

    @Override
    public void done() {
        fireListeners();
        if (handler != null) {
            try {
                handler.onCompleted(result);
            } catch (Exception e) {
                logger.error("[async-httpclient] invoke async handler onCompleted occur exception", e);
            }
        }
    }

    @Override
    public void abort(Throwable t) {

    }

    @Override
    public void touch() {

    }

    @Override
    public ListenableFuture addListener(Runnable listener, Executor exec) {
        listeners.put(listener, exec != null ? exec : executor);
        return this;
    }

    @Override
    public CompletableFuture toCompletableFuture() {
        CompletableFuture future = new CompletableFuture();
        ReflectionUtils.set(future, "result", result);
        return future;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public Response get() throws InterruptedException, ExecutionException {
        return result;
    }

    @Override
    public Response get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return result;
    }

    private void fireListeners() {
        for (Map.Entry<Runnable, Executor> entry : listeners.entrySet()) {
            entry.getValue().execute(entry.getKey());
        }
    }
}
