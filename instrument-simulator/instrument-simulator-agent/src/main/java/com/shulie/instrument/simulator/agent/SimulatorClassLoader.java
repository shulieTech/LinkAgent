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
package com.shulie.instrument.simulator.agent;


import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarFile;

/**
 * 加载Simulator用的ClassLoader
 */
class SimulatorClassLoader extends URLClassLoader {
    private String toString;
    private String path;

    SimulatorClassLoader(final String simulatorCoreJarFilePath) throws MalformedURLException {
        /**
         * 指定父类加载器为 null，防止类冲突问题
         */
        super(new URL[]{new URL("file:" + simulatorCoreJarFilePath)}, null);
        this.path = simulatorCoreJarFilePath;
        this.toString = String.format("SimulatorClassLoader[path=%s;]", path);
    }

    @Override
    public URL getResource(String name) {
        URL url = super.getResource(name);
        if (null != url) {
            return url;
        }
        return getBizClassLoader().getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> urls = super.getResources(name);
        if (null != urls) {
            return urls;
        }
        return getBizClassLoader().getResources(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            Class<?> aClass = super.loadClass(name, resolve);
            return aClass;
        } catch (Throwable t) {
            try {
                return getBizClassLoader().loadClass(name);
            } catch (Throwable ex) {
                if (t instanceof ClassNotFoundException) {
                    throw (ClassNotFoundException) t;
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
                throw new ClassNotFoundException("class " + name + " not found.", t);
            }
        }
    }

    private ClassLoader getBizClassLoader() {
        ClassLoader classLoader = SimulatorClassLoader.class.getClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader instanceof SimulatorClassLoader) {
                classLoader = null;
            }
        }
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return classLoader;
    }

    @Override
    public String toString() {
        return toString;
    }


    /**
     * 尽可能关闭ClassLoader
     * <p>
     * URLClassLoader会打开指定的URL资源，在Simulator中则是对应的Jar文件，如果不在shutdown的时候关闭ClassLoader，会导致下次再次加载
     * 的时候，依然会访问到上次所打开的文件（底层被缓存起来了）
     * <p>
     * 在JDK1.7版本中，URLClassLoader提供了{@code close()}方法来完成这件事；但在JDK1.6版本就要下点手段了；
     * <p>
     * 该方法将会被{@code ControlModule#shutdown}通过反射调用，
     * 请保持方法声明一致
     */
    @SuppressWarnings("unused")
    public void closeIfPossible() {

        // 如果是JDK7+的版本, URLClassLoader实现了Closeable接口，直接调用即可
        if (this instanceof Closeable) {
            try {
                ((Closeable) this).close();
            } catch (Throwable cause) {
                // ignore...
            }
            releaseClasses();
            return;
        }


        // 对于JDK6的版本，URLClassLoader要关闭起来就显得有点麻烦，这里弄了一大段代码来稍微处理下
        // 而且还不能保证一定释放干净了，至少释放JAR文件句柄是没有什么问题了
        try {
            final Object ucp = forceGetDeclaredFieldValue(URLClassLoader.class, "ucp", this);
            final Object loaders = forceGetDeclaredFieldValue(ucp.getClass(), "loaders", ucp);

            for (final Object loader :
                    ((Collection) loaders).toArray()) {
                try {
                    final JarFile jarFile = forceGetDeclaredFieldValue(loader.getClass(), "jar", loader);
                    jarFile.close();
                } catch (Throwable t) {
                    // if we got this far, this is probably not a JAR loader so skip it
                }
            }
            releaseClasses();
        } catch (Throwable cause) {
            // ignore...
        }
        path = null;
        toString = null;
    }

    private void releaseClasses() {
        try {
            final Object classes = forceGetDeclaredFieldValue(ClassLoader.class, "classes", this);
            if (classes == null) {
                return;
            }
            if (!(classes instanceof Vector)) {
                return;
            }
            ((Vector) classes).clear();
        } catch (Throwable e) {
        }
    }

    private <T> T forceGetDeclaredFieldValue(Class<?> clazz, String name, Object target) throws NoSuchFieldException, IllegalAccessException {
        final Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

}