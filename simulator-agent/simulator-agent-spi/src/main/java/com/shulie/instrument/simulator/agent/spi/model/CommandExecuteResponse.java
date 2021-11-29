/*
 * *
 *  * Copyright 2021 Shulie Technology, Co.Ltd
 *  * Email: shulie@shulie.io
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.shulie.instrument.simulator.agent.spi.model;

/**
 * @author angju
 * @date 2021/11/17 14:29
 */
public class CommandExecuteResponse {
    private String taskId;
    private long commandId;
    private boolean success;

    private Object result;

    /**
     * 执行状态 0、完成，1、失败，2、进行中
     */
    private String executeStatus;
    private String msg;

    private String extrasString = null;

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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


    public String getExecuteStatus() {
        return executeStatus;
    }

    public void setExecuteStatus(String executeStatus) {
        this.executeStatus = executeStatus;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getExtrasString() {
        return extrasString;
    }

    public void setExtrasString(String extrasString) {
        this.extrasString = extrasString;
    }
}
