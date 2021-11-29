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
package com.pamirs.attach.plugin.log4j.interceptor.v2.holder;

import com.pamirs.attach.plugin.log4j.interceptor.utils.BeanUtil;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mocheng
 * @since 2021/09/17 16:47
 */
public class TriggerPolicyHolder {

    static Logger logger = LoggerFactory.getLogger(TriggerPolicyHolder.class);

    public static Object copyTriggerPolicy(Object ptManager,Object compositeTriggeringPolicy) {
        Object ptCompositeTriggeringPolicy = null;
        if (compositeTriggeringPolicy != null) {
            Object[] triggeringPolicies = Reflect.on(compositeTriggeringPolicy).get("triggeringPolicies");
            if (triggeringPolicies != null && triggeringPolicies.length > 0) {
                TriggeringPolicy[] ptTriggeringPolicies = new TriggeringPolicy[triggeringPolicies.length];
                for(int i = 0; i < triggeringPolicies.length; i++) {
                    TriggeringPolicy ptTriggeringPolicy;
                    Object triggeringPolicy = triggeringPolicies[i];
                    if (triggeringPolicy != null) {
                        ptTriggeringPolicy = (TriggeringPolicy)BeanUtil.copyBean(triggeringPolicy);
                        if (ptTriggeringPolicy != null) {
                            Reflect.on(ptTriggeringPolicy).set("manager", ptManager);
                            Reflect.on(ptTriggeringPolicy).set("state", Reflect.on(triggeringPolicy).get("state"));
                            ptTriggeringPolicies[i] = ptTriggeringPolicy;
                        } else {
                            logger.error("[Log4j-Plugin] not adapted. copyTriggerPolicy fail, TriggerPolicy ClassNotFound: {}", triggeringPolicy.getClass().getName());
                        }
                    }
                }
                ptCompositeTriggeringPolicy = Reflect.on(compositeTriggeringPolicy).call("createPolicy", (Object)ptTriggeringPolicies).get();
                Reflect.on(ptCompositeTriggeringPolicy).call("start");
            }
        }
        return ptCompositeTriggeringPolicy;
    }
}
