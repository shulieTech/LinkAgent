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
package com.pamirs.attach.plugin.dynamic;

import com.pamirs.attach.plugin.dynamic.template.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: vernon
 * @Date: 2021/8/20 01:26
 * @Description:转换器
 */
public interface Converter<K, V> {


    V valueOf(K key);

    K convert(V value);

    /**
     * 模版转换器
     */
    class TemplateConverter implements Converter {

        public static String SPLITTER = "@##";

        public enum TemplateEnum {
            _default("_default", Object.class, SPLITTER, Type.MiddleWareType.OTHER),


            _1("_1", C3p0Template.class, SPLITTER, Type.MiddleWareType.LINK_POOL),
            _2("_2", DruidTemplate.class, SPLITTER, Type.MiddleWareType.LINK_POOL),
            _3("_3", HikariTemplate.class, SPLITTER, Type.MiddleWareType.LINK_POOL),


            _4("_4", HbaseTemplate.class, SPLITTER, Type.MiddleWareType.DB),


            _5("_5", HttpTemplate.class, SPLITTER, Type.MiddleWareType.HTTP_CLIENT),


            _6("_6", RedisTemplate.JedisClusterTemplate.class, SPLITTER, Type.MiddleWareType.CACHE),
            _7("_7", RedisTemplate.JedisSentinelTemplate.class, SPLITTER, Type.MiddleWareType.CACHE),
            _8("_8", RedisTemplate.JedisSingleTemplate.class, SPLITTER, Type.MiddleWareType.CACHE),
            _9("_9", RedisTemplate.RedissionSingleTemplate.class, SPLITTER, Type.MiddleWareType.CACHE),
            _10("_10", RedisTemplate.RedissionClusterTemplate.class, SPLITTER, Type.MiddleWareType.CACHE),
            _11("_11", RedisTemplate.RedissionSentinelTemplate.class, SPLITTER, Type.MiddleWareType.CACHE),
            _12("_12", RedisTemplate.RedissonMasterSlaveTemplate.class, SPLITTER, Type.MiddleWareType.CACHE),
            _13("_13", RedisTemplate.LettuceSingleTemplate.class, SPLITTER, Type.MiddleWareType.CACHE),
            _14("_14", RedisTemplate.LettuceClusterTemplate.class, SPLITTER, Type.MiddleWareType.CACHE),
            _15("_15", RedisTemplate.LettuceSentinelTemplate.class, SPLITTER, Type.MiddleWareType.CACHE),
            _16("_16", DbcpTemplate.class, SPLITTER, Type.MiddleWareType.LINK_POOL),
            _17("_17", RedisTemplate.LettuceMasterSlaveTemplate.class, SPLITTER, Type.MiddleWareType.CACHE),
            _18("_18", RedisTemplate.JedisMasterSlaveTemplate.class, SPLITTER, Type.MiddleWareType.CACHE),
            _19("_19", ProxoolTemplate.class, SPLITTER, Type.MiddleWareType.LINK_POOL);


            String key;
            Class aClass;
            String split;
            Type type;

            public String getKey() {
                return key;
            }

            public Class getaClass() {
                return aClass;
            }

            public String getSplit() {
                return split;
            }

            public Type getType() {
                return type;
            }

            TemplateEnum(String key, Class clazz, String split, Type type) {
                this.key = key;
                aClass = clazz;
                this.split = split;
                this.type = type;
            }
        }

        @Override
        public Object valueOf(Object key) {
            return null;
        }


        @Override
        public Object convert(Object value) {
            return null;
        }


        public static TemplateEnum ofKey(String key) {
            for (TemplateEnum templateEnum : TemplateEnum.values()) {
                if (templateEnum.key.equals(key)) {
                    return templateEnum;
                }
            }
            return TemplateEnum._default;

        }

        public static TemplateEnum ofClass(Class<?> clazz) {
            for (TemplateEnum templateEnum : TemplateEnum.values()) {
                if (clazz == templateEnum.aClass) {
                    return templateEnum;
                }
            }
            return TemplateEnum._default;
        }

        public static List<TemplateEnum> ofType(Type type) {
            List<TemplateEnum> lists = new ArrayList();
            for (TemplateEnum templateEnum : TemplateEnum.values()) {
                if (type == templateEnum.type) {
                    lists.add(templateEnum);
                }
            }
            return lists;
        }
    }
}
