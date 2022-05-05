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
package com.shulie.instrument.module.log.data.pusher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.log.parser.DataType;
import com.pamirs.pradar.remoting.protocol.ProtocolCode;
import com.shulie.instrument.module.log.data.pusher.log.AgentLogFileEnum;
import com.shulie.instrument.module.log.data.pusher.enums.DataPushEnum;
import com.shulie.instrument.module.log.data.pusher.log.PullLogResponse;
import com.shulie.instrument.module.log.data.pusher.log.reader.impl.LogPusherOptions;
import com.shulie.instrument.module.log.data.pusher.push.DataPushManager;
import com.shulie.instrument.module.log.data.pusher.push.impl.DefaultDataPushManager;
import com.shulie.instrument.module.log.data.pusher.server.HttpPushOptions;
import com.shulie.instrument.module.log.data.pusher.server.PusherOptions;
import com.shulie.instrument.module.log.data.pusher.utils.FileReaderUtils;
import com.shulie.instrument.simulator.api.CommandResponse;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.annotation.Command;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import com.shulie.instrument.simulator.api.guard.SimulatorGuard;
import com.shulie.instrument.simulator.api.util.ParameterUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/1 1:36 上午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "log-data-pusher", version = "1.0.0", author = "xiaobin@shulie.io",
    description = "日志推送模式,包含 trace、monitor 日志推送")
public class LogDataPusherModule extends ModuleLifecycleAdapter implements ExtensionModule {
    private final static Logger logger = LoggerFactory.getLogger(LogDataPusherModule.class.getName());
    private static final String APP = "app";
    private static final String AGENT = "agent";

    private DataPushManager dataPushManager;
    private boolean isActive;
    private ScheduledFuture future;

    @Override
    public boolean onActive() throws Throwable {
        isActive = true;
        final PusherOptions pusherOptions = buildPusherOptions();

        future = ExecutorServiceFactory.getFactory().schedule(new Runnable() {
            @Override
            public void run() {
                if (!isActive) {
                    return;
                }
                /**
                 * 启动数据推送管理器
                 */
                try {
                    dataPushManager = SimulatorGuard.getInstance().doGuard(DataPushManager.class,
                        new DefaultDataPushManager(pusherOptions));
                    dataPushManager.start();
                    if (logger.isInfoEnabled()) {
                        logger.info("SIMULATOR: Data push Manager start success.");
                    }
                } catch (Throwable e) {
                    logger.warn("SIMULATOR: Data Push Manager start failed. log data can't push to the server.", e);
                    future = ExecutorServiceFactory.getFactory().schedule(this, 5, TimeUnit.SECONDS);
                }
            }
        }, 30, TimeUnit.SECONDS);
        return true;
    }

    private PusherOptions buildPusherOptions() {
        PusherOptions pusherOptions = new PusherOptions();
        pusherOptions.setDataPusher(
            DataPushEnum.getByType(simulatorConfig.getProperty("pradar.data.pusher", "http"), DataPushEnum.HTTP));
        pusherOptions.setTimeout(simulatorConfig.getIntProperty("pradar.data.pusher.timeout", 3000));
        pusherOptions.setServerZkPath(
            simulatorConfig.getProperty("pradar.server.zk.path", "/config/log/pradar/server"));
        pusherOptions.setZkServers(simulatorConfig.getZkServers());
        pusherOptions.setConnectionTimeoutMillis(simulatorConfig.getZkConnectionTimeout());
        pusherOptions.setSessionTimeoutMillis(simulatorConfig.getZkSessionTimeout());
        pusherOptions.setProtocolCode(
            simulatorConfig.getIntProperty("pradar.push.serialize.protocol.code", ProtocolCode.NONE));
        HttpPushOptions httpPushOptions = new HttpPushOptions();
        httpPushOptions.setMaxHttpPoolSize(simulatorConfig.getIntProperty("pradar.max.httpPool.size", 10));
        httpPushOptions.setHttpPath(simulatorConfig.getProperty("pradar.push.server.http.path", ""));
        httpPushOptions.setEnableGzip(simulatorConfig.getBooleanProperty("pradar.push.server.http.enable.gzip", true));
        pusherOptions.setHttpPushOptions(httpPushOptions);

        List<LogPusherOptions> logPusherOptionsList = new ArrayList<LogPusherOptions>();
        LogPusherOptions traceLogOptions = new LogPusherOptions();
        traceLogOptions.setPath(Pradar.PRADAR_INVOKE_LOG_FILE);
        traceLogOptions.setDataType(DataType.TRACE_LOG);
        traceLogOptions.setVersion(Pradar.PRADAR_TARCE_LOG_VERSION);
        traceLogOptions.setMaxFailureSleepInterval(
            simulatorConfig.getIntProperty("max.push.log.failure.sleep.interval", 10000));
        logPusherOptionsList.add(traceLogOptions);

        LogPusherOptions monitorLogOptions = new LogPusherOptions();
        monitorLogOptions.setPath(Pradar.PRADAR_MONITOR_LOG_FILE);
        monitorLogOptions.setDataType(DataType.MONITOR_LOG);
        monitorLogOptions.setVersion(Pradar.PRADAR_MONITOR_LOG_VERSION);
        monitorLogOptions.setMaxFailureSleepInterval(
            simulatorConfig.getIntProperty("max.push.log.failure.sleep.interval", 10000));
        logPusherOptionsList.add(monitorLogOptions);

        LogPusherOptions agentErrorLogOptions = new LogPusherOptions();
        agentErrorLogOptions.setPath(Pradar.PRADAR_AGENT_ERROR_LOG_FILE);
        agentErrorLogOptions.setDataType(DataType.AGENT_LOG);
        agentErrorLogOptions.setVersion(Pradar.PRADAR_ERROR_LOG_VERSION);
        agentErrorLogOptions.setMaxFailureSleepInterval(
            simulatorConfig.getIntProperty("max.push.log.failure.sleep.interval", 10000));
        logPusherOptionsList.add(agentErrorLogOptions);

        LogPusherOptions simulatorErrorLogOptions = new LogPusherOptions();
        simulatorErrorLogOptions.setPath(Pradar.PRADAR_SIMULATOR_ERROR_LOG_FILE);
        simulatorErrorLogOptions.setDataType(DataType.AGENT_LOG);
        simulatorErrorLogOptions.setVersion(Pradar.PRADAR_ERROR_LOG_VERSION);
        simulatorErrorLogOptions.setMaxFailureSleepInterval(
            simulatorConfig.getIntProperty("max.push.log.failure.sleep.interval", 10000));
        logPusherOptionsList.add(simulatorErrorLogOptions);
        pusherOptions.setLogPusherOptions(logPusherOptionsList);

        return pusherOptions;
    }

    @Override
    public void onFrozen() throws Throwable {
        isActive = false;
        if (future != null && !future.isCancelled() && !future.isDone()) {
            future.cancel(true);
        }
        if (dataPushManager != null) {
            dataPushManager.stop();
        }
    }

    private void readLog(List<PullLogResponse.Log> logs, Integer batchLines, Integer lineStart, String filePath) {
        PullLogResponse.Log log = new PullLogResponse.Log();
        if (null == lineStart) {
            // 直接读取文件最末尾batchLines
            String s = FileReaderUtils.reverseReadLines(filePath, batchLines);
            log.setLogContent(s);
            log.setEndLine(FileReaderUtils.countTotalLines(filePath));
        } else {
            log = FileReaderUtils.readLines(filePath, lineStart, batchLines);
        }
        log.setFilePath(filePath);
        log.setFileName(new File(filePath).getName());
        logs.add(log);
    }

    @Command("pullAppLog")
    public CommandResponse pullAppLog(final Map<String, String> params) {
        try {
            String filePath = params.get("filePath");
            int batchLines = ParameterUtils.getInt(params, "batchLines", 100);
            Integer lineStart = ParameterUtils.getInt(params, "lineStart", null);
            PullLogResponse pullLogResponse = new PullLogResponse();
            pullLogResponse.setAgentId(Pradar.AGENT_ID_NOT_CONTAIN_USER_INFO);
            pullLogResponse.setAppName(AppNameUtils.appName());
            pullLogResponse.setTraceId(params.get("traceId"));
            pullLogResponse.setType(APP);
            List<PullLogResponse.Log> logs = new ArrayList<PullLogResponse.Log>();
            pullLogResponse.setLogs(logs);

            File file = new File(filePath);
            if (!file.exists()) {
                PullLogResponse.Log log = new PullLogResponse.Log();
                log.setHasLogFile(Boolean.FALSE);
                log.setFileName(file.getName());
                log.setFilePath(filePath);
                logs.add(log);
            }
            readLog(logs, batchLines, lineStart, filePath);
            return CommandResponse.success(pullLogResponse);
        } catch (Throwable e) {
            logger.error("SIMULATOR: pullAppLog occured a unknow error! ", e);
            return CommandResponse.failure("pullAppLog occured a unknow error! " + e.getMessage());
        }
    }

    @Command("pullAgentLog")
    public CommandResponse pullAgentLog(final Map<String, String> params) {
        try {
            String fileName = params.get("fileName");
            int batchLines = ParameterUtils.getInt(params, "batchLines", 100);
            Integer lineStart = ParameterUtils.getInt(params, "lineStart", null);
            PullLogResponse pullLogResponse = new PullLogResponse();
            pullLogResponse.setAgentId(Pradar.AGENT_ID_NOT_CONTAIN_USER_INFO);
            pullLogResponse.setAppName(AppNameUtils.appName());
            pullLogResponse.setTraceId(params.get("traceId"));
            pullLogResponse.setType(AGENT);
            List<PullLogResponse.Log> logs = new ArrayList<PullLogResponse.Log>();
            pullLogResponse.setLogs(logs);

            if (null == fileName) {
                // 两个日志文件都查看
                // simulator.log
                readLog(logs, batchLines, lineStart, simulatorConfig.getLogPath() + "/simulator.log");
                // simulator-agent.log
                readLog(logs, batchLines, lineStart, simulatorConfig.getLogPath() + "/simulator-agent.log");
            } else {
                readLog(logs, batchLines, lineStart, simulatorConfig.getLogPath() + "/" + fileName);
            }
            return CommandResponse.success(pullLogResponse);
        } catch (Throwable e) {
            logger.error("SIMULATOR: pullAgentLog occur a unknown error! ", e);
            return CommandResponse.failure("pullAgentLog occur a unknown error! " + e.getMessage());
        }
    }

    @Command("pullLog")
    public CommandResponse pullLog(final Map<String, String> params) {

        String logName = params.get("logName");
        String fileRegx = AgentLogFileEnum.find(logName);
        String rowNumStr = params.get("rowNum");
        if(StringUtils.isEmpty(rowNumStr)){
            return CommandResponse.failure("rowNum can't be null");
        }
        int rowNum;
        try {
            rowNum = Integer.parseInt(rowNumStr);
        } catch (NumberFormatException e) {
            return CommandResponse.failure("rowNum invalid");
        }
        List<String> grepParam = StringUtils.isEmpty(params.get("grepParam")) ? Collections.EMPTY_LIST
                : JSON.parseArray(params.get("grepParam"),String.class);
        try {
            File file = FileReaderUtils.findNewestFile(simulatorConfig.getLogPath(),fileRegx);
            if(file == null){
                return CommandResponse.failure("log file not found");
            }
            String logContent = FileReaderUtils.reverseReadLinesWithGrep(file, rowNum, grepParam);
            PullLogResponse pullLogResponse = new PullLogResponse();
            pullLogResponse.setAgentId(Pradar.AGENT_ID_NOT_CONTAIN_USER_INFO);
            pullLogResponse.setAppName(AppNameUtils.appName());
            PullLogResponse.Log log = new PullLogResponse.Log();
            log.setFileName(file.getName());
            log.setFilePath(file.getAbsolutePath());
            log.setLogContent(logContent);
            pullLogResponse.setLogs(Collections.singletonList(log));
            return CommandResponse.success(pullLogResponse);
        } catch (Exception e) {
            logger.error("SIMULATOR: pull log occur a unknown error! ", e);
            return CommandResponse.failure("pull log occur a unknown error");
        }
    }


}
