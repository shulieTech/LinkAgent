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

import com.pamirs.attach.plugin.apache.kafka.header.HeaderProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/8/6 5:31 下午
 */
public class DefaultHeaderProcessor implements HeaderProcessor {
    @Override
    public Map<String, String> getHeaders(ConsumerRecord consumerRecord) {
        Headers headers = consumerRecord.headers();
        Header[] headersArr = headers.toArray();
        if (headersArr == null || headersArr.length == 0) {
            return Collections.EMPTY_MAP;
        }
        Map<String, String> ctx = new HashMap<String, String>();
        for (Header header : headersArr) {
            if (!TRACE_HEADERS.contains(header.key())) {
                continue;
            }
            ctx.put(header.key(), new String(header.value()));
        }

        return ctx;
    }

    @Override
    public void setHeader(ProducerRecord producerRecord, String key, String value) {
        Headers headers = producerRecord.headers();
        headers.add(new RecordHeader(key, value.getBytes()));
    }
}
