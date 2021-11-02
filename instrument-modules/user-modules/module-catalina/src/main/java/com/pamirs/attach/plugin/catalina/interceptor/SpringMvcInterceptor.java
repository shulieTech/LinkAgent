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
package com.pamirs.attach.plugin.catalina.interceptor;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Auther: vernon
 * @Date: 2020/4/8 00:24
 * @Description:
 */
public class SpringMvcInterceptor extends AroundInterceptor {

    @Override
    public void doBefore(Advice advice) {
        if (GlobalConfig.getInstance().getApis() != null &&
                GlobalConfig.getInstance().getApis().size() > 0){
            return;
        }
        Set<String> apis = new HashSet<String>();
        Map<RequestMappingInfo, HandlerMethod> arg = getArgs(advice.getTarget());
        if (arg.size() > 0){
            Set<RequestMappingInfo> sets = arg.keySet();
            for (RequestMappingInfo info : sets) {
                String url = info.getPatternsCondition().toString();
                String api = (url).substring(1, url.length() - 1);
                String type = info.getMethodsCondition().toString();
                apis.add(api + "#" + type);

            }
            GlobalConfig.getInstance().setApis(apis);
        }
    }

    private Map<RequestMappingInfo, HandlerMethod> getArgs(Object target){
        Method getMappingsMethod = null;
        boolean access = false;
        boolean r = true;
        try {
            getMappingsMethod = target.getClass().getDeclaredMethod("getMappings");
            access = getMappingsMethod.isAccessible();
            getMappingsMethod.setAccessible(true);
            Map<RequestMappingInfo, HandlerMethod> map = (Map<RequestMappingInfo, HandlerMethod>)getMappingsMethod.invoke(target);
            r = true;
            return map;
        } catch (Throwable t){
            LOGGER.error("getArgs error", t);
        } finally {
            if (getMappingsMethod != null && r){
                getMappingsMethod.setAccessible(access);
            }
        }
        return Collections.EMPTY_MAP;
    }


}
