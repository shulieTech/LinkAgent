package com.pamirs.attach.plugin.shadow.preparation.constants;

public enum AgentType {

    trace_agent("trace-agent"),

    simulator_agent("simulator-agent");


    String type;

    AgentType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
