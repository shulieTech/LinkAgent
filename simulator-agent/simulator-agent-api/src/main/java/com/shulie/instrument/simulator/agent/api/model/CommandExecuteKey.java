package com.shulie.instrument.simulator.agent.api.model;

/**
 * @author angju
 * @date 2021/11/17 16:12
 */
public class CommandExecuteKey {
    private String taskId;
    private Long commandId;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public long getCommandId() {
        return commandId;
    }

    public void setCommandId(long commandId) {
        this.commandId = commandId;
    }

    @Override
    public String toString(){
        return "commandId:"+commandId + ",taskId"+ taskId;
    }

    @Override
    public int hashCode(){
        return (taskId!=null?taskId.hashCode():1)*31 + (commandId > Integer.MAX_VALUE ? 1 : commandId.intValue());
    }


    @Override
    public boolean equals(Object obj) {
        if ((this == obj)){
            return true;
        }

        return (this == obj);
    }
}
