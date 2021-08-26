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
package com.shulie.instrument.simulator.core.util.collection;

import java.lang.ref.WeakReference;

public class Pair {

    private WeakReference<ClassLoader> classloaderHolder;
    private String javaClassName;

    public Pair(ClassLoader classLoader, String javaClassName) {
        if (classLoader != null) {
            classloaderHolder = new WeakReference<ClassLoader>(classLoader);
        }
        this.javaClassName = javaClassName;
    }

    public void clear() {
        if (classloaderHolder != null) {
            classloaderHolder.clear();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair pair = (Pair) o;

        if (classloaderHolder != null ? !classloaderHolder.equals(pair.classloaderHolder) : pair.classloaderHolder != null)
            return false;
        return javaClassName != null ? javaClassName.equals(pair.javaClassName) : pair.javaClassName == null;
    }

    @Override
    public int hashCode() {
        int result = classloaderHolder != null ? classloaderHolder.hashCode() : 0;
        result = 31 * result + (javaClassName != null ? javaClassName.hashCode() : 0);
        return result;
    }
}
