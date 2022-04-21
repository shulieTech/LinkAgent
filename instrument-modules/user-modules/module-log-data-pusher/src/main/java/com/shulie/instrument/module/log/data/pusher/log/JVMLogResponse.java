package com.shulie.instrument.module.log.data.pusher.log;

/**
 * @author guann1n9
 * @date 2022/4/14 2:21 PM
 */
public class JVMLogResponse {

    /**
     * agent节点id
     */
    private String agentId;

    /**
     * 应用名
     */
    private String appName;


    private String info;


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

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public JVMLogResponse() {
    }
}
