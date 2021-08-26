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
package com.pamirs.attach.plugin.neo4j.operation;


import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Auther: vernon
 * @Date: 2020/9/9 13:29
 * @Description:
 */
public abstract class AbstractOperation implements Operation {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public void check(Object t) {
        boolean ok = Boolean.FALSE;
        if (Pradar.isClusterTest()) {
            if (t instanceof com.pamirs.attach.plugin.neo4j.config.Neo4JSessionExt) {
            } else {
                throw new PressureMeasureError("[error] 压测流量获取到业务会话.");
            }
        } else if (!Pradar.isClusterTest()) {
            if (t instanceof com.pamirs.attach.plugin.neo4j.config.Neo4JSessionExt) {
                logger.error("[error] 非压测流量获取到压测session.");
            }
        }
    }
}
