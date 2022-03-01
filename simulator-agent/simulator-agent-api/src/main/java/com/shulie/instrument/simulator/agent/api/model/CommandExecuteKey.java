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
package com.shulie.instrument.simulator.agent.api.model;

/**
 * @author angju
 * @date 2021/11/17 16:12
 */
public class CommandExecuteKey {
    private String taskId;
    private Long commandId;

    public CommandExecuteKey(Long commandId, String taskId){
        this.commandId = commandId;
        this.taskId = taskId;
    }

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
        return commandId + ":" + taskId;
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

        if (!(obj instanceof CommandExecuteKey)){
            return false;
        }
        return ((CommandExecuteKey) obj).getCommandId() == commandId && taskId.equals(((CommandExecuteKey) obj).getTaskId());
    }
}
