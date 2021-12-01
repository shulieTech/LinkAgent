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
package com.pamirs.attach.plugin.log4j.interceptor.v2.async;

import java.util.Collections;
import java.util.List;

import com.pamirs.attach.plugin.log4j.Log4jConstants;
import com.pamirs.attach.plugin.log4j.destroy.Log4jDestroy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.logging.log4j.core.config.Property;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/08 3:22 下午
 */
@Destroyable(Log4jDestroy.class)
public class AsyncTestMarkSetInterceptor extends ParametersWrapperInterceptorAdaptor {

    private final static Property TEST_FLAG_PROPERTY = Property.createProperty(Log4jConstants.TEST_MARK, "1");

    @Override
    protected Object[] getParameter0(Advice advice) throws Throwable {
        if (!Pradar.isClusterTest()) {
            return advice.getParameterArray();
        }
        Object[] args = advice.getParameterArray();
        if (args[5] == null) {
            args[5] = Collections.singletonList(TEST_FLAG_PROPERTY);
        } else {
            List<Property> properties = (List<Property>)args[5];
            properties.add(TEST_FLAG_PROPERTY);
        }
        return args;
    }
}
