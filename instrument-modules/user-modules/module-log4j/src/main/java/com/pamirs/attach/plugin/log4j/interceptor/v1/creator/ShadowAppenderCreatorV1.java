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
package com.pamirs.attach.plugin.log4j.interceptor.v1.creator;

import org.apache.log4j.Appender;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/05 11:50 上午
 */
public interface ShadowAppenderCreatorV1<T extends Appender> {

    T creatorPtAppender(T appender, String bizShadowLogPath);
}
