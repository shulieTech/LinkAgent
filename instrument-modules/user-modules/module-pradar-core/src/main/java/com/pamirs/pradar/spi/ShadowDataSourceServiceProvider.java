/*
 * *
 *  * Copyright 2021 Shulie Technology, Co.Ltd
 *  * Email: shulie@shulie.io
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.pamirs.pradar.spi;


import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;

/**
 * @author jiangjibo
 * @date 2021/11/10 11:14 上午
 * @description: 影子库密码提供
 */
public interface ShadowDataSourceServiceProvider {

    String spi_key = "passwordProvider";

    /**
     * 获取影子库密码
     *
     * @param config
     * @return
     */
    boolean processShadowDatabaseConfig(ShadowDatabaseConfig config);

}
