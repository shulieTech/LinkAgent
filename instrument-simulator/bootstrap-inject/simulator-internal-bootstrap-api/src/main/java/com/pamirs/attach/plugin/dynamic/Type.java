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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Auther: vernon
 * @Date: 2021/8/20 13:38
 * @Description:
 */
public interface Type {

    enum MiddleWareType implements Type {
        HTTP_CLIENT("http-client") {
            @Override
            public List<String> child() {
                return Arrays.asList("httpclient3",
                        "httpclient4",
                        "jdk-http",
                        "async-httpclient",
                        "google-httpclient");
            }
        },
        RPC("RPC框架") {
            @Override
            public List<String> child() {
                return Arrays.asList("apache-dubbo",
                        "grpc",
                        "hessian");
            }
        },/*不包括http*/
        DB("存储") {
            @Override
            public List<String> child() {
                return Arrays.asList(DataBaseType.types());
            }
        },
        LINK_POOL("连接池") {
            @Override
            public List<String> child() {
                return Arrays.asList("druid"
                        , "hikari", "c3p0", "dbcp", "dbcp2");
            }
        },
        CACHE("缓存") {
            @Override
            public List<String> child() {
                return Arrays.asList("redis-jedis," +
                                "redis-lettuce",
                        "redis-redisson");
            }
        },
        MQ("消息队列") {
            @Override
            public List<String> child() {
                return Arrays.asList("rocketmq",
                        "kafka",
                        "rabbitmq");
            }
        },
        JOB("定时任务") {
            @Override
            public List<String> child() {
                return null;
            }
        },
        CONTAINER("容器") {
            @Override
            public List<String> child() {
                return Arrays.asList("docker");
            }
        },
        OTHER("其他") {
            @Override
            public List<String> child() {
                return null;
            }
        },
        GATEWAY("网关") {
            @Override
            public List<String> child() {
                return null;
            }
        };

        String value;

        public MiddleWareType of(String key) {
            for (MiddleWareType type : MiddleWareType.values()) {
                if (type.value.equals(key)) {
                    return type;
                }
            }
            return null;
        }

        public abstract List<String> child();

        MiddleWareType(String value) {
            this.value = value;
        }


        public static Type ofKey(String key) {
            for (MiddleWareType type : MiddleWareType.values()) {
                if (key.equals(type.value)) {
                    return type;
                }
            }
            return null;
        }

        public String value() {
            return value;
        }
    }

    enum DataBaseType implements Type {
        MYSQL,
        OCEANBASE,
        ORACLE,
        SQLSERVER,
        DB2,
        POSTGRESQL,
        POLARDB,
        HIVE,
        NEO4J,
        MONGODB;


        protected static String[] types = null;

        static {
            init();
        }


        public static String[] types() {
            return types;
        }

        static void init() {
            List<String> list = new ArrayList<String>();
            DataBaseType[] dbTypes = DataBaseType.values();
            for (DataBaseType dbType : dbTypes) {
                list.add(dbType.name());
            }
            types = list.toArray(new String[list.size()]);
        }

    }
}
