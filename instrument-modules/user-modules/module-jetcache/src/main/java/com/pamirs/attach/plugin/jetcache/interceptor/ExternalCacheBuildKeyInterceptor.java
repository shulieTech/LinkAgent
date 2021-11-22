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
package com.pamirs.attach.plugin.jetcache.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ResultInterceptorAdaptor;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/19 7:50 下午
 */
public class ExternalCacheBuildKeyInterceptor extends ResultInterceptorAdaptor {
    @Override
    protected Object getResult0(Advice advice) {
        Object result = advice.getReturnObj();
        if (!Pradar.isClusterTest()) {
            return result;
        }

        byte[] data = (byte[]) result;
        String key = new String(data);

        //白名单 忽略
        if (whiteListValidate(GlobalConfig.getInstance().getCacheKeyWhiteList(), key)) {
            return result;
        }
        if (!Pradar.isClusterTestPrefix(key)) {
            key = Pradar.addClusterTestPrefix(key);
            result = key.getBytes();
        }
        return result;
    }

    private boolean whiteListValidate(Collection<String> whiteList, String key) {
        for (String white : whiteList) {
            if (key.startsWith(white)) {
                return true;
            }
        }
        return false;
    }
}
