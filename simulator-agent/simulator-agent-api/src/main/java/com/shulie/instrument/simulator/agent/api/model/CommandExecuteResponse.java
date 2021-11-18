package com.shulie.instrument.simulator.agent.api.model;

/**
 * @author angju
 * @date 2021/11/17 14:29
 */
public class CommandExecuteResponse {
    private String taskId;
    private long commandId;
    private boolean result;
    private String msg;

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

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
