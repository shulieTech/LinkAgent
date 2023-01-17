package com.pamirs.attach.plugin.async.http.client.plugin.utils;

import org.asynchttpclient.ListenableFuture;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class DefaultListenableFuture<V> implements ListenableFuture<V> {

    private V result;
    private Map<Runnable, Executor> listeners = new HashMap<Runnable, Executor>();

    private static Executor executor = Executors.newFixedThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("[agent-async-httpclient-thread]");
            return thread;
        }
    });

    public DefaultListenableFuture(V result) {
        this.result = result;
    }

    @Override
    public void done() {
        fireListeners();
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
        return new CompletableFuture(result);
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
    public V get() throws InterruptedException, ExecutionException {
        return result;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return result;
    }

    private void fireListeners() {
        for (Map.Entry<Runnable, Executor> entry : listeners.entrySet()) {
            entry.getValue().execute(entry.getKey());
        }
    }
}
