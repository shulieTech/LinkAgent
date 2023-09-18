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
package com.pamirs.pradar.script;

import java.util.Map;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/18 2:31 下午
 */
public interface ScriptEvaluator {
    /**
     * script type
     *
     * @return
     */
    String getType();

    /**
     *
     * @param classLoader
     */
    void setClassLoader(ClassLoader classLoader);

    /**
     * Evaluate the given script.
     *
     * @param script the ScriptSource for the script to evaluate
     * @return the return value of the script, if any
     */
    Object evaluate(String script);

    /**
     * Evaluate the given script with the given arguments.
     *
     * @param script    the ScriptSource for the script to evaluate
     * @param arguments the key-value pairs to expose to the script,
     *                  typically as script variables (may be {@code null} or empty)
     * @return the return value of the script, if any
     */
    Object evaluate(String script, Map<String, Object> arguments);

    /**
     * Evaluate the given script with the given arguments.
     *
     * @param script    the ScriptSource for the script to evaluate
     * @param arguments the key-value pairs to expose to the script,
     *                  typically as script variables (may be {@code null} or empty)
     * @return the return value of the script, if any
     */
    Object evaluate(ClassLoader classLoader, String script, Map<String, Object> arguments);
}
