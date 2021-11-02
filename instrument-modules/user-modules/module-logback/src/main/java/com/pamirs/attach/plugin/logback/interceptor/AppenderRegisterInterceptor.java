/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.logback.interceptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ch.qos.logback.core.util.COWArrayList;
import com.pamirs.attach.plugin.logback.utils.AppenderHolder;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Auther: vernon
 * @Date: 2020/12/7 23:37
 * @Description:
 */
@ListenerBehavior(isFilterClusterTest = true)
public class AppenderRegisterInterceptor extends CutoffInterceptorAdaptor {

    protected boolean isBusinessLogOpen;
    protected String bizShadowLogPath;
    private static volatile Field appenderListField;
    private static volatile Method doAppendMethod;
    private final Logger log = LoggerFactory.getLogger(AppenderRegisterInterceptor.class);

    private final static ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(Runtime.getRuntime()
        .availableProcessors(), Runtime.getRuntime().availableProcessors(),
        0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>(2048), new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "shadow-logback-worker-" + threadNumber.getAndIncrement());
            if (t.isDaemon()) {t.setDaemon(false);}
            if (t.getPriority() != Thread.NORM_PRIORITY) {t.setPriority(Thread.NORM_PRIORITY);}
            return t;
        }
    }, new ThreadPoolExecutor.DiscardPolicy());

    public AppenderRegisterInterceptor(boolean isBusinessLogOpen, String bizShadowLogPath) {
        this.isBusinessLogOpen = isBusinessLogOpen;
        this.bizShadowLogPath = bizShadowLogPath;
    }

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if (!isBusinessLogOpen) {
            return CutOffResult.passed();
        }
        if (!Pradar.isClusterTest()) {
            return CutOffResult.passed();
        }

        int clusterTestSamplingInterval = PradarSwitcher.getClusterTestSamplingInterval();
        //测试代码
        if (clusterTestSamplingInterval == 9527) {
            return CutOffResult.cutoff(1);
        }
        if (clusterTestSamplingInterval == 9526) {
            THREAD_POOL_EXECUTOR.submit(new LogRunnable(advice.getTarget(), advice.getParameterArray()[0]));
            return CutOffResult.cutoff(1);
        }

        new LogRunnable(advice.getTarget(), advice.getParameterArray()[0]).run();
        return CutOffResult.cutoff(1);
    }

    private final class LogRunnable implements Runnable {

        private final Object appenderAttachable;
        private final Object event;

        private LogRunnable(Object appenderAttachable, Object event) {
            this.appenderAttachable = appenderAttachable;
            this.event = event;
        }

        @Override
        public void run() {
            initAppenderListFieldField(appenderAttachable);
            List appenderList = Reflect.on(appenderAttachable).get(appenderListField);
            if (appenderList == null) {
                return;
            }
            ClassLoader bizClassLoader = appenderAttachable.getClass().getClassLoader();
            COWArrayList<Object> ptAppenderList = getPtAppenderList(appenderList, bizClassLoader);
            appendLoopOnAppenders(ptAppenderList, event);
        }

        private COWArrayList<Object> getPtAppenderList(List appenderList, ClassLoader bizClassLoader) {
            COWArrayList<Object> ptAppenderList = AppenderHolder.getPtAppenders(appenderAttachable);
            if (ptAppenderList == null) {
                synchronized (appenderAttachable) {
                    ptAppenderList = AppenderHolder.getPtAppenders(appenderAttachable);
                    if (ptAppenderList == null) {
                        ptAppenderList = new COWArrayList<Object>(new Object[0]);
                        for (Object appender : appenderList) {
                            try {
                                Object ptAppender = AppenderHolder.getOrCreatePtAppender(bizClassLoader, appender,
                                    bizShadowLogPath);
                                if (ptAppender != null) {
                                    ptAppenderList.add(ptAppender);
                                }
                            } catch (Exception e) {
                                log.warn("[logback] create pt appender fail!", e);
                            }
                        }
                        AppenderHolder.putPtAppenders(appenderAttachable, ptAppenderList);
                    }
                }
            }
            return ptAppenderList;
        }
    }

    public void appendLoopOnAppenders(COWArrayList<Object> ptAppenderList, Object e) {
        final Object[] appenderArray = ptAppenderList.asTypedArray();
        for (Object objectAppender : appenderArray) {
            try {
                initDoAppendMethod(objectAppender);
                doAppendMethod.invoke(objectAppender, e);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static void initAppenderListFieldField(Object appenderAttachable) {
        if (appenderListField == null) {
            synchronized (AppenderRegisterInterceptor.class) {
                if (appenderListField == null) {
                    appenderListField = Reflect.on(appenderAttachable).field0("appenderList");
                }
            }
        }
    }

    private static void initDoAppendMethod(Object objectAppender) throws NoSuchMethodException {
        if (doAppendMethod == null) {
            synchronized (AppenderRegisterInterceptor.class) {
                if (doAppendMethod == null) {
                    doAppendMethod = Reflect.on(objectAppender).exactMethod("doAppend", new Class[] {Object.class});
                }
            }
        }
    }

    public static void release(){
        THREAD_POOL_EXECUTOR.shutdownNow();
    }

}
