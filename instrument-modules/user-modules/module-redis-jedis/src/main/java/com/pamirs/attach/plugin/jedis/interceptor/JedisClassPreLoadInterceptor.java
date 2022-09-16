package com.pamirs.attach.plugin.jedis.interceptor;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author Licey
 * @date 2022/9/16
 */
public class JedisClassPreLoadInterceptor extends AroundInterceptor {
    private boolean isLoaded = false;

    @Override
    public void doAfter(Advice advice) throws Throwable {
        if (isLoaded) {
            return;
        }
        preLoadClass(advice.getTarget().getClass().getClassLoader());
        isLoaded = true;
    }


    private void preLoadClass(ClassLoader classLoader){
        try {
            classLoader.loadClass("redis.clients.jedis.Jedis");
            classLoader.loadClass("redis.clients.jedis.Client");
            classLoader.loadClass("redis.clients.jedis.MultiKeyPipelineBase");
            classLoader.loadClass("redis.clients.jedis.PipelineBase");
            classLoader.loadClass("redis.clients.jedis.Connection");
        } catch (Throwable e) {
            //
        }
    }
}
