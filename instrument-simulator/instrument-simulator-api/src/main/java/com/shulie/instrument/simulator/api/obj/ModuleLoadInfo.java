/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.simulator.api.obj;

/**
 * @author angju
 * @date 2021/8/13 20:31
 */
public class ModuleLoadInfo {
    private String moduleId;
    private ModuleLoadStatusEnum status;
    private String errorMsg;

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public ModuleLoadStatusEnum getStatus() {
        return status;
    }

    public void setStatus(ModuleLoadStatusEnum status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }



    public void setErrorMsg(ModuleLoadStatusEnum status, String errorMsg){
        this.status = status;
        this.errorMsg = errorMsg;
    }
}
