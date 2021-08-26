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
package com.pamirs.attach.plugin.apache.kafka.stream.common;

/**
 * @author angju
 * @date 2021/5/8 15:56
 */
public enum  KStreamProcessorProcessTypeEnum {
    MAP("mapper"),FOREACH("action");
    private String actionFieldName;
    KStreamProcessorProcessTypeEnum(String actionFieldName){
        this.actionFieldName = actionFieldName;
    }

    public String getActionFieldName() {
        return actionFieldName;
    }
}
