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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Auther: vernon
 * @Date: 2021/8/17 23:38
 * @Description:
 */
public interface Filter<R, V> {

    V filter(R r);

    class DefaultFilter<R, V> implements Filter {

        @Override
        public Object filter(Object o) {
            return o;
        }
    }

    class SetterFilter implements Filter {
        @Override
        public Object filter(Object o) {
            Map<String, Class<?>> names = new HashMap<String, Class<?>>();
            if (o instanceof Method[]) {
                Method[] methods = (Method[]) o;

                for (Method method : methods) {
                    if (method.getName().startsWith("set")) {
                        names.put(method.getName().substring(3), method.getParameterTypes()[0].getClass());
                    }
                }
            }
            return names;
        }

        ;
    }

    class GetterFilter implements Filter {
        @Override
        public Object filter(Object o) {
            Set<String> names = new HashSet<String>();
            if (o instanceof Method[]) {
                Method[] methods = (Method[]) o;

                for (Method method : methods) {
                    if (method.getName().startsWith("get")) {
                        names.add(method.getName().substring(3));
                    }
                }
            }
            return names;
        }

    }
}
