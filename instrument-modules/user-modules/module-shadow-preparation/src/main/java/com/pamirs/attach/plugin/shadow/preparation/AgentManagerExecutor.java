package com.pamirs.attach.plugin.shadow.preparation;

import io.shulie.agent.management.client.AgentManagementClient;

public class AgentManagerExecutor {

    private static AgentManagementClient client;

    public static void setAgentManagementClient(AgentManagementClient client){
        AgentManagerExecutor.client = client;
    }

}
