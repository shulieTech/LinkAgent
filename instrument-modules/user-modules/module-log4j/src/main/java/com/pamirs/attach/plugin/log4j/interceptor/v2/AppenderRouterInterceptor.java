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
package com.pamirs.attach.plugin.log4j.interceptor.v2;

import com.pamirs.attach.plugin.log4j.destroy.Log4jDestroy;
import com.pamirs.attach.plugin.log4j.message.WithTestFlagMessage;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.appender.mom.kafka.KafkaAppender;
import org.apache.logging.log4j.core.config.AppenderControl;

/**
 * @Auther: vernon
 * @Date: 2020/12/9 15:13
 * @Description:
 */
@Destroyable(Log4jDestroy.class)
public class AppenderRouterInterceptor extends CutoffInterceptorAdaptor {
    protected boolean isBusinessLogOpen;
    protected String bizShadowLogPath;
    private final String TEST_FLAG_MESSAGE_CLASS_NAME = WithTestFlagMessage.class.getName();

    public AppenderRouterInterceptor(boolean isBusinessLogOpen, String bizShadowLogPath) {
        this.isBusinessLogOpen = isBusinessLogOpen;
        this.bizShadowLogPath = bizShadowLogPath;
    }

    @Override
    public CutOffResult cutoff0(Advice advice) {
        if (!isBusinessLogOpen) {
            return CutOffResult.passed();
        }
        if (!(advice.getTarget() instanceof AppenderControl) ||
            !(advice.getParameterArray().length == 1 && advice.getParameterArray()[0] instanceof LogEvent)) {
            return CutOffResult.passed();
        }
        AppenderControl control = (AppenderControl)advice.getTarget();
        LogEvent logEvent = (LogEvent)advice.getParameterArray()[0];
        Appender appender = control.getAppender();
        if (appender == null) {
            return CutOffResult.passed();
        }
        if (isFileAppender(appender)) {
            return fileRoute(logEvent, appender);
        }
        if (isRemoteAppender(appender)) { //需要传播压测标的appender，目前只有kafka
            return testPropagate(logEvent, appender);
        }
        return CutOffResult.passed();
    }

    public CutOffResult testPropagate(LogEvent logEvent, Appender appender) {
        Pradar.setClusterTest(isClusterTest(logEvent));
        return CutOffResult.passed();
    }

    public boolean isRemoteAppender(Appender appender) {
        return appender instanceof KafkaAppender;
    }

    public boolean isFileAppender(Appender appender) {
        return appender instanceof FileAppender || appender instanceof RollingFileAppender
            || appender instanceof RollingRandomAccessFileAppender;
    }

    public CutOffResult fileRoute(LogEvent logEvent, Appender appender) {
        String name = appender.getName();
        if (isClusterTest(logEvent)) {
            return name.startsWith(Pradar.CLUSTER_TEST_PREFIX) ? CutOffResult.passed() : CutOffResult.cutoff(null);
        } else {
            return name.startsWith(Pradar.CLUSTER_TEST_PREFIX) ? CutOffResult.cutoff(null) : CutOffResult.passed();
        }
    }

    private boolean isClusterTest(LogEvent logEvent) {
        //注意：这里不要判断Pradar.isClusterTest()，因为testPropagate方法有可能已经污染了当前线程，按WithTestFlagMessage为准
        //由于类加载器的关系 instanceof WithTestFlagMessage 不安全
        return TEST_FLAG_MESSAGE_CLASS_NAME.equals(logEvent.getMessage().getClass().getName());
    }
}
