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
package com.pamirs.attach.plugin.httpclient;

import com.pamirs.pradar.MiddlewareType;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/15 4:22 下午
 */
public final class HttpClientConstants {

    public static final int PLUGIN_TYPE = MiddlewareType.TYPE_RPC;
    public static final String PLUGIN_NAME = "httpclient";

    public static final String HTTP_CLIENT_NAME_3X = "httpclient3";
    public static final String HTTP_CLIENT_NAME_4X = "httpclient4";
    public static final String HTTP_CLIENT_NAME_5X = "httpclient5";

    public static final String DYNAMIC_FIELD_PARAMETERS = "parameters";

    public static Class clazz = null;
}
