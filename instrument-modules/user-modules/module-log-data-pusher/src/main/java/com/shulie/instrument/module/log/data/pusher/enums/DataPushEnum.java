/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shulie.instrument.module.log.data.pusher.enums;

import java.util.HashMap;
import java.util.Map;

import com.shulie.instrument.simulator.api.util.StringUtil;

/**
 * @Description 数据推送枚举类
 * @Author ocean_wll
 * @Date 2022/3/7 4:22 下午
 */
public enum DataPushEnum {
    TCP("tcp"),
    HTTP("http"),
    ;

    private String type;

    private final static Map<String, DataPushEnum> dataMap = new HashMap<String, DataPushEnum>();

    DataPushEnum(String type) {
        this.type = type;
    }

    static {
        for (DataPushEnum dataPushEnum : DataPushEnum.values()) {
            dataMap.put(dataPushEnum.getType(), dataPushEnum);
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static DataPushEnum getByType(String type, DataPushEnum defaultValue) {
        if (StringUtil.isEmpty(type)) {
            return defaultValue;
        }

        return dataMap.get(type.toLowerCase()) == null ? defaultValue : dataMap.get(type.toLowerCase());
    }
}
