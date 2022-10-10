package com.shulie.instrument.simulator.core.classloader;

import java.lang.ref.SoftReference;

/**
 * 业务类加载器节点，由这些节点组成链表结构
 * 对应每一个调用层级为一个节点
 *
 * @author Licey
 * @date 2022/9/28
 */
public class ClassLoaderNode {
    SoftReference<ClassLoader> classLoader;
    ClassLoaderNode parent;

    ClassLoaderNode(ClassLoader classLoader, ClassLoaderNode parent) {
        if (classLoader != null) {
            this.classLoader = new SoftReference<ClassLoader>(classLoader);
        }
        this.parent = parent;
    }

    ClassLoader getClassLoader() {
        return classLoader == null ? null : classLoader.get();
    }
}
