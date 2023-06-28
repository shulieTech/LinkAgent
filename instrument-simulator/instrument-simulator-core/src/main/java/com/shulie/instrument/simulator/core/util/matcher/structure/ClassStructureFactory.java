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
package com.shulie.instrument.simulator.core.util.matcher.structure;

import com.google.common.collect.HashBasedTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类结构工厂类
 * <p>
 * 根据构造方式的不同，返回的实现方式也不一样。但无论哪一种实现方式都尽可能符合接口约定。
 * </p>
 */
public class ClassStructureFactory {

    private static final Logger logger = LoggerFactory.getLogger(ClassStructureFactory.class);

    private static HashBasedTable<Object, Integer, ClassStructure> classStructureCache = HashBasedTable.create(2048, 1);

    private static AtomicInteger callNum = new AtomicInteger();

    /**
     * 通过Class类来构造类结构
     *
     * @param clazz 目标Class类
     * @return JDK实现的类结构
     */
    public static ClassStructure createClassStructure(final Class<?> clazz) {
        callNum.incrementAndGet();
        ClassLoader loader = clazz.getClassLoader();
        int hashCode = loader == null ? 0 : loader.hashCode();
        ClassStructure classStructure = classStructureCache.get(clazz, hashCode);
        if (classStructure == null) {
            classStructure = new JdkClassStructure(clazz);
            classStructureCache.put(clazz, hashCode, classStructure);
        }
        return classStructure;
    }

    /**
     * 通过Class类字节流来构造类结构
     *
     * @param classInputStream Class类字节流
     * @param loader           即将装载Class的ClassLoader
     * @return ASM实现的类结构
     */
    public static ClassStructure createClassStructure(final InputStream classInputStream, final ClassLoader loader) {
        callNum.incrementAndGet();
        int hashCode = loader == null ? 0 : loader.hashCode();
        try {
            ClassStructure classStructure = classStructureCache.get(classInputStream, hashCode);
            if (classStructure == null) {
                classStructure = new AsmClassStructure(classInputStream, loader);
                classStructureCache.put(classInputStream, hashCode, classStructure);
            }
            return classStructure;
        } catch (IOException cause) {
            logger.warn("SIMULATOR: create class structure failed by using ASM, return null. loader={};", loader, cause);
            return null;
        }
    }

    /**
     * 通过Class类字节数组来构造类结构
     *
     * @param classByteArray Class类字节数组
     * @param loader         即将装载Class的ClassLoader
     * @return ASM实现的类结构
     */
    public static ClassStructure createClassStructure(final byte[] classByteArray, final ClassLoader loader) {
        callNum.incrementAndGet();
        int hashCode = loader == null ? 0 : loader.hashCode();
        ClassStructure classStructure = classStructureCache.get(classByteArray, hashCode);
        if (classStructure == null) {
            classStructure = new AsmClassStructure(classByteArray, loader);
            classStructureCache.put(classByteArray, hashCode, classStructure);
        }
        return classStructure;
    }

}
