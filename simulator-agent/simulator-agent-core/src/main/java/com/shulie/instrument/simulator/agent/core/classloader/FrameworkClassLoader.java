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
package com.shulie.instrument.simulator.agent.core.classloader;

import com.shulie.instrument.simulator.agent.core.util.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Collection;
import java.util.jar.JarFile;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/5/20 8:50 下午
 */
public class FrameworkClassLoader extends URLClassLoader {
    private final Logger logger = LoggerFactory.getLogger(FrameworkClassLoader.class.getName());

    public FrameworkClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public FrameworkClassLoader(URL[] urls) {
        super(urls);
    }

    public FrameworkClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    /**
     * destroy all resources
     */
    public void closeIfPossible() {
        // 如果是JDK7+的版本, URLClassLoader实现了Closeable接口，直接调用即可
        if (this instanceof Closeable) {
            if (logger.isDebugEnabled()) {
                logger.debug("SIMULATOR: JDK is 1.7+, use URLClassLoader.close()");
            }
            try {
                ((Closeable) this).close();
            } catch (Throwable cause) {
                logger.warn("SIMULATOR: close ModuleJarClassLoader failed. JDK7+", cause);
            }
            return;
        }


        // 对于JDK6的版本，URLClassLoader要关闭起来就显得有点麻烦，这里弄了一大段代码来稍微处理下
        // 而且还不能保证一定释放干净了，至少释放JAR文件句柄是没有什么问题了
        try {
            if (logger.isDebugEnabled()) {
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

        } catch (Throwable cause) {
            logger.warn("SIMULATOR: close ModuleJarClassLoader failed. probably not a HOTSPOT VM", cause);
        }


    }
}
