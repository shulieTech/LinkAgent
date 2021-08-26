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

import com.pamirs.pradar.script.bsh.BshScriptEvaluator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/18 2:57 下午
 */
public class ScriptManager {
    private Map<String, ScriptEvaluator> evaluators;

    public ScriptManager() {
        this.evaluators = new HashMap<String, ScriptEvaluator>();

        BshScriptEvaluator bshScriptEvaluator = new BshScriptEvaluator();
        bshScriptEvaluator.setClassLoader(ScriptManager.class.getClassLoader());
        evaluators.put(bshScriptEvaluator.getType(), bshScriptEvaluator);
    }

    private static ScriptManager INSTANCE;

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
}
