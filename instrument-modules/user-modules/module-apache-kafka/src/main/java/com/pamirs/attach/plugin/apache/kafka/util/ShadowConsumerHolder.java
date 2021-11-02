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
package com.pamirs.attach.plugin.apache.kafka.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author angju
 * @date 2021/10/11 20:19
 */
public class ShadowConsumerHolder {
    public final static Map<String, String> topicGroupBeanNameMap = new ConcurrentHashMap<String, String>(8, 1);
    public final static Map<String, Integer> topicGroupCodeMap = new ConcurrentHashMap<String, Integer>(8, 1);
}
