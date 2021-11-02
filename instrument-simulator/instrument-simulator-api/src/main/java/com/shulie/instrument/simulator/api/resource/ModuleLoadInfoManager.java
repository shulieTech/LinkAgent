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
package com.shulie.instrument.simulator.api.resource;


import com.shulie.instrument.simulator.api.obj.ModuleLoadInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author angju
 * @date 2021/8/13 20:26
 */
public interface ModuleLoadInfoManager {
    //<moduleId, ModuleLoadInfo>
    Map<String, ModuleLoadInfo> moduleLoadInfos = new HashMap<String, ModuleLoadInfo>();

    /**
     * 添加模块加载信息
     * @param moduleLoadInfo
     */
    void addModuleLoadInfo(ModuleLoadInfo moduleLoadInfo);

    /**
     * 获取所有模块加载
     * @return
     */
    Map<String, ModuleLoadInfo> getModuleLoadInfos();
}
