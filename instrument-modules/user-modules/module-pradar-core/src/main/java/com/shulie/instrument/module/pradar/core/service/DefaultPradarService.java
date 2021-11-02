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
package com.shulie.instrument.module.pradar.core.service;

import com.pamirs.pradar.IPradarService;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;

import java.util.Map;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/3/11 6:58 下午
 */
public class DefaultPradarService implements IPradarService {

    @Override
    public boolean isClusterTest() {
        return Pradar.isClusterTest();
    }

    @Override
    public boolean mark(String key, String mark) {
        return Pradar.putUserData(key, mark);
    }

    @Override
    public void unmark(String key) {
        Pradar.removeUserData(key);
    }

    @Override
    public boolean isSaveBusinessTrace() {
        return PradarSwitcher.isSwitchSaveBusinessTrace();
    }

    @Override
    public boolean hasMark(String key) {
        return Pradar.hasUserData(key);
    }

    @Override
    public Map<String, String> getInvokeContext() {
        return Pradar.getInvokeContextMap();
    }

    @Override
    public String getTraceId() {
        return Pradar.getTraceId();
    }

    @Override
    public boolean isSilence() {
        return PradarSwitcher.silenceSwitchOn();
    }

    @Override
    public String printInvokeContext() {
        return Pradar.printContextStack();
    }

    @Override
    public boolean setClusterTest() {
        if (Pradar.getInvokeContext() == null){
            return false;
        }
        Pradar.setClusterTest(true);
        return true;
    }

    @Override
    public boolean isClusterTestEnabled() {
        return PradarSwitcher.isClusterTestEnabled();
    }


}
