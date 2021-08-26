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
package com.pamirs.attach.plugin.hessian;

import com.pamirs.pradar.MiddlewareType;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/16 5:20 下午
 */
public final class HessianConstants {

    public final static String PLUGIN_NAME = "hessian";
    public final static int PLUGIN_TYPE = MiddlewareType.TYPE_RPC;

    public final static String MODULE_NAME = "hessian";

    public final static String METHOD_HEADER = "Hessian-Invoke-Method";

    public final static String DYNAMIC_FIELD_URL = "_url";
    public final static String DYNAMIC_FIELD_TYPE = "_type";
    public final static String DYNAMIC_FIELD_METHOD = "method";
    public final static String DYNAMIC_FIELD_HOME_SKELETON = "_homeSkeleton";
    public final static String DYNAMIC_FIELD_OBJECT_SKELETON = "_objectSkeleton";
    public final static String DYNAMIC_FIELD_METHOD_MAP = "_methodMap";
    public final static String DYNAMIC_FIELD_SKELETON = "_skeleton";
    public final static String DYNAMIC_FIELD_SERIALIZER_FACTORY = "serializerFactory";
}
