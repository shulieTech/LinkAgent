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
package com.pamirs.attach.plugin.tomcat.dbcp.destroy;

import com.pamirs.attach.plugin.tomcat.dbcp.ListenerRegisterStatus;
import com.pamirs.attach.plugin.tomcat.dbcp.util.DataSourceWrapUtil;
import com.shulie.instrument.simulator.api.listener.Destroyed;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/16 2:13 下午
 */
public class Dbcp2Destroy implements Destroyed {
    @Override
    public void destroy() {
        DataSourceWrapUtil.destroy();
        ListenerRegisterStatus.release();
    }
}
