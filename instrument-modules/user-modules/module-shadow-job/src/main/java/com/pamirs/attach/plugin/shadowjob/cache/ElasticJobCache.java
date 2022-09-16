package com.pamirs.attach.plugin.shadowjob.cache;

import com.pamirs.attach.plugin.shadowjob.common.ShaDowJobConstant;
import com.pamirs.attach.plugin.shadowjob.obj.PtDataflowJob;
import com.pamirs.attach.plugin.shadowjob.obj.PtElasticJobSimpleJob;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.shulie.instrument.simulator.api.reflect.Reflect;
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

    public static Object registryCenter = null;

    public static void release() {
        if (registryCenter == null) {
            return;
        }
        ClassLoader currClassLoad = Thread.currentThread().getContextClassLoader();
        if (bizClassLoad != null) {
            Thread.currentThread().setContextClassLoader(bizClassLoad);
        }
        for (ShadowJob shadowJob : EXECUTE_JOB) {
            try {
                String ptClassName;
                if (ShaDowJobConstant.SIMPLE.equals(shadowJob.getJobDataType())) {
                    ptClassName = PtElasticJobSimpleJob.class.getName() + shadowJob.getClassName();
                } else {
                    ptClassName = PtDataflowJob.class.getName() + shadowJob.getClassName();
                }
                Reflect.on(registryCenter).call("remove", "/" + ptClassName);
            } catch (Throwable throwable) {
                logger.error("[elasticJob] disable error", throwable);
            }
        }
        EXECUTE_JOB.clear();
        Thread.currentThread().setContextClassLoader(currClassLoad);
    }
}
