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

package com.pamirs.attach.plugin.spring.web;

import com.pamirs.pradar.MiddlewareType;

/**
 * @Description 常量类
 * @Author ocean_wll
 * @Date 2022/3/24 11:18 上午
 */
public class SpringWebConstants {

    public final static String MODULE_NAME = "spring-web";

    public static final Integer PLUGIN_TYPE = MiddlewareType.TYPE_WEB_SERVER;

    public static final String WEB_CONTEXT = "p-gateway-context";

    public static final String END_TRACE = "p-spring-web-end" ;

}
