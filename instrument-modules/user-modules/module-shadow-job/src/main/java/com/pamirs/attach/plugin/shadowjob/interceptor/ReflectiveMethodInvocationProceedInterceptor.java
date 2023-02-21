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
package com.pamirs.attach.plugin.shadowjob.interceptor;

import com.pamirs.attach.plugin.shadowjob.ShadowJobConstants;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectionUtils;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/3/23 11:33 上午
 */
public class ReflectiveMethodInvocationProceedInterceptor extends AroundInterceptor {
    private volatile Field interceptorsField;
    private volatile Field beanFactoryField;
    private AtomicBoolean isInited = new AtomicBoolean(false);

    /**
     * 只初始化一次
     */
    private void init() {
        try {
            interceptorsField = ReflectiveMethodInvocation.class.getDeclaredField(ShadowJobConstants.DYNAMIC_FIELD_INTERCEPTORS_AND_DYNAMIC_METHOD_MATCHERS);
            interceptorsField.setAccessible(true);
        } catch (Throwable e) {
        }

        try {
            beanFactoryField = TransactionAspectSupport.class.getDeclaredField(ShadowJobConstants.DYNAMIC_FIELD_BEAN_FACTORY);
            beanFactoryField.setAccessible(true);
        } catch (Throwable e) {
        }
    }

    private List getInterceptors(Object target) {
        try {
            return (List) interceptorsField.get(target);
        } catch (Throwable e) {
            return null;
        }
    }

    private BeanFactory getBeanFactory(Object target) {
        try {
            return (BeanFactory) beanFactoryField.get(target);
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public void doBefore(Advice advice) throws Throwable {
        // 只需要执行一次，不然会有严重的性能损耗
        if (!isInited.compareAndSet(false, true)) {
            return;
        }
        if (PradarSpringUtil.isInit()) {
            return;
        }
        /**
         * 如果找不到就不执行了
         */
        init();
        /**
         * 如果初始化属性失败则就不找了
         */
        if (interceptorsField == null || beanFactoryField == null) {
            return;
        }

        List interceptorsOfList = getInterceptors(advice.getTarget());
        if (interceptorsOfList == null || interceptorsOfList.isEmpty()) {
            return;
        }

        for (Object interceptor : interceptorsOfList) {
            if (interceptor instanceof TransactionInterceptor) {
                TransactionInterceptor transactionInterceptor = (TransactionInterceptor) interceptor;
                BeanFactory beanFactory = _getBeanFactory(transactionInterceptor);
                if (beanFactory != null && beanFactory instanceof DefaultListableBeanFactory) {
                    PradarSpringUtil.refreshBeanFactory((DefaultListableBeanFactory) beanFactory);
                    break;
                }
            } else {
                BeanFactory beanFactory = _getBeanFactory(interceptor);
                if (beanFactory != null && beanFactory instanceof DefaultListableBeanFactory) {
                    PradarSpringUtil.refreshBeanFactory((DefaultListableBeanFactory) beanFactory);
                    break;
                }
            }
        }
    }


    private BeanFactory _getBeanFactory(Object target) {
        Field beanFactory = ReflectionUtils.findField(target.getClass(), "beanFactory");
        if (beanFactory == null) {
            return null;
        }
        return ReflectionUtils.getField(beanFactory, target);
    }
}
