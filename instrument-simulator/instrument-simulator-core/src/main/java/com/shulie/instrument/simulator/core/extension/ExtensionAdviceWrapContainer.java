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
package com.shulie.instrument.simulator.core.extension;

import com.shulie.instrument.simulator.api.extension.AdviceListenerWrap;
import com.shulie.instrument.simulator.api.extension.AdviceListenerWrapBuilder;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.listener.ext.AdviceListener;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/08/03 4:09 下午
 */
public class ExtensionAdviceWrapContainer extends AdviceListenerWrap {

    public ExtensionAdviceWrapContainer(AdviceListener adviceListener) {
        super(adviceListener);
    }

    @Override
    public void before(Advice advice) throws Throwable {
        AdviceListener adviceListener = wrapAdviceListener();
        adviceListener.before(advice);
    }

    @Override
    public void afterReturning(Advice advice) throws Throwable {
        AdviceListener adviceListener = wrapAdviceListener();
        adviceListener.afterReturning(advice);
    }

    @Override
    public void after(Advice advice) throws Throwable {
        AdviceListener adviceListener = wrapAdviceListener();
        adviceListener.after(advice);
    }

    @Override
    public void afterThrowing(Advice advice) throws Throwable {
        AdviceListener adviceListener = wrapAdviceListener();
        adviceListener.afterThrowing(advice);
    }

    @Override
    public void beforeCall(Advice advice, int callLineNum, boolean isInterface, String callJavaClassName,
        String callJavaMethodName, String callJavaMethodDesc) {
        AdviceListener adviceListener = wrapAdviceListener();
        adviceListener.beforeCall(advice, callLineNum, isInterface, callJavaClassName, callJavaMethodName,
            callJavaMethodDesc);
    }

    @Override
    public void afterCallReturning(Advice advice, int callLineNum, boolean isInterface, String callJavaClassName,
        String callJavaMethodName, String callJavaMethodDesc) {
        AdviceListener adviceListener = wrapAdviceListener();
        adviceListener.afterCallReturning(advice, callLineNum, isInterface, callJavaClassName, callJavaMethodName,
            callJavaMethodDesc);
    }

    @Override
    public void afterCallThrowing(Advice advice, int callLineNum, boolean isInterface, String callJavaClassName,
        String callJavaMethodName, String callJavaMethodDesc, Throwable callThrowable) {
        AdviceListener adviceListener = wrapAdviceListener();
        adviceListener.afterCallThrowing(advice, callLineNum, isInterface, callJavaClassName, callJavaMethodName,
            callJavaMethodDesc,
            callThrowable);
    }

    @Override
    public void afterCall(Advice advice, int callLineNum, String callJavaClassName, String callJavaMethodName,
        String callJavaMethodDesc, Throwable callThrowable) {
        AdviceListener adviceListener = wrapAdviceListener();
        adviceListener.afterCall(advice, callLineNum, callJavaClassName, callJavaMethodName, callJavaMethodDesc,
            callThrowable);
    }

    @Override
    public void beforeLine(Advice advice, int lineNum) {
        AdviceListener adviceListener = wrapAdviceListener();
        adviceListener.beforeLine(advice, lineNum);
    }

    private AdviceListener wrapAdviceListener() {
        AdviceListener adviceListener = this.adviceListener;
        for (AdviceListenerWrapBuilder adviceListenerWrapBuilder : GlobalAdviceWrapBuilders.getAdviceListenerWrapBuilders()) {
/*
             adviceListener = adviceListenerWrapBuilder.build(adviceListener);
*/
        }
        return adviceListener;
    }
}
