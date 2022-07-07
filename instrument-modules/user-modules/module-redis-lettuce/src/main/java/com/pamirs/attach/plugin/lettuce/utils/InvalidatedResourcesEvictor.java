package com.pamirs.attach.plugin.lettuce.utils;


import com.pamirs.attach.plugin.dynamic.reflect.Reflect;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.lettuce.core.api.StatefulConnection;
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

public class InvalidatedResourcesEvictor {

    protected static DynamicFieldManager manager;

    private final static Logger LOGGER = LoggerFactory.getLogger(InvalidatedResourcesEvictor.class.getName());

    public static void scheduleDropInvalidatedResources(DynamicFieldManager manager) {
        InvalidatedResourcesEvictor.manager = manager;
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        InvalidatedResourcesEvictor.run();
                        LOGGER.info("Invalidated-Resources-Evictor-Job done");
                        Thread.sleep(3 * 60 * 1000);
                    } catch (Exception e) {
                        LOGGER.error("Invalidated-Resources-Evictor-Job invoke occur exception", e);
                    }
                }
            }
        };
        thread.setName("Invalidated-Resources-Evictor-Job");
        thread.start();
    }

    public static void run() {
        Map<Object, Object> fields = Reflect.on(manager).get("dynamicFields");
        if (fields == null || fields.isEmpty()) {
            return;
        }
        Set<Object> needRemoved = new HashSet<Object>();
        for (Map.Entry<Object, Object> entry : fields.entrySet()) {
            Object key = entry.getKey();
            if (key.getClass().getSimpleName().startsWith("StatefulRedis")) {
                boolean isOpen = Reflect.on(key).call("isOpen").get();
                if (!isOpen) {
                    needRemoved.add(key);
                }
            }
        }
        for (Object key : needRemoved) {
            fields.remove(key);
        }

    }


}
