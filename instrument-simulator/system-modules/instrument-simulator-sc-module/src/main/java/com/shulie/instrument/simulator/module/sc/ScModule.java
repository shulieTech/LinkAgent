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
package com.shulie.instrument.simulator.module.sc;

import com.shulie.instrument.simulator.api.CommandResponse;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.annotation.Command;
import com.shulie.instrument.simulator.api.filter.NameRegexFilter;
import com.shulie.instrument.simulator.api.resource.LoadedClassDataSource;
import com.shulie.instrument.simulator.module.ParamSupported;
import com.shulie.instrument.simulator.module.util.InterfaceNameRegexFilter;
import com.shulie.instrument.simulator.module.util.SuperNameRegexFilter;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 反编译模块扩展，可以通过指定一个匹配规则来反编译指定的类
 *
 * @author xiaobin@shulie.io
 * @since 1.0.0
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "sc", version = "1.0.0", author = "xiaobin@shulie.io", description = "查找类模块")
public class ScModule extends ParamSupported implements ExtensionModule {

    @Resource
    private LoadedClassDataSource loadedClassDataSource;

    @Command(value = "sc", description = "查找类")
    public CommandResponse sc(final Map<String, String> param) {
        try {
            final String cnPattern = getParameter(param, "class");
            final String type = getParameter(param, "type");
            if (StringUtils.isBlank(type)) {
                Set<Class<?>> classes = loadedClassDataSource.find(new NameRegexFilter(cnPattern, ".*", true, true));
                List<String> classNames = new ArrayList<String>();
                for (Class<?> clazz : classes) {
                    String name = clazz.getCanonicalName() + " " + clazz.getClassLoader().toString();
                    classNames.add(name);
                }
                return CommandResponse.success(classNames);
            } else if (StringUtils.equals("s", type)) {
                Set<Class<?>> classes = loadedClassDataSource.find(new SuperNameRegexFilter(cnPattern, ".*", true, true));
                List<String> classNames = new ArrayList<String>();
                for (Class<?> clazz : classes) {
                    String name = clazz.getCanonicalName() + " " + clazz.getClassLoader().toString();
                    classNames.add(name);
                }
                return CommandResponse.success(classNames);
            } else if (StringUtils.equals("s", type)) {
                Set<Class<?>> classes = loadedClassDataSource.find(new InterfaceNameRegexFilter(cnPattern, ".*", true, true));
                List<String> classNames = new ArrayList<String>();
                for (Class<?> clazz : classes) {
                    String name = clazz.getCanonicalName() + " " + clazz.getClassLoader().toString();
                    classNames.add(name);
                }
                return CommandResponse.success(classNames);
            }
            return CommandResponse.failure("Unsupported type value:" + type);
        } catch (Throwable e) {
            return CommandResponse.failure(e);
        }

    }
}
