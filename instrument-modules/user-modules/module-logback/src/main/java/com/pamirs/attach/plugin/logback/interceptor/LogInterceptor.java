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

import ch.qos.logback.core.util.COWArrayList;
import com.pamirs.attach.plugin.logback.utils.AppenderHolder;
import com.pamirs.pradar.CutOffResult;
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
public class LogInterceptor extends CutoffInterceptorAdaptor {

    protected String bizShadowLogPath;
    private static volatile Field appenderListField;
    private static volatile Method doAppendMethod;
    private final Logger log = LoggerFactory.getLogger(LogInterceptor.class);

    public LogInterceptor(String bizShadowLogPath) {
        this.bizShadowLogPath = bizShadowLogPath;
    }

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        doLog(advice.getTarget(), advice.getParameterArray()[0]);
        return CutOffResult.cutoff(1);
    }

    private void doLog(Object appenderAttachable, Object event) {
        initAppenderListFieldField(appenderAttachable);
        List appenderList = Reflect.on(appenderAttachable).get(appenderListField);
        if (appenderList == null) {
            return;
        }
        ClassLoader bizClassLoader = appenderAttachable.getClass().getClassLoader();
        COWArrayList<Object> ptAppenderList = getPtAppenderList(appenderAttachable, appenderList, bizClassLoader);
        appendLoopOnAppenders(ptAppenderList, event);
    }

    private COWArrayList<Object> getPtAppenderList(Object appenderAttachable, List appenderList,
        ClassLoader bizClassLoader) {
        COWArrayList<Object> ptAppenderList = AppenderHolder.getPtAppenders(appenderAttachable);
        if (ptAppenderList == null) {
            synchronized (appenderAttachable) {
                ptAppenderList = AppenderHolder.getPtAppenders(appenderAttachable);
                if (ptAppenderList == null) {
                    ptAppenderList = new COWArrayList<Object>(new Object[0]);
                    for (Object appender : appenderList) {
                        String appenderName;
                        try {
                            appenderName = Reflect.on(appender).call("getName").get();
                        } catch (Exception e) {
                            log.warn("[logback] can not get appender name", e);
                            continue;
                        }
                        try {
                            if (Reflect.on(appender).call("isStarted").get()) {
                                Object ptAppender = AppenderHolder.getOrCreatePtAppender(bizClassLoader, appender,
                                    bizShadowLogPath);
                                if (ptAppender != null) {
                                    ptAppenderList.add(ptAppender);
                                }
                            } else {
                                log.warn("[logback] appender {} is not started! skip create pt appender", appenderName);
                            }
                        } catch (Exception e) {
                            log.warn("[logback] create pt appender : {} fail!", appenderName, e);
                        }
                    }
                    AppenderHolder.putPtAppenders(appenderAttachable, ptAppenderList);
                }
            }
        }
        return ptAppenderList;
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
            synchronized (LogInterceptor.class) {
                if (appenderListField == null) {
                    appenderListField = Reflect.on(appenderAttachable).field0("appenderList");
                }
            }
        }
    }

    private static void initDoAppendMethod(Object objectAppender) throws NoSuchMethodException {
        if (doAppendMethod == null) {
            synchronized (LogInterceptor.class) {
                if (doAppendMethod == null) {
                    doAppendMethod = Reflect.on(objectAppender).exactMethod("doAppend", new Class[] {Object.class});
                }
            }
        }
    }

}
