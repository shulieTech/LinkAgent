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
package com.pamirs.pradar.script.bsh;

import bsh.EvalError;
import bsh.Interpreter;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.script.ScriptEvaluator;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/18 2:35 下午
 */
public class ThreadLocalCacheBshScriptEvaluator implements ScriptEvaluator {

    private ClassLoader classLoader;

    private ThreadLocal<Map<String, Interpreter>> tt = new ThreadLocal<Map<String, Interpreter>>();

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public String getType() {
        return "bsh";
    }

    @Override
    public Object evaluate(String script) {
        return evaluate(script, null);
    }

    @Override
    public Object evaluate(String script, Map<String, Object> arguments) {
        try {
            Interpreter interpreter = fetchInterpreter(classLoader, script);
            // 非必要不刷新ClassLoader
            ClassLoader externalClassLoader = ReflectionUtils.get(interpreter.getClassManager(), "externalClassLoader");
            if (externalClassLoader != this.classLoader) {
                interpreter.setClassLoader(this.classLoader);
            }
            if (arguments != null) {
                for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                    interpreter.set(entry.getKey(), entry.getValue());
                }
            }
            return interpreter.eval(new StringReader(script));
        } catch (EvalError ex) {
            throw new RuntimeException(script, ex);
        }
    }

    @Override
    public Object evaluate(ClassLoader classLoader, String script, Map<String, Object> arguments) {
        try {
            Interpreter interpreter = fetchInterpreter(classLoader, script);
            if (arguments != null) {
                for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                    interpreter.set(entry.getKey(), entry.getValue());
                }
            }
            return interpreter.eval(new StringReader(script));
        } catch (EvalError ex) {
            throw new RuntimeException(script, ex);
        }
    }

    private Interpreter fetchInterpreter(ClassLoader classLoader, String script) {
        Map<String, Interpreter> map = tt.get();
        if (map == null) {
            map = new HashMap<String, Interpreter>();
            tt.set(map);
        }
        String key = keyOf(classLoader, script);
        Interpreter interpreter = map.get(key);
        if (interpreter == null) {
            interpreter = new Interpreter();
            interpreter.setClassLoader(classLoader);
            map.put(key, interpreter);
        }
        return interpreter;
    }

    private String keyOf(ClassLoader classLoader, String script) {
        return String.valueOf(classLoader) + script;
    }
}
