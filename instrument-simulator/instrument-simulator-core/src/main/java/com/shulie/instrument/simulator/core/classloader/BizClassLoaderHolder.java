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
package com.shulie.instrument.simulator.core.classloader;

import com.shulie.instrument.simulator.core.util.SimulatorClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

/**
 * 业务类加载器持有者
 * 业务类加载器持有者是一个链式结构，因为每一级的调用都会设置自己的业务 ClassLoader,多层级之间的调用
 * 就会组成一个链式结构，每一层执行完操作后都会清除当前层的 ClassLoader
 *
 * @author xiaobin@shulie.io
 * @since 1.0.0
 */
public class BizClassLoaderHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(BizClassLoaderHolder.class);

    private static final ThreadLocal<ClassLoaderNode> holder = new ThreadLocal<ClassLoaderNode>();

    /**
     * 设置业务类加载器
     * 当前还存在业务类加载器时，则设置子级的业务类加载器，如果当前没有业务类加载器，则
     * 当前的类加载器节点是顶节点
     * 如果classLoader为空，则设置当前线程类加载器，因为不是所有的class.getClassLoader()都是非null
     * 否则会导致同一个类的同一个方法的before 和 and 执行的时候类加载器不一致，导致interceptor不是同一个实例
     * 最终导致opStack不成对，Event强转失败
     *
     * see class.getClassLoader()
     * Returns the class loader for the class.
     * Some implementations may use null to represent the bootstrap class loader.
     * This method will return null in such implementations
     * if this class was loaded by the bootstrap class loader.
     *
     * @param classLoader 业务类加载器
     */
    public static void setBizClassLoader(ClassLoader classLoader) {
        if (null == classLoader) {
            classLoader = Thread.currentThread().getContextClassLoader();
            if (SimulatorClassUtils.isSimulatorClassLoader(classLoader)) {
                LOGGER.warn("setBizClassLoader classLoader is isSimulatorClassLoader!", new Exception());
                return;
            }
        }
        ClassLoaderNode classLoaderNode = holder.get();
        ClassLoaderNode child = new ClassLoaderNode(classLoader, classLoaderNode);
        holder.set(child);
    }

    /**
     * 清除业务类加载器
     * 当前的业务类加载器节点是顶点时，则清空 ThreadLocal
     * 当前业务类加载器是非顶点时，则设置当前类加载器顶点为父级
     */
    public static void clearBizClassLoader() {
        ClassLoaderNode stack = holder.get();
        if (stack == null) {
            return;
        }
        ClassLoaderNode parent = stack.parent;
        if (parent == null) {
            holder.remove();
        } else {
            holder.set(parent);
        }
    }

    /**
     * 获取当前节点的业务类加载器
     * 获取不存业务类加载器则获取当前线程类加载器
     *
     * @return 业务类加载器
     */
    public static ClassLoader getBizClassLoader() {
        ClassLoaderNode stack = holder.get();
        if (stack == null) {
            LOGGER.warn("getBizClassLoader stack is null! this should never happen!", new Exception());
            throw new IllegalStateException("getBizClassLoader stack is null! this should never happen!");
        }
        ClassLoader classLoader = stack.getClassLoader();
        ClassLoader bizClassLoader = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
        if (SimulatorClassUtils.isSimulatorClassLoader(bizClassLoader)) {
            return null;
        }
        return bizClassLoader;
    }

    /**
     * 业务类加载器节点，由这些节点组成链表结构
     * 对应每一个调用层级为一个节点
     */
    static class ClassLoaderNode {
        WeakReference<ClassLoader> classLoader;
        ClassLoaderNode parent;

        ClassLoaderNode(ClassLoader classLoader, ClassLoaderNode parent) {
            if (classLoader != null) {
                this.classLoader = new WeakReference<ClassLoader>(classLoader);
            }
            this.parent = parent;
        }

        ClassLoader getClassLoader() {
            return classLoader == null ? null : classLoader.get();
        }
    }
}
