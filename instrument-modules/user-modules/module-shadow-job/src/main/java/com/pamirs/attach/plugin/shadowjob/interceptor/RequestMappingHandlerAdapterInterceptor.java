/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.shadowjob.interceptor;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author angju
 * @date 2021/3/22 18:02
 */
public class RequestMappingHandlerAdapterInterceptor extends AroundInterceptor {
    private volatile static AtomicBoolean isInited = new AtomicBoolean(false);

    private static final String contextRefreshedEnabledKey = "agent.spring.context.refreshed.enable";

    /**
     * 是否允许Spring上下文刷新, agent-test2.0里有很多刷新Spring容器的场景, 仅供测试用例使用
     */
    private static final boolean enableRefreshApplicationContext = System.getProperty(contextRefreshedEnabledKey) != null ? Boolean.parseBoolean(System.getProperty(contextRefreshedEnabledKey)) : false;

    @Override
    public void doBefore(Advice advice) {
        Object args[] = advice.getParameterArray();
        HttpServletRequest httpServletRequest = (HttpServletRequest) args[0];
        if (!enableRefreshApplicationContext) {
            // 不允许刷新,则只自行一次, 否则每次都刷新
            if (!isInited.compareAndSet(false, true)) {
                return;
            }
        }
        try {
            PradarSpringUtil.refreshBeanFactory(WebApplicationContextUtils.findWebApplicationContext(httpServletRequest.getServletContext()));
        } catch (Throwable e) {
            try {
                //spring 4.0
                if (WebApplicationContextUtils.getWebApplicationContext(httpServletRequest.getServletContext()) == null) {
                    ApplicationContext applicationContext = (ApplicationContext) httpServletRequest.getServletContext().getAttribute("org.springframework.web.servlet.FrameworkServlet.CONTEXT.dispatcherServlet");
                    PradarSpringUtil.refreshBeanFactory(applicationContext);
                } else {
                    PradarSpringUtil.refreshBeanFactory(WebApplicationContextUtils.getWebApplicationContext(httpServletRequest.getServletContext()));
                }
            } catch (Throwable e1) {
                isInited.set(false);
            }
        }
    }

}
