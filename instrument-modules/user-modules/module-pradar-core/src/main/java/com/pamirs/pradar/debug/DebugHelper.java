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
package com.pamirs.pradar.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.shulie.instrument.simulator.api.ModuleRuntimeException;
import com.shulie.instrument.simulator.api.ModuleRuntimeException.ErrorCode;
import com.shulie.instrument.simulator.api.resource.ModuleCommandInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/07/29 7:44 下午
 */
public class DebugHelper {

    private static ModuleCommandInvoker moduleCommandInvoker;
    private final static String MODULE_ID_DEBUG = "DEBUG-MODULE";
    private final static String ADD_DEBUG_COMMAND = "addDebugInfo";
    private final static String MACHINE_DEBUG_COMMAND = "addMachineDebugInfo";
    private static boolean hasDebugModule = true;
    private static AtomicBoolean warnAlready = new AtomicBoolean(false);
    private final static Logger LOGGER = LoggerFactory.getLogger(Pradar.class);

    private DebugHelper() {}

    public static void registerModuleCommandInvoker(ModuleCommandInvoker moduleCommandInvoker) {
        DebugHelper.moduleCommandInvoker = moduleCommandInvoker;
    }

    public static void addDebugInfo(String level, String content) {
        if (hasDebugModule && Pradar.isDebug()) {
            Map<String, String> args = buildArgs();
            args.put("level", level);
            args.put("content", content);
            try {
                moduleCommandInvoker.invokeCommand(MODULE_ID_DEBUG, ADD_DEBUG_COMMAND, args);
            } catch (ModuleRuntimeException e) {
                if (ErrorCode.MODULE_NOT_EXISTED.equals(e.getErrorCode())) {
                    hasDebugModule = false;
                    if (warnAlready.compareAndSet(false, true)) {
                        LOGGER.warn("debug module is not existed! debug will not take effect");
                    }
                }
            }

        }
    }

    public static void addMachineDebugInfo(String name) {
        if (hasDebugModule && Pradar.isDebug()) {
            Map<String, String> args = buildArgs();
            args.put("methodName", name);
            try {
                moduleCommandInvoker.invokeCommand(MODULE_ID_DEBUG, MACHINE_DEBUG_COMMAND, args);
            } catch (ModuleRuntimeException e) {
                if (ErrorCode.MODULE_NOT_EXISTED.equals(e.getErrorCode())) {
                    hasDebugModule = false;
                    if (warnAlready.compareAndSet(false, true)) {
                        LOGGER.warn("debug module is not existed! debug will not take effect");
                    }
                }
            }
        }
    }

    private static Map<String, String> buildArgs() {
        Map<String, String> args = new HashMap<String, String>();
        args.put("traceId", Pradar.getTraceId());
        args.put("rpcId", Pradar.getInvokeId());
        args.put("logType", Pradar.getLogType() + "");
        args.put("agentId", Pradar.getAgentId());
        args.put("appName", AppNameUtils.appName());
        return args;
    }
}
