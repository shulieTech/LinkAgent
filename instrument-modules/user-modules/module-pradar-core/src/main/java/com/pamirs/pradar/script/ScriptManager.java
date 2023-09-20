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
package com.pamirs.pradar.script;

import bsh.EvalError;
import bsh.Interpreter;
import com.pamirs.pradar.script.bsh.GlobalCacheBshScriptEvaluator;
import com.pamirs.pradar.script.bsh.ThreadLocalCacheBshScriptEvaluator;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/18 2:57 下午
 */
public class ScriptManager {
    private Map<String, ScriptEvaluator> evaluators;

    public ScriptManager() {
        this.evaluators = new HashMap<String, ScriptEvaluator>();

        ScriptEvaluator bshScriptEvaluator = "global".equals(System.getProperty("simulator.mock.interpreter.caches"))
                ? new GlobalCacheBshScriptEvaluator() : new ThreadLocalCacheBshScriptEvaluator();
        bshScriptEvaluator.setClassLoader(ScriptManager.class.getClassLoader());
        evaluators.put(bshScriptEvaluator.getType(), bshScriptEvaluator);
    }

    private static ScriptManager INSTANCE;

    private static ClassLoader classLoader = ScriptManager.class.getClassLoader();


    public static ScriptManager getInstance() {
        if (INSTANCE == null) {
            synchronized (ScriptManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ScriptManager();
                }
            }
        }
        return INSTANCE;
    }

    public ScriptEvaluator getScriptEvaluator(String type) {
        return evaluators.get(type);
    }

    public static void main(String[] args) throws EvalError, InterruptedException {

        final String scriptContent = "return Long.valueOf(aaaL) +\" : \" +args[1];";
        final int max = 10000, num = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(8);

        long l = System.nanoTime();
//        final Map<String, Object> binding = new HashMap<String, Object>(4, 1.0f);
//        binding.put("args", new Object[]{"a", String.valueOf(1)});
//        binding.put("target", "aaa");
//        binding.put("classLoader", classLoader);

//        test1(1,scriptContent,binding,"1");
//        test1(1,"import java.util.concurrent.Executors; Executors a=null; return Long.valueOf(aaaL) +\" : \" +args[1]+ a;",binding,"1");
        final CountDownLatch count = new CountDownLatch(num);
        for (int i = 0; i < num; i++) {
            final Map<String, Object> binding = new HashMap<String, Object>(4, 1.0f);
            binding.put("args", new Object[]{"a", String.valueOf(i)});
            binding.put("target", "aaa");
            binding.put("classLoader", classLoader);
            final String i1 = String.valueOf(i);
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        test1(max, scriptContent, binding, i1);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    } finally {
                        count.countDown();
                    }
                }
            });
        }
        count.await();

        System.out.println("avg: " + ((System.nanoTime() - l) / max) / 1000);

//        long l1 = System.nanoTime();
//        test2(max, scriptContent,binding);
//
//
//        System.out.println("avg: " + ((System.nanoTime() - l1) / max) / 1000);
    }

    private static void test1(int max, String scriptContent, Map<String, Object> binding, String value) {
        ScriptEvaluator evaluator = ScriptManager.getInstance().getScriptEvaluator("bsh");
        scriptContent = scriptContent.replace("aaa", value);
        Object result = evaluator.evaluate(classLoader, scriptContent, binding);

        for (int i = 0; i < max; i++) {
            result = evaluator.evaluate(classLoader, scriptContent, binding);
            if (!(value + " : " + value).equals(String.valueOf(result))) {
                System.out.println("value: " + value + ", but " + result);
            }
        }
    }

    private static void test2(int max, String scriptContent, Map<String, Object> arguments) throws EvalError {
        Interpreter interpreter = new Interpreter();
        interpreter.eval(new StringReader(scriptContent));

        for (int i = 0; i < max; i++) {
            interpreter.setClassLoader(classLoader);
            if (arguments != null) {
                for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                    interpreter.set(entry.getKey(), entry.getValue());
                }
            }

            interpreter.eval(new StringReader(scriptContent));
        }

    }
}
