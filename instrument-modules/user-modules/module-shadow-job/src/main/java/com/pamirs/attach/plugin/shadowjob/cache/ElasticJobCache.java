package com.pamirs.attach.plugin.shadowjob.cache;

import com.pamirs.attach.plugin.shadowjob.interceptor.JobExecutorFactoryGetJobExecutorInterceptor;
import com.pamirs.pradar.internal.config.ShadowJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/9/7 15:14
 */
public class ElasticJobCache {
    private static final Logger logger = LoggerFactory.getLogger(ElasticJobCache.class);

    public static Set<ShadowJob> EXECUTE_JOB = new HashSet();

    public static ClassLoader bizClassLoad = null;

    public static void release() {
        ClassLoader currClassLoad = Thread.currentThread().getContextClassLoader();
        if (bizClassLoad != null) {
            Thread.currentThread().setContextClassLoader(bizClassLoad);
        }
        for (ShadowJob shadowJob : EXECUTE_JOB) {
            try {
                JobExecutorFactoryGetJobExecutorInterceptor.disableShaDowJob(shadowJob);
            } catch (Throwable throwable) {
                logger.error("[elasticJob] disable error", throwable);
            }
        }
        EXECUTE_JOB.clear();
        Thread.currentThread().setContextClassLoader(currClassLoad);
    }
}
