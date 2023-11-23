package com.pamirs.attach.plugin.lettuce.utils;


import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.lettuce.core.protocol.RedisCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
                        InvalidatedResourcesEvictor.releaseInvalidatedResources();
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

    public static void releaseInvalidatedResources() {
        Map<Object, Object> fields = ReflectionUtils.get(manager, "dynamicFields");;
        if (fields == null || fields.isEmpty()) {
            return;
        }
        Set<Object> needRemoved = new HashSet<Object>();
        for (Map.Entry<Object, Object> entry : fields.entrySet()) {
            Object key = entry.getKey();
            if (key.getClass().getSimpleName().startsWith("StatefulRedis")) {
                boolean isOpen = ReflectionUtils.invoke(key, "isOpen");
                if (!isOpen) {
                    needRemoved.add(key);
                }
            }
            if(key instanceof RedisCommand){
                if(((RedisCommand<?, ?, ?>) key).isDone()){
                    needRemoved.add(key);
                }
            }
        }
        for (Object key : needRemoved) {
            fields.remove(key);
        }

    }


}
