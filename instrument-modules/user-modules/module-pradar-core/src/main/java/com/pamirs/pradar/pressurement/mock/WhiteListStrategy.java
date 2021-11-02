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

import com.pamirs.pradar.*;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.ExecutionCall;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;

import java.util.Map;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.pradar.pressurement.mock
 * @Date 2021/6/7 7:09 下午
 */
public class WhiteListStrategy implements ExecutionStrategy {

    private static final String TRUE = "true";

    @Override
    public Object processBlock(Class returnType, ClassLoader classLoader, Object params) {
        if (!Pradar.isClusterTest()) {
            return true;
        }
        if (!PradarSwitcher.whiteListSwitchOn()) {
            return true;
        }
        if (params instanceof MatchConfig) {
            MatchConfig config = (MatchConfig) params;
            Map<String, Object> args = config.getArgs();
            // 直接通过通过白名单校验
            if (TRUE.equals(args.get(PradarService.PRADAR_WHITE_LIST_CHECK))) {
                return true;
            }
            InvokeContext invokeContext = Pradar.getInvokeContext();

            if (invokeContext.getParentInvokeContext() != null &&
                    (invokeContext.getParentInvokeContext().getInvokeType() == MiddlewareType.TYPE_RPC ||
                            invokeContext.getParentInvokeContext().getInvokeType() == MiddlewareType.TYPE_WEB_SERVER) &&
                    invokeContext.getParentInvokeContext().isPassCheck()) {
                return true;
            }
            if (!config.isSuccess()) {
                Boolean isInterface = isInterface(config);
                if (isInterface) {
                    String className = (String) args.get("class");
                    String methodName = (String) args.get("method");
                    String message = "WhiteListError: [" + AppNameUtils.appName() + "] interface [" + className + "#" + methodName + "] is not allowed in WhiteList.";
                    if (Pradar.isClusterTest()) {
                        ErrorReporter.buildError()
                                .setErrorType(ErrorTypeEnum.AgentError)
                                .setErrorCode("whiteList-0001")
                                .setMessage(message)
                                .setDetail(message)
                                .closePradar(ConfigNames.RPC_WHITE_LIST)
                                .report();
                        throw new PressureMeasureError(message);
                    }
                } else {
                    String url = (String) args.get("url");
                    String message = "WhiteListError: [" + AppNameUtils.appName() + "] url [" + url + "] is not allowed in WhiteList.";
                    ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.AgentError)
                            .setErrorCode("whiteList-0001")
                            .setMessage(message)
                            .setDetail(message)
                            .closePradar(ConfigNames.URL_WHITE_LIST)
                            .report();
                    throw new PressureMeasureError(message);
                }


            }
        }
        return true;
    }

    private Boolean isInterface(MatchConfig config) {
        try {
            Object isInterface = config.getArgs().get("isInterface");
            if (isInterface == null) {
                return Boolean.FALSE;
            }
            if (isInterface instanceof Boolean) {
                return (Boolean) isInterface;
            }
            return Boolean.FALSE;
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

    @Override
    public Object processBlock(Class returnType, ClassLoader classLoader, Object params, ExecutionCall call) {
        return processBlock(returnType, classLoader, params);
    }

    @Override
    public Object processNonBlock(Class returnType, ClassLoader classLoader, Object params, ExecutionCall call) {
        return processBlock(returnType, classLoader, params);
    }
}
