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
package com.pamirs.attach.plugin.dynamic;

import com.shulie.instrument.simulator.api.reflect.Reflect;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @Auther: vernon
 * @Date: 2021/8/17 23:20
 * @Description: getter/setter属性解析器
 */
public class AttributeResolver implements Resolver {

    Filter filter;

    public AttributeResolver() {
        this(new Filter.DefaultFilter());
    }

    public AttributeResolver(Filter filter) {
        this.filter = filter;

    }

    @Override
    public Map<String, Class<?>> resolver(Object t) {
        return resolver(t, Thread.currentThread().getContextClassLoader());

    }


    public Map<String, Class<?>> resolver(Object t, ClassLoader loader) {
        if (String.class.isAssignableFrom(t.getClass())) {
            Method[] methods = Reflect.on(t.toString(), loader).get().getClass().getDeclaredMethods();
            return (Map<String, Class<?>>) filter.filter(methods);
        } else {
            Method[] methods = Reflect.on(t).getClass().getDeclaredMethods();
            return (Map<String, Class<?>>) filter.filter(methods);
        }
    }

}
