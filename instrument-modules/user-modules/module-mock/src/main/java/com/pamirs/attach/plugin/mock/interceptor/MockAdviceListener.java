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
package com.pamirs.attach.plugin.mock.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.script.ScriptEvaluator;
import com.pamirs.pradar.script.ScriptManager;
import com.shulie.instrument.simulator.api.ProcessController;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.listener.ext.AdviceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MockAdviceListener extends AdviceListener {
    private final static Logger mockLogger = LoggerFactory.getLogger("MOCK-LOGGER");

    private String scriptContent;

    public MockAdviceListener(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    @Override
    public void before(Advice advice) throws Throwable {
        if (Pradar.isClusterTest()) {
            Map<String, Object> binding = new HashMap<String, Object>(4, 1.0f);
            binding.put("args", advice.getParameterArray());
            binding.put("target", advice.getTarget());
            binding.put("classLoader", advice.getClassLoader());
            binding.put("logger", mockLogger);

            ScriptEvaluator evaluator = ScriptManager.getInstance().getScriptEvaluator("bsh");
            Object result = evaluator.evaluate(advice.getClassLoader(), scriptContent, binding);
            ProcessController.returnImmediately(result);
        }

    }

}
