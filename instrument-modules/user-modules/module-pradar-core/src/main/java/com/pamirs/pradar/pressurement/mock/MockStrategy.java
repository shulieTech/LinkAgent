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
package com.pamirs.pradar.pressurement.mock;

import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.ExecutionCall;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.script.ScriptEvaluator;
import com.pamirs.pradar.script.ScriptManager;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.ProcessController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.pradar.pressurement.mock
 * @Date 2021/6/7 7:09 下午
 */
public class MockStrategy implements ExecutionStrategy {

    private final static Logger LOGGER = LoggerFactory.getLogger(MockStrategy.class.getName());

    @Override
    public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {
        if (Pradar.isClusterTest()) {
            if (params instanceof MatchConfig) {
                try {
                    MatchConfig config = (MatchConfig) params;
                    String scriptContent = config.getScriptContent();
                    ScriptEvaluator evaluator = ScriptManager.getInstance().getScriptEvaluator("bsh");
                    Object result = evaluator.evaluate(scriptContent, config.getArgs());
                    ProcessController.returnImmediately(returnType, result);
                } catch (ProcessControlException e) {
                    throw e;
                } catch (Throwable e) {
                    LOGGER.error("mock处理异常 {}", e);
                    ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.mock)
                            .setErrorCode("mock-0003")
                            .setMessage("mock处理异常！" + e.getMessage())
                            .setDetail("脚本内容" + ((MatchConfig) params).getScriptContent())
                            .report();
                    throw new PressureMeasureError(e);
                }

            }
        }
        return true;
    }

    @Override
    public Object processBlock(Class returnType, ClassLoader classLoader, Object params, ExecutionCall call) throws ProcessControlException {
        if (Pradar.isClusterTest()) {
            if (params instanceof MatchConfig) {
                try {
                    MatchConfig config = (MatchConfig) params;
                    String scriptContent = config.getScriptContent();
                    ScriptEvaluator evaluator = ScriptManager.getInstance().getScriptEvaluator("bsh");
                    Object result = evaluator.evaluate(scriptContent, config.getArgs());
                    Object callResult = call.call(result);
                    ProcessController.returnImmediately(returnType, callResult);
                } catch (ProcessControlException e) {
                    throw e;
                } catch (Throwable e) {
                    LOGGER.error("mock处理异常 {}", e);
                    ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.mock)
                            .setErrorCode("mock-0001")
                            .setMessage("mock处理异常！" + e.getMessage())
                            .setDetail("脚本内容" + ((MatchConfig) params).getScriptContent())
                            .report();
                    throw new PressureMeasureError(e);
                }

            }
        }
        return true;
    }

    @Override
    public Object processNonBlock(Class returnType, ClassLoader classLoader, Object params, ExecutionCall call) throws ProcessControlException {
        if (Pradar.isClusterTest()) {
            if (params instanceof MatchConfig) {
                try {
                    MatchConfig config = (MatchConfig) params;
                    String scriptContent = config.getScriptContent();
                    ScriptEvaluator evaluator = ScriptManager.getInstance().getScriptEvaluator("bsh");
                    Object result = evaluator.evaluate(scriptContent, config.getArgs());
                    return call.call(result);
                } catch (ProcessControlException e) {
                    throw e;
                } catch (Throwable e) {
                    LOGGER.error("mock处理异常 {}", e);
                    ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.mock)
                            .setErrorCode("mock-0001")
                            .setMessage("mock处理异常！" + e.getMessage())
                            .setDetail("脚本内容" + ((MatchConfig) params).getScriptContent())
                            .report();
                    throw new PressureMeasureError(e);
                }

            }
        }
        return true;
    }
}
