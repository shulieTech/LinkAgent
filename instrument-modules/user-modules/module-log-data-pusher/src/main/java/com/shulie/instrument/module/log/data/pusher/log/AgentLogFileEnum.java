package com.shulie.instrument.module.log.data.pusher.log;

import org.apache.commons.lang.StringUtils;

/**
 * @author guann1n9
 * @date 2022/4/13 10:42 AM
 */
public enum AgentLogFileEnum {

    /**
     * 获取trace日志的最近一个文件
     */
    PRADAR_TRACE("pradar_trace","^pradar_trace.log.\\d+$"),
    /**
     * 获取trace推送记录文件
     */
    PRADAR_TRACE_IDX("pradar_trace_idx","^pradar_trace.log.idx"),
    /**
     * 获取monitor日志的最近一个文件
     */
    PRADAR_MONITOR("pradar_monitor","^pradar_monitor.log.\\d+$"),

    /**
     * 获取monitor推送记录文件
     */
    PRADAR_MONITOR_IDX("pradar_monitor_idx","^pradar_monitor.log.idx"),

    /**
     * 获取simulator日志文件
     */
    SIMULATOR("simulator","^simulator.log"),
    SIMULATOR_ERROR("simulator_error","^simulator-error.log.\\d+$"),

    /**
     * 获取simulator-agent日志文件
     */
    SIMULATOR_AGENT("simulator_agent","^simulator-agent.log"),
    SIMULATOR_AGENT_ERROR("simulator_agent_error","^simulator-agent-error.log.\\d+$"),

    ;


    private String code;

    private String regx;


    AgentLogFileEnum(String code, String regx) {
        this.code = code;
        this.regx = regx;
    }


    public String getCode() {
        return code;
    }

    public String getRegx() {
        return regx;
    }

    /**
     * 未定义 返回原值
     * @param code
     * @return
     */
    public static String find(String code){
        if(StringUtils.isEmpty(code)){
            return null;
        }
        for (AgentLogFileEnum value : AgentLogFileEnum.values()) {
            if(value.getCode().equals(code)){
                return value.getRegx();
            }
        }
        return code;
    }



}
