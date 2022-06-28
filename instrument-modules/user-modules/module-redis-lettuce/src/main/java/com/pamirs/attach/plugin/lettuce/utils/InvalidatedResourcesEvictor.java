package com.pamirs.attach.plugin.lettuce.utils;


import com.pamirs.attach.plugin.dynamic.reflect.Reflect;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class InvalidatedResourcesEvictor implements Runnable {

    private static AtomicBoolean started = new AtomicBoolean(false);

    protected static DynamicFieldManager manager;

    private final static Logger LOGGER = LoggerFactory.getLogger(InvalidatedResourcesEvictor.class.getName());

    private static ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "DynamicFieldManager-invalidated-resources-evictor");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    LOGGER.error("Thread {} caught a unknow exception with UncaughtExceptionHandler", t.getName(), e);
                }
            });
            return t;
        }
    });

    @Override
    public void run() {
        Map<Object, Object> fields = Reflect.on(manager).get("dynamicFields");

        Set<Object> needRemoved = new HashSet<Object>();
        for (Map.Entry<Object, Object> entry : fields.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof StatefulRedisConnection) {
                StatefulRedisConnection conn = (StatefulRedisConnection) key;
                if (!conn.isOpen()) {
                    needRemoved.add(key);
                }
            }
        }
        for (Object key : needRemoved) {
            fields.remove(key);
        }

    }

    public static void scheduleDropInvalidatedResources(DynamicFieldManager manager) {
        if (started.compareAndSet(false, true)) {
            InvalidatedResourcesEvictor.manager = manager;
            service.scheduleAtFixedRate(new InvalidatedResourcesEvictor(), 3, 3, TimeUnit.MINUTES);
        }
    }


}
