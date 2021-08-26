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
package com.pamirs.attach.plugin.apache.kafka.header;

import com.pamirs.pradar.Pradar;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;
import java.util.Map;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/8/6 5:30 下午
 */
public interface HeaderProcessor {
    List<String> TRACE_HEADERS = Pradar.getInvokeContextTransformKeys();

    /**
     * get headers from consumer record
     *
     * @param consumerRecord
     * @return
     */
    Map<String, String> getHeaders(ConsumerRecord consumerRecord);

    /**
     * set header to producer record
     *
     * @param producerRecord
     * @param key
     * @param value
     */
    void setHeader(ProducerRecord producerRecord, String key, String value);
}
