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
 * @date 2021/8/13 20:32
 */
public enum ModuleLoadStatusEnum {
    LOAD_SUCCESS("加载成功"), LOAD_FAILED("加载失败"),
    LOAD_DISABLE("禁用"),UNLOAD("未加载");
    private String msg;
    ModuleLoadStatusEnum(String msg){
        this.msg = msg;
    }
}
