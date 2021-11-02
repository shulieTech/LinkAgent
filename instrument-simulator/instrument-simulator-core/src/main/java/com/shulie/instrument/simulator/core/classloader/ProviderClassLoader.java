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

import com.shulie.instrument.simulator.api.annotation.Stealth;
import com.shulie.instrument.simulator.core.util.ReflectUtils;
import org.apache.commons.io.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Vector;
import java.util.jar.JarFile;

/**
 * 服务提供库ClassLoader
 */
@Stealth
public class ProviderClassLoader extends RoutingURLClassLoader {

    private File providerJarFile;

    public ProviderClassLoader(final File providerJarFile,
                               final ClassLoader simulatorClassLoader) throws IOException {
        super(
                new URL[]{new URL("file:" + providerJarFile.getCanonicalPath())},
                new Routing(
                        simulatorClassLoader,
                        "com.shulie.instrument.simulator.api.*",
                        "com.shulie.instrument.simulator.spi.*",
                        "org.apache.commons.lang.*",
                        "org.slf4j.*",
                        "com.shulie.instrument.simulator.dependencies.ch.qos.logback.*",
                        "org.objectweb.asm.*",
                        "javax.annotation.Resource*"
                )
        );
        this.providerJarFile = providerJarFile;
    }

    public void closeIfPossible() {
        try {

            // 如果是JDK7+的版本, URLClassLoader实现了Closeable接口，直接调用即可
            if (this instanceof Closeable) {
                if (isDebugEnabled) {
                    logger.debug("SIMULATOR: JDK is 1.7+, use URLClassLoader[file={}].close()", providerJarFile);
                }
                try {
                    ((Closeable) this).close();
                } catch (Throwable cause) {
                    logger.warn("SIMULATOR: close ProviderClassLoader[file={}] failed. JDK7+", providerJarFile, cause);
                }
                if (routingArray != null) {
                    for (Routing routing : routingArray) {
                        routing.clean();
                    }
                    routingArray = null;
                }
                releaseClasses();
                return;
            }


            // 对于JDK6的版本，URLClassLoader要关闭起来就显得有点麻烦，这里弄了一大段代码来稍微处理下
            // 而且还不能保证一定释放干净了，至少释放JAR文件句柄是没有什么问题了
            try {
                if (isDebugEnabled) {
                    logger.debug("SIMULATOR: JDK is less then 1.7+, use File.release()");
                }
                final Object ucp = ReflectUtils.getDeclaredJavaFieldValueUnCaught(URLClassLoader.class, "ucp", this);
                final Object loaders = ReflectUtils.getDeclaredJavaFieldValueUnCaught(ucp.getClass(), "loaders", ucp);

                for (Object loader :
                        ((Collection) loaders).toArray()) {
                    try {
                        final JarFile jarFile = ReflectUtils.getDeclaredJavaFieldValueUnCaught(
                                loader.getClass(),
                                "jar",
                                loader
                        );
                        jarFile.close();
                    } catch (Throwable t) {
                        // if we got this far, this is probably not a JAR loader so skip it
                    }
                }

                if (routingArray != null) {
                    for (Routing routing : routingArray) {
                        routing.clean();
                    }
                    routingArray = null;
                }

                releaseClasses();
            } catch (Throwable cause) {
                logger.warn("SIMULATOR: close ProviderClassLoader[file={}] failed. probably not a HOTSPOT VM", providerJarFile, cause);
            }

        } finally {

            // 在这里删除掉临时文件
            FileUtils.deleteQuietly(providerJarFile);
        }

    }

    private void releaseClasses() {
        try {
            final Object classes = ReflectUtils.getDeclaredJavaFieldValueUnCaught(ClassLoader.class, "classes", this);
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
}
