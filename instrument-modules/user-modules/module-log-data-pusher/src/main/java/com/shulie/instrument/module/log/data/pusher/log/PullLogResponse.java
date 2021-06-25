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
package com.shulie.instrument.module.log.data.pusher.log;

import java.util.List;

/**
 * 拉取日志响应
 *
 * @author wangjian
 * @since 2021/1/5 20:02
 */
public class PullLogResponse {


    /**
     * trace节点id
     */
    private String traceId;

    /**
     * agent节点id
     */
    private String agentId;

    /**
     * 应用名
     */
    private String appName;

    /**
     * 类型
     */
    private String type;

    /**
     * 日志
     */
    private List<Log> logs;


    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Log> getLogs() {
        return logs;
    }

    public void setLogs(List<Log> logs) {
        this.logs = logs;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public static class Log {

        /**
         * 最后一行行号
         */
        private int endLine;

        /**
         * 日志名称
         */
        private String fileName;

        /**
         * 日志路径
         */
        private String filePath;

        /**
         * 日志内容
         */
        private String logContent;

        /**
         * 文件是否存在
         */
        private Boolean hasLogFile = Boolean.TRUE;

        /**
         * 文件总行数
         */
        private int totalLines;

        public int getEndLine() {
            return endLine;
        }

        public void setEndLine(int endLine) {
            this.endLine = endLine;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getLogContent() {
            return logContent;
        }

        public void setLogContent(String logContent) {
            this.logContent = logContent;
        }

        public Boolean getHasLogFile() {
            return hasLogFile;
        }

        public void setHasLogFile(Boolean hasLogFile) {
            this.hasLogFile = hasLogFile;
        }

        public int getTotalLines() {
            return totalLines;
        }

        public void setTotalLines(int totalLines) {
            this.totalLines = totalLines;
        }
    }
}
