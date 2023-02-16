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

import com.alibaba.fastjson.JSON;
import com.pamirs.pradar.pressurement.entry.CheckerEntry;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author angju
 * @date 2021/8/17 17:35
 */
public class SimulatorDynamicConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatorDynamicConfig.class.getName());

    /**
     * 默认业务流量采样率,默认为1/9999
     */
    public static final int DEFAULT_TRACE_SAMPLING_INTERVAL = 9999;
    /**
     * 默认压测流量采样率，默认为1/1
     */
    public static final int DEFAULT_CLUSTER_TEST_TRACE_SAMPLING_INTERVAL = 1;

    private static final String PRADAR_TRACE_LOG_VERSION_KEY = "pradar.trace.log.version";
    private static final String PRADAR_MONITOR_LOG_VERSION_KEY = "pradar.monitor.log.version";
    private static final String PRADAR_ERROR_LOG_VERSION_KEY = "pradar.error.log.version";
    private static final String IS_KAFKA_MESSAGE_HEADERS_KEY = "is.kafka.message.headers";
    private static final String TRACE_SAMPLING_INTERVAL_KEY = "trace.samplingInterval";
    private static final String CLUSTER_TEST_TRACE_SAMPLING_INTERVAL_KEY = "trace.ct.samplingInterval";
    private static final String SWITCH_SAVE_BUSINESS_TRACE_KEY = "pradar.switch.save.business.trace";
    private static final String BUS_REQUEST_RESPONSE_DATA_ALLOW_TRACE = "pradar.bus.request.response.data.allow.trace";
    private static final String SHADOW_REQUEST_RESPONSE_DATA_ALLOW_TRACE
            = "pradar.shadow.request.response.data.allow.trace";
    private static final String SECURITY_FIELD = "securityField";

    /**
     * 单个应用的静默开关
     */
    private static final String PRADAR_SINGLE_SILENCE_SWITCH_KEY = "pradar.single.silence.switch";

    private static final String FILED_CHECK_RULES_SWITCH = "filed.check.rules.switch";
    private static final String FILED_CHECK_RULES = "filed.check.rules";
    private static final String KAFKA_PT_CONSUMER_MAX_POLL_RATION_KEY = "kafka.pt.consumer.max.poll.ratio";

    /**
     * 影子库，账密前缀及后缀
     */
    private static final String SHADOW_DATASOURCE_ACCOUNT_PREFIX = "shadow.datasource.account.prefix";
    private static final String DEFAULT_SHADOW_DATASOURCE_ACCOUNT_PREFIX = "PT_";

    private static final String SHADOW_DATASOURCE_ACCOUNT_SUFFIX = "shadow.datasource.account.suffix";
    private static final String DEFAULT_SHADOW_DATASOURCE_ACCOUNT_SUFFIX = "";

    /**
     * 性能分析、线程信息采集间隔 pradar.perf.thread.collect.interval
     */
    private static final String PRADAR_PERF_THREAD_COLLECT_INTERVAL = "pradar.perf.thread.collect.interval";
    private static final Integer DEFAULT_PRADAR_PERF_THREAD_COLLECT_INTERVAL = 60;

    /**
     * apache-kafka poll方法报警
     */
    private static final String PRADAR_CLOSE_KAFKA_POLL_REPORT = "pradar.close.kafka.poll.report";

    private final boolean closeKafkaPollReport;
    /**
     * trace 业务流量采样率
     */
    private final int traceSamplingInterval;
    /**
     * trace 压测流量采样率
     */
    private final int clusterTestTraceSamplingInterval;
    /**
     * trace 日志 的版本号
     */
    private final Integer pradarTraceLogVersion;
    /**
     * monitor 日志的版本号
     */
    private final Integer pradarMonitorLogVersion;
    /**
     * error 日志的版本号
     */
    private final Integer pradarErrorLogVersion;
    /**
     * 是否使用 kafka 消息头传递 trace 上下文
     */
    private final Boolean isKafkaMessageHeader;
    /**
     * 是否关闭所有的业务流量执行
     */
    private final boolean isSwitchSaveBusinessTrace;

    /**
     * 业务请求响应是否采集 默认不采集
     */
    private final boolean busRequestResponseDataAllowTrace;

    /**
     * 压测请求响应是否采集 默认不采集
     */
    private final boolean shadowRequestResponseDataAllowTrace;

    /**
     * 单个应用的静默开关是否打开
     */
    private final boolean isSingleSilenceSwitchOn;

    /**
     * kafka影子消费者poll最大比例
     */
    private final double kafkaPtConsumerPollMaxRatio;
    public static final double DEFAULT_PT_KAFKA_CONSUMER_MAX_POLL_RATIO = 0.8;

    /**
     * 影子库账号前后缀
     */
    private final String shadowDatasourceAccountPrefix;
    private final String shadowDatasourceAccountSuffix;

    /**
     * 性能分析线程池定时线程采集频率
     */
    private final Integer perfThreadCollectInterval;

    private Map<String, CheckerEntry> fieldCheckRules;
    private final List<String> securityFieldCollection;

    private Map<String, String> config = null;
    private boolean fieldCheckRulesSwitcher;

    public SimulatorDynamicConfig(Map<String, String> config) {
        this.config = config;
        for (Map.Entry<String, String> entry : config.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
        this.traceSamplingInterval = getTraceSamplingInterval(config);
        this.clusterTestTraceSamplingInterval = getClusterTestTraceSamplingInterval(config);
        this.pradarTraceLogVersion = getPradarTraceLogVersion(config);
        this.pradarMonitorLogVersion = getPradarMonitorLogVersion(config);
        this.pradarErrorLogVersion = getPradarErrorLogVersion(config);
        this.isKafkaMessageHeader = getIsKafkaMessageHeaders(config);
        this.isSwitchSaveBusinessTrace = getSwitchSaveBusinessTrace(config);
        this.busRequestResponseDataAllowTrace = getBusRequestResponseDataAllowTrace(config);
        this.shadowRequestResponseDataAllowTrace = getShadowRequestResponseDataAllowTrace(config);
        this.fieldCheckRulesSwitcher = getFieldCheckRulesSwithcer(config);
        this.fieldCheckRules = getFieldCheckRules(config);
        this.securityFieldCollection = getSecurityFieldCollection(config);
        this.shadowDatasourceAccountPrefix = getShadowDatasourceAccountPrefix(config);
        this.shadowDatasourceAccountSuffix = getShadowDatasourceAccountSuffix(config);
        this.kafkaPtConsumerPollMaxRatio = getKafkaPtConsumerMaxPollRatio(config);
        this.perfThreadCollectInterval = getPerfThreadCollectInterval(config);
        this.closeKafkaPollReport = getCloseKafkaPollReport(config);
        this.isSingleSilenceSwitchOn = getSingleSilenceSwitchStatus(config);
    }

    public String shadowDatasourceAccountPrefix() {
        return this.shadowDatasourceAccountPrefix;
    }

    public Integer perfThreadCollectInterval() {
        return this.perfThreadCollectInterval;
    }

    public String shadowDatasourceAccountSuffix() {
        return this.shadowDatasourceAccountSuffix;
    }

    public boolean closeKafkaPollReport() {
        return this.closeKafkaPollReport;
    }

    /**
     * 获取影子库账密前缀
     *
     * @return string
     */
    private String getShadowDatasourceAccountPrefix(Map<String, String> config) {
        try {
            if (config == null) {
                return DEFAULT_SHADOW_DATASOURCE_ACCOUNT_PREFIX;
            }
            String data = getConfig(config, SHADOW_DATASOURCE_ACCOUNT_PREFIX);
            if (data == null) {
                return DEFAULT_SHADOW_DATASOURCE_ACCOUNT_PREFIX;
            }
            return data;
        } catch (Exception e) {
            LOGGER.error("getShadowDatasourceAccountPrefix error ", e);
            return DEFAULT_SHADOW_DATASOURCE_ACCOUNT_PREFIX;
        }
    }

    /**
     * 获取影子库账密前缀
     *
     * @return string
     */
    private String getShadowDatasourceAccountSuffix(Map<String, String> config) {
        try {
            if (config == null) {
                return DEFAULT_SHADOW_DATASOURCE_ACCOUNT_SUFFIX;
            }
            String data = getConfig(config, SHADOW_DATASOURCE_ACCOUNT_SUFFIX);
            if (data == null) {
                return DEFAULT_SHADOW_DATASOURCE_ACCOUNT_SUFFIX;
            }
            return data;
        } catch (Exception e) {
            LOGGER.error("getShadowDatasourceAccountSuffix error ", e);
            return DEFAULT_SHADOW_DATASOURCE_ACCOUNT_SUFFIX;
        }
    }

    private List<String> getSecurityFieldCollection(Map<String, String> config) {
        if (config == null) {
            return null;
        }
        String date = config.get(SECURITY_FIELD);
        if (date == null) {
            return null;
        }
        String[] splitter = date.split(",");
        return Arrays.asList(splitter);
    }

    public List<String> getSecurityFieldCollection() {
        return securityFieldCollection;
    }

    public boolean isShadowRequestResponseDataAllowTrace() {
        return shadowRequestResponseDataAllowTrace;
    }

    public boolean isBusRequestResponseDataAllowTrace() {
        return busRequestResponseDataAllowTrace;
    }

    public Map<String, CheckerEntry> getFieldCheckRules() {
        return fieldCheckRules;
    }

    public boolean getFieldCheckRulesSwithcer() {
        return this.fieldCheckRulesSwitcher;
    }

    public boolean getFieldCheckRulesSwithcer(Map<String, String> config) {
        boolean result = false;
        if (config == null) {
            return false;
        }
        String date = config.get(FILED_CHECK_RULES_SWITCH);
        if (date == null) {
            return false;
        }
        try {
            result = Boolean.parseBoolean(date);
        } catch (Throwable t) {
            //
        }
        return result;
    }

    String mockStr
            = "[{ \"url\":\"jdbc:mysql://114.55.42.181:3306/atester?useUnicode=true\", \"table\":\"m_user\", "
            + "\"operateType\":\"insert\", \"prefix\":\"PT_\" }]";

    private Map<String, CheckerEntry> getFieldCheckRules(Map<String, String> config) {
        if (!getFieldCheckRulesSwithcer()) {
            return null;
        }
        if (config == null) {
            return null;
        }
        String data = config.get(FILED_CHECK_RULES);
        if (data == null) {
            return null;
        }
        List<CheckerEntry> list = JSON.parseArray(data, CheckerEntry.class);
        return parser(list);
    }

    private Map<String, CheckerEntry> parser(List<CheckerEntry> list) {
        Map<String, CheckerEntry> res = new ConcurrentHashMap<String, CheckerEntry>();
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        for (CheckerEntry entry : list) {

            /**
             * 格式化url,可能Url中有很多附加属性
             */
            String url = entry.getUrl();
            url = url.contains("?") ? url.split("\\?")[0] : url;
            /**
             * 格式化table,可能是db.table
             */
            String table = entry.getTable();
            table = table.contains(".") ? table.split("\\.")[1] : table;
            String operateType = entry.getOperateType();
            String key = url.concat("#").concat(table).concat("#").concat(operateType).toLowerCase();
            res.put(key, entry);
        }
        return res;
    }

    /**
     * trace日志推送版本
     *
     * @return
     */
    public Integer getPradarTraceLogVersion(Map<String, String> config) {
        try {
            if (config == null) {
                return null;
            }
            String data = getConfig(config, PRADAR_TRACE_LOG_VERSION_KEY);
            if (data == null) {
                return null;
            }
            return Integer.valueOf(data.replace(".", ""));
        } catch (Exception e) {
            LOGGER.error("getPradarTraceLogVersion error ", e);
            return null;
        }
    }

    /**
     * trace日志推送版本
     *
     * @param defaultVersion
     * @return
     */
    public int getPradarTraceLogVersion(int defaultVersion) {
        // 兼容不同版本共存, 配置文件中的优先级最高
        //return pradarTraceLogVersion == null ? defaultVersion : pradarTraceLogVersion;
        return defaultVersion;
    }

    /**
     * monitor日志推送版本
     *
     * @return
     */
    public Integer getPradarMonitorLogVersion(Map<String, String> config) {
        try {
            if (config == null) {
                return null;
            }
            String data = getConfig(config, PRADAR_MONITOR_LOG_VERSION_KEY);
            if (data == null) {
                return null;
            }
            return Integer.valueOf(data.replace(".", ""));
        } catch (Exception e) {
            LOGGER.error("getPradarMonitorLogVersion error ", e);
            return null;
        }
    }

    /**
     * monitor日志推送版本
     *
     * @param defaultVersion
     * @return
     */
    public int getPradarMonitorLogVersion(int defaultVersion) {
        // 兼容不同版本共存, 配置文件中的优先级最高
        //return pradarMonitorLogVersion == null ? defaultVersion : pradarMonitorLogVersion;
        return defaultVersion;
    }

    public Integer getPradarErrorLogVersion(Map<String, String> config) {
        try {
            if (config == null) {
                return null;
            }
            String data = getConfig(config, PRADAR_ERROR_LOG_VERSION_KEY);
            if (data == null) {
                return null;
            }
            return Integer.valueOf(data);
        } catch (Exception e) {
            LOGGER.error("getPradarErrorLogVersion error ", e);
            return null;
        }
    }

    public int getPradarErrorLogVersion(int defaultVersion) {
        // 兼容不同版本共存, 配置文件中的优先级最高
        //return pradarErrorLogVersion == null ? defaultVersion : pradarErrorLogVersion;
        return defaultVersion;
    }

    /**
     * kafka是否可以用header
     *
     * @return
     */
    public Boolean getIsKafkaMessageHeaders(Map<String, String> config) {
        try {
            if (config == null) {
                return null;
            }
            String data = getConfig(config, IS_KAFKA_MESSAGE_HEADERS_KEY);
            if (data == null) {
                return null;
            }
            return Boolean.valueOf(data);
        } catch (Exception e) {
            LOGGER.error("getIsKafkaMessageHeaders error ", e);
            return null;
        }
    }

    /**
     * kafka是否可以用header
     *
     * @param defaultValue
     * @return
     */
    public boolean getIsKafkaMessageHeaders(boolean defaultValue) {
        return isKafkaMessageHeader == null ? defaultValue : isKafkaMessageHeader;
    }

    /**
     * 是否为了性能不执行业务流量的trace和隔离逻辑
     *
     * @return
     */
    public boolean getSwitchSaveBusinessTrace(Map<String, String> config) {
        try {
            if (config == null) {
                return false;
            }
            String data = getConfig(config, SWITCH_SAVE_BUSINESS_TRACE_KEY);
            if (data == null) {
                return false;
            }
            return Boolean.parseBoolean(data);
        } catch (Exception e) {
            LOGGER.error("getIsKafkaMessageHeaders error, use default false.", e);
            return false;
        }
    }

    /**
     * 是否为了性能不执行业务流量的trace和隔离逻辑
     *
     * @return
     */
    public boolean isSwitchSaveBusinessTrace() {
        return isSwitchSaveBusinessTrace;
    }

    private int getTraceSamplingInterval(Map<String, String> config) {
        try {
            if (config == null) {
                return DEFAULT_TRACE_SAMPLING_INTERVAL;
            }
            final String value = getConfig(config, TRACE_SAMPLING_INTERVAL_KEY);
            if (value == null) {
                return DEFAULT_TRACE_SAMPLING_INTERVAL;
            }
            return Integer.parseInt(value);
        } catch (Throwable e) {
            LOGGER.error("getTraceSamplingInterval error.", e);
            return DEFAULT_TRACE_SAMPLING_INTERVAL;
        }
    }

    private double getKafkaPtConsumerMaxPollRatio(Map<String, String> config) {
        try {
            if (config == null) {
                return DEFAULT_PT_KAFKA_CONSUMER_MAX_POLL_RATIO;
            }
            final String value = getConfig(config, KAFKA_PT_CONSUMER_MAX_POLL_RATION_KEY);
            if (value == null) {
                return DEFAULT_PT_KAFKA_CONSUMER_MAX_POLL_RATIO;
            }
            return Double.parseDouble(value);
        } catch (Throwable e) {
            LOGGER.error("getKafkaPtConsumerMaxPollRatio error.", e);
            return DEFAULT_PT_KAFKA_CONSUMER_MAX_POLL_RATIO;
        }
    }

    private Integer getPerfThreadCollectInterval(Map<String, String> config) {
        try {
            if (config == null) {
                return DEFAULT_PRADAR_PERF_THREAD_COLLECT_INTERVAL;
            }
            final String value = getConfig(config, PRADAR_PERF_THREAD_COLLECT_INTERVAL);
            if (value == null) {
                return DEFAULT_PRADAR_PERF_THREAD_COLLECT_INTERVAL;
            }
            return Integer.valueOf(value);
        } catch (Throwable e) {
            LOGGER.error("getKafkaPtConsumerMaxPollRatio error .", e);
            return DEFAULT_PRADAR_PERF_THREAD_COLLECT_INTERVAL;
        }
    }

    /**
     * trace日志采样率
     *
     * @return
     */
    public int getClusterTestTraceSamplingInterval(Map<String, String> config) {
        try {
            if (config == null) {
                return DEFAULT_CLUSTER_TEST_TRACE_SAMPLING_INTERVAL;
            }
            final String value = getConfig(config, CLUSTER_TEST_TRACE_SAMPLING_INTERVAL_KEY);
            if (value == null) {
                return DEFAULT_CLUSTER_TEST_TRACE_SAMPLING_INTERVAL;
            }
            return Integer.parseInt(value);
        } catch (Exception e) {
            LOGGER.error("getClusterTestTraceSamplingInterval error .", e);
            return DEFAULT_CLUSTER_TEST_TRACE_SAMPLING_INTERVAL;
        }
    }

    /**
     * trace日志采样率
     *
     * @return
     */
    public int getTraceSamplingInterval() {
        return traceSamplingInterval;
    }

    /**
     * trace日志采样率
     *
     * @return
     */
    public int getClusterTestTraceSamplingInterval() {
        return clusterTestTraceSamplingInterval;
    }

    private String getConfig(Map<String, String> config, String key) {
        String s = config.get(key);
        return s == null ? System.getProperty(key) : s;
    }

    /**
     * 是否允许采集业务的请求体、响应体
     *
     * @return
     */
    private boolean getBusRequestResponseDataAllowTrace(Map<String, String> config) {
        try {
            if (config == null) {
                return false;
            }
            String data = getConfig(config, BUS_REQUEST_RESPONSE_DATA_ALLOW_TRACE);
            if (data == null) {
                return false;
            }
            return Boolean.parseBoolean(data);
        } catch (Exception e) {
            LOGGER.error("getBusRequestResponseDataAllowTrace error , use default false.", e);
            return false;
        }
    }

    /**
     * 是否允许采集压测的请求体、响应体
     *
     * @return
     */
    private boolean getShadowRequestResponseDataAllowTrace(Map<String, String> config) {
        try {
            if (config == null) {
                return false;
            }
            String data = getConfig(config, SHADOW_REQUEST_RESPONSE_DATA_ALLOW_TRACE);
            if (data == null) {
                return false;
            }
            return Boolean.parseBoolean(data);
        } catch (Exception e) {
            LOGGER.error("getShadowRequestResponseDataAllowTrace error , use default false.", e);
            return false;
        }
    }

    /**
     * 是否关闭kafka poll报警
     *
     * @return
     */
    private boolean getCloseKafkaPollReport(Map<String, String> config) {
        try {
            if (config == null) {
                return false;
            }
            String data = getConfig(config, PRADAR_CLOSE_KAFKA_POLL_REPORT);
            if (data == null) {
                return false;
            }
            return Boolean.parseBoolean(data);
        } catch (Exception e) {
            LOGGER.error("getCloseKafkaPollReport error , use default false.", e);
            return false;
        }
    }

    private boolean getSingleSilenceSwitchStatus(Map<String, String> config){
        try {
            if (config == null) {
                return false;
            }
            String data = getConfig(config, PRADAR_SINGLE_SILENCE_SWITCH_KEY);
            if (data == null) {
                return false;
            }
            return Boolean.parseBoolean(data);
        } catch (Exception e) {
            LOGGER.error("getSingleSilenceSwitchStatus error , use default false.", e);
            return false;
        }
    }

    public boolean isSingleSilenceSwitchOn() {
        return isSingleSilenceSwitchOn;
    }

    public double getKafkaPtConsumerPollMaxRatio() {
        return kafkaPtConsumerPollMaxRatio;
    }
}
