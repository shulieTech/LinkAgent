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
package com.shulie.instrument.simulator.api.listener.ext;

import com.shulie.instrument.simulator.api.util.BehaviorDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;

/**
 * 延迟的行为的构造器实现
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/3/5 4:59 下午
 */
public class Constructor implements Behavior {
    private volatile java.lang.reflect.Constructor<?> target;
    private volatile boolean isInit;
    private final Class clazz;
    private final String javaMethodDesc;


    public Constructor(Class clazz, String javaMethodDesc) {
        this.clazz = clazz;
        this.javaMethodDesc = javaMethodDesc;
    }

    private void init() {
        if (isInit) {
            return;
        }
        for (final java.lang.reflect.Constructor constructor : clazz.getDeclaredConstructors()) {
            if (javaMethodDesc.equals(new BehaviorDescriptor(constructor).getDescriptor())) {
                this.target = constructor;
                break;
            }
        }
        isInit = true;
    }


    @Override
    public Object invoke(Object obj, Object... args)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (!isInit) {
            init();
        }
        return target.newInstance(args);
    }

    @Override
    public boolean isAccessible() {
        if (!isInit) {
            init();
        }
        return target.isAccessible();
    }

    @Override
    public void setAccessible(boolean accessFlag) {
        if (!isInit) {
            init();
        }
        target.setAccessible(accessFlag);
    }

    @Override
    public String getName() {
        return "<init>";
    }

    @Override
    public Class<?>[] getParameterTypes() {
        if (!isInit) {
            init();
        }
        return target.getParameterTypes();
    }

    @Override
    public Annotation[] getAnnotations() {
        if (!isInit) {
            init();
        }
        return target.getAnnotations();
    }

    @Override
    public int getModifiers() {
        if (!isInit) {
            init();
        }
        return target.getModifiers();
    }

    @Override
    public Class<?> getDeclaringClass() {
        if (!isInit) {
            init();
        }
        return target.getDeclaringClass();
    }

    @Override
    public Class<?> getReturnType() {
        if (!isInit) {
            init();
        }
        return target.getDeclaringClass();
    }

    @Override
    public Class<?>[] getExceptionTypes() {
        if (!isInit) {
            init();
        }
        return target.getExceptionTypes();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        if (!isInit) {
            init();
        }
        return target.getDeclaredAnnotations();
    }

    @Override
    public AccessibleObject getTarget() {
        if (!isInit) {
            init();
        }
        return target;
    }

    @Override
    public int hashCode() {
        if (!isInit) {
            init();
        }
        return target.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!isInit) {
            init();
        }
        return target.equals(obj);
    }

    @Override
    public String toString() {
        if (!isInit) {
            init();
        }
        return target.toString();
    }
}
