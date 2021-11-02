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

/**
 * @Auther: vernon
 * @Date: 2021/8/17 21:44
 * @Description:
 */
public enum MiddleWareType {


    DRUID(TemplateType.DATASOURCE, "druid"),
    HIKARI(TemplateType.DATASOURCE, "hikari"),
    C3P0(TemplateType.DATASOURCE, "c3p0"),


    KAFKA(TemplateType.MQ, "kafka"),
    ROCKETMQ(TemplateType.MQ, "rocketmq"),
    PULSAR(TemplateType.MQ, "pulsar"),
    RABBITMQ(TemplateType.MQ, "rabbitmq"),


    HTTPCLIENT3(TemplateType.RPC, "httpclient3"),

    HTTPCLIENT4(TemplateType.RPC, "httpclient4"),
    JDKHTTP(TemplateType.RPC, "jdkhttp");





    TemplateType templateType;
    Object mdiddlewareType;

    MiddleWareType(TemplateType templateType, Object secondaryLevel, String... indexes) {
        this.templateType = templateType;
        this.mdiddlewareType = mdiddlewareType;
    }


    enum TemplateType {

        DATASOURCE,
        MQ,
        RPC;

        TemplateType() {
        }

        String name;


    }

}
