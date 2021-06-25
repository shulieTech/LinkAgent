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
package com.pamirs.attach.plugin.mock.utils;

import com.pamirs.pradar.exception.PressureMeasureError;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/12 11:33 上午
 */
public class GroovyUtils {
    private final static Logger LOGGER = LoggerFactory.getLogger(GroovyUtils.class);
    private static GroovyShell groovyShell;

    static {
        groovyShell = new GroovyShell(GroovyUtils.class.getClassLoader());
    }

    /**
     * 获取脚本的class
     *
     * @param script
     * @return
     */
    public static Class<?> compile(String script) {
        String scriptMd5 = MD5Util.MD5_32(script, "UTF-8");
        String className = "com.pamirs.attach.plugin.mock.script.dynamic.Script" + scriptMd5;
        return compile(script, className);
    }

    /**
     * 获取脚本的class对象
     *
     * @param script    groovy 脚本
     * @param className 类名字
     */
    public static Class<?> compile(String script, String className) {
        GroovyClassLoader loader = getDefaultParentGroovyClassLoader();
        Class<?> groovyClass = loader.parseClass(script, className);
        return groovyClass;
    }

    /**
     * 默认的父加载器为GroovyCompiler().getClass().getClassLoader()
     *
     * @return 返回groovy的类加载器
     */
    public static GroovyClassLoader getDefaultParentGroovyClassLoader() {
        ClassLoader cl = GroovyUtils.class.getClassLoader();
        return new GroovyClassLoader(cl);
    }

    public static Object execute(final String ruleScript, Binding args) {
        String scriptMd5 = MD5Util.MD5_32(ruleScript, "UTF-8");
        Script script;
        if (scriptMd5 == null) {
            script = groovyShell.parse(ruleScript);
        } else {
            final String finalScriptMd5 = scriptMd5;
            final String scriptName = generateScriptName(finalScriptMd5);
            script = GroovyCache.getValue(GroovyCache.GROOVY_SHELL_KEY_PREFIX + scriptMd5,
                    new Callable<Object>() {
                        @Override
                        public Object call() {
                            return groovyShell.parse(ruleScript, scriptName);
                        }
                    });
        }
        if (script == null) {
            throw new PressureMeasureError("can't parse script. script=" + ruleScript);
        }

        // 此处锁住script，为了防止多线程并发执行Binding数据混乱
        synchronized (script) {
            script.setBinding(args);
            return script.run();
        }
    }

    public static Object execute(final String ruleScript, Map<String, Object> args) {
        return execute(ruleScript, new Binding(args));
    }

    private static String generateScriptName(String scriptName) {
        return "Script" + scriptName + ".groovy";
    }

    /**
     * 清空所有缓存
     */
    public static void clearCache() {
        if (groovyShell == null) {
            return;
        }
        groovyShell.resetLoadedClasses();
        groovyShell = null;
    }
}
