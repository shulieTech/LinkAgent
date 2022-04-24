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
    PRADAR_TRACE("pradar_trace","$(ls -t  %s/pradar_trace.log.[0-9]* |head -n 1)"),
    /**
     * 获取trace推送记录文件
     */
    PRADAR_TRACE_IDX("pradar_trace_idx","$(ls %s/pradar_trace.log.idx)"),
    /**
     * 获取monitor日志的最近一个文件
     */
    PRADAR_MONITOR("pradar_monitor","$(ls -t  %s/pradar_monitor.log.[0-9]* |head -n 1)"),

    /**
     * 获取monitor推送记录文件
     */
    PRADAR_MONITOR_IDX("pradar_monitor_idx","$(ls %s/pradar_monitor.log.idx)"),

    /**
     * 获取simulator日志文件
     */
    SIMULATOR("simulator","$(ls %s/simulator.log)"),
    SIMULATOR_ERROR("simulator_error","$(ls -t  %s/simulator-error.log.[0-9]* |head -n 1)"),

    /**
     * 获取simulator-agent日志文件
     */
    SIMULATOR_AGENT("simulator_agent","$(ls %s/simulator-agent.log)"),
    SIMULATOR_AGENT_ERROR("simulator_agent_error","$(ls -t %s/simulator-agent-error.log.[0-9]* |head -n 1)"),

    ;


    private String code;

    private String fileScript;


    AgentLogFileEnum(String code, String fileScript) {
        this.code = code;
        this.fileScript = fileScript;
    }


    public String getCode() {
        return code;
    }

    public String getFileScript() {
        return fileScript;
    }

    /**
     * 未定义 返回原值
     * @param code
     * @return
     */
    public static String find(String code,String rootPath){
        if(StringUtils.isEmpty(code)){
            return null;
        }
        for (AgentLogFileEnum value : AgentLogFileEnum.values()) {
            if(value.getCode().equals(code)){
                return String.format(value.getFileScript(),rootPath);
            }
        }
        return code;
    }



}
