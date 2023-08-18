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
package com.pamirs.pradar.pressurement.agent.shared.service;

import com.google.common.collect.HashBasedTable;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.common.FormatUtils;
import com.pamirs.pradar.debug.DebugHelper;
import com.pamirs.pradar.gson.GsonFactory;
import com.pamirs.pradar.utils.MD5Util;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于错误信息汇报
 *
 * @author xiaobin.zfb | xiaobin@shulie.io
 * @since 2020/7/9 1:30 下午
 */
public class ErrorReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorReporter.class);

    /**
     * key: errorCode; value: md5
     */
    private static HashBasedTable<String, String, Long> reportCache = HashBasedTable.create(8, 1024);

    /**
     * 压测全局开关各插件关闭状态
     */
    private Map<String, Object> pradarSwitchAndAccessErrorMap = new ConcurrentHashMap<String, Object>();

    private static ErrorReporter INSTANCE;

    public static ErrorReporter getInstance() {
        if (INSTANCE == null) {
            synchronized (ErrorReporter.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ErrorReporter();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 该函数自版本4.2.3以及sprint2.1开始废弃（分支feature/log_20200804起）
     * 使用ErrorReporter.buildError()进行异常上报，中断压测
     */
    @Deprecated
    public void addError(String name, Object value) {
        this.pradarSwitchAndAccessErrorMap.put(name, value);
    }

    public Map<String, Object> getErrors() {
        return Collections.unmodifiableMap(this.pradarSwitchAndAccessErrorMap);
    }

    public void clear() {
        this.pradarSwitchAndAccessErrorMap.clear();
    }

    public void clear(Map<String, Object> clear) {
        if (null == clear) {
            return;
        }
        for (String s : clear.keySet()) {
            this.pradarSwitchAndAccessErrorMap.remove(s);
        }
    }

    public static Error buildError() {
        return new Error();
    }

    public static class Error {

        /**
         * 异常类型
         */
        private ErrorTypeEnum errorType;

        /**
         * 异常编码
         */
        private String errorCode;

        /**
         * 异常简要信息
         */
        private String message;

        /**
         * 异常细节
         */
        private String detail;

        /**
         * 异常时间
         */
        private String occurTime;

        private long occurTimestamp;

        /**
         * 是否关闭压测全局开关
         */
        private Boolean isClosePradar = Boolean.FALSE;

        /**
         * 需要关闭的配置项名称
         *
         * @see com.pamirs.pradar.ConfigNames
         */
        private String closeConfigName;

        public ErrorTypeEnum getErrorType() {
            return errorType;
        }

        public Error setErrorType(ErrorTypeEnum errorType) {
            this.errorType = errorType;
            return this;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public Error setErrorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public String getMessage() {
            return message;
        }

        public Error setMessage(String message) {
            this.message = message;
            return this;
        }

        public String getDetail() {
            return detail;
        }

        public Error setDetail(String detail) {
            this.detail = detail;
            return this;
        }

        public Error closePradar(String configName) {
            isClosePradar = Boolean.TRUE;
            this.closeConfigName = configName;
            return this;
        }

        public String getOccurTime() {
            return occurTime;
        }

        @Override
        public String toString() {
            ErrorReportPojo errorReportPojo = new ErrorReportPojo();
            errorReportPojo.setErrorCode(errorCode);
            errorReportPojo.setOccurTime(occurTime);
            errorReportPojo.setDetail(detail);
            errorReportPojo.setMessage(message);
            return GsonFactory.getGson().toJson(errorReportPojo);
        }

        public void report() {
            PradarSwitcher.setErrorCode(this.errorCode);
            PradarSwitcher.setErrorMsg(message);
            this.occurTimestamp = System.currentTimeMillis();

            if (!checkIfEnableReport(this)) {
                return;
            }

            this.occurTime = FormatUtils.formatTimeRange(new Date());
            ErrorReporter.getInstance()
                    .addError(this.errorType.getErrorCnDesc() + ":" + this.toString().hashCode(), this.toString());
            if (this.isClosePradar) {
                LOGGER.error("close cluster switch!error msg:{}", this.toString());
                // 需要关闭全局压测开关
                if (StringUtils.equals(ConfigNames.CLUSTER_TEST_READY_CONFIG, closeConfigName)) {
                    /**
                     * 如果上次已经 ready 了，此次拉取配置存在问题，则忽略
                     */
                } else {
                    PradarSwitcher.turnConfigSwitcherOff(closeConfigName);
                }
            }
            if (Pradar.isDebug()) {
                //打印快速调试配置日志
                printFastDebugLog();
            }
        }

        public void printFastDebugLog() {
            if (!Pradar.isDebug()) {
                return;
            }
            String builder = "traceId:" + Pradar.getTraceId() + "|"
                    + "rpcId:" + Pradar.getInvokeId() + "|"
                    + "isPressure:" + Pradar.isClusterTest() + "|"
                    + "errorType:" + errorType + "|"
                    + "errorCode:" + errorCode + "|"
                    + "message:" + message + "|"
                    + "detail:" + detail + "|";
            DebugHelper.addDebugInfo("ERROR", builder);
        }

    }

    private static boolean checkIfEnableReport(Error error) {
        String errorCode = error.errorCode;
        // 缓存数量超过1000个,清空
        if (reportCache.cellSet().size() > 1000) {
            reportCache.clear();
        }

        String message = error.message;
        String fullMessage = errorCode + "#" + message;
        if (fullMessage.length() > 100) {
            fullMessage = fullMessage.substring(0, 100);
        }

        String md5 = MD5Util.MD5_32(fullMessage, "UTF8");
        Long timestamp = reportCache.get(errorCode, md5);
        if (timestamp == null) {
            reportCache.put(errorCode, md5, error.occurTimestamp);
            return true;
        }
        if (error.occurTimestamp - timestamp > 10 * 1000) {
            reportCache.put(errorCode, md5, error.occurTimestamp);
            return true;
        }
        return false;
    }


    static class ErrorReportPojo {
        private String errorCode;
        private String message;
        private String detail;
        private String occurTime;

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public String getOccurTime() {
            return occurTime;
        }

        public void setOccurTime(String occurTime) {
            this.occurTime = occurTime;
        }
    }
}
