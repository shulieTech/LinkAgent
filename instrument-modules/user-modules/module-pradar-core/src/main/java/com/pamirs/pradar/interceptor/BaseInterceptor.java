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
package com.pamirs.pradar.interceptor;

import com.pamirs.pradar.Pradar;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.listener.ext.AdviceListener;
import com.shulie.instrument.simulator.api.scope.InterceptorScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 拦截器
 * <pre>
 *     注意:实现的这些方法不支持重载,否则会抛出异常RuntimeException
 * </pre>
 *
 * @author fabing.zhaofb
 */
abstract class BaseInterceptor extends AdviceListener {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseInterceptor.class);

    protected InterceptorScope interceptorScope;
    protected boolean costEnabled;
    protected long minInterceptorCost;

    public BaseInterceptor() {
        this.costEnabled = Pradar.isInterceptorCostEnabled();
        this.minInterceptorCost = Pradar.getMinInterceptorCost();
    }

    public void setInterceptorScope(InterceptorScope interceptorScope) {
        this.interceptorScope = interceptorScope;
    }

    @Override
    public void before(Advice advice) throws Throwable {
        long start = 0L;
        if (costEnabled) {
            start = System.nanoTime();
        }
        Throwable e = null;
        try {
            doBefore(advice);
        } catch (ProcessControlException t) {
            throw t;
        } catch (Throwable t) {
            e = t;
            throw t;
        } finally {
            if (costEnabled) {
                long end = System.nanoTime();
                double cost = (end - start) / 1000000.0;
                if (cost > minInterceptorCost) {
                    LOGGER.info("interceptor execute before cost interceptor={}, cost={}ms", getClass().getName(), cost);
                }
            }
        }
    }

    @Override
    public void afterReturning(Advice advice) throws Throwable {
        long start = 0L;
        if (costEnabled) {
            start = System.nanoTime();
        }
        boolean isDebug = Pradar.isDebug();
        Throwable throwable = null;
        try {
            doAfter(advice);
            if (advice.attachment() != null && advice.attachment() instanceof Throwable) {
                throwable = advice.attachment();
                advice.attach(null);
            }
        } catch (ProcessControlException e) {
            throw e;
        } catch (Throwable e) {
            throwable = e;
            throw e;
        } finally {
            if (costEnabled) {
                long end = System.nanoTime();
                double cost = (end - start) / 1000000.0;
                if (cost > minInterceptorCost) {
                    LOGGER.info("interceptor execute after cost interceptor={}, cost={}ms", getClass().getName(), cost);
                }
            }
        }
    }

    @Override
    public void afterThrowing(Advice advice) throws Throwable {
        long start = 0L;
        if (costEnabled) {
            start = System.nanoTime();
        }
        boolean isDebug = Pradar.isDebug();
        Throwable throwable = null;
        try {
            doException(advice);
        } catch (ProcessControlException e) {
            throw e;
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            if (costEnabled) {
                long end = System.nanoTime();
                double cost = (end - start) / 1000000.0;
                if (cost > minInterceptorCost) {
                    LOGGER.info("interceptor execute exception cost interceptor={}, cost={}ms", getClass().getName(), cost);
                }
            }
        }
    }

    /**
     * 方法调用开始
     *
     * @param advice
     * @throws Throwable
     */
    public void doBefore(Advice advice) throws Throwable {

    }

    /**
     * 方法调用结束
     * 如果在 {@link #doBefore(Advice)} 时
     * 强行返回结果或者强行抛出异常，则此方法不会再被执行
     *
     * @param advice
     * @throws Throwable
     */
    public void doAfter(Advice advice) throws Throwable {

    }

    /**
     * 方法出现异常时调用
     * 如果在 {@link #doBefore(Advice)} 时
     * 强行返回结果或者强行抛出异常，则此方法不会再被执行
     *
     * @param advice
     * @throws Throwable
     */
    public void doException(Advice advice) throws Throwable {

    }
}

