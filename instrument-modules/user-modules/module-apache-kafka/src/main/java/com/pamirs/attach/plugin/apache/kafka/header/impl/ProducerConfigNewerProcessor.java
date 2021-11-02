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
package com.pamirs.attach.plugin.apache.kafka.header.impl;

import com.pamirs.attach.plugin.apache.kafka.header.ProducerConfigProcessor;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.List;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/9/24 6:27 下午
 */
public class ProducerConfigNewerProcessor implements ProducerConfigProcessor {
    @Override
    public String getValue(ProducerConfig producerConfig, String key) {
        List<String> value = producerConfig.getList(key);
        return getListString(value);
    }

    private String getListString(List<String> value) {
        if (value == null) {
            return "";
        }
        if (value.size() == 1) {
            return value.get(0);
        } else {
            StringBuilder builder = new StringBuilder();
            for (String str : value) {
                builder.append(str).append(',');
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        }
    }
}
