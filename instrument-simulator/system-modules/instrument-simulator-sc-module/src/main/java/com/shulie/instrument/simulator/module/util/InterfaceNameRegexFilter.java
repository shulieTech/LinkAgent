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
package com.shulie.instrument.simulator.module.util;

import com.shulie.instrument.simulator.api.filter.ClassDescriptor;
import com.shulie.instrument.simulator.api.filter.ExtFilter;
import com.shulie.instrument.simulator.api.filter.MethodDescriptor;
import com.shulie.instrument.simulator.api.listener.ext.BuildingForListeners;
import com.shulie.instrument.simulator.api.util.StringUtil;

import java.util.*;

/**
 * 类名和方法名正则表达式匹配过滤器
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/9/30 11:32 下午
 */
public class InterfaceNameRegexFilter implements ExtFilter {

    /**
     * 类名正则表达式
     */
    private final String interfaceJavaNameRegex;

    /**
     * 方法名正则表达式
     */
    private final String javaMethodRegex;

    /**
     * 是否包含子类
     */
    private boolean isIncludeSubClasses;

    /**
     * 是否包含 bootstrap 加载的类
     */
    private boolean isIncludeBootstrap;

    /**
     * 构造名称正则表达式过滤器
     *
     * @param interfaceJavaNameRegex 类名正则表达式
     * @param javaMethodRegex        方法名正则表达式
     */
    public InterfaceNameRegexFilter(String interfaceJavaNameRegex, String javaMethodRegex) {
        this(interfaceJavaNameRegex, javaMethodRegex, false, false);
    }

    public InterfaceNameRegexFilter(String interfaceJavaNameRegex, String javaMethodRegex, boolean isIncludeBootstrap, boolean isIncludeSubClasses) {
        this.interfaceJavaNameRegex = interfaceJavaNameRegex;
        this.javaMethodRegex = javaMethodRegex;
        this.isIncludeBootstrap = isIncludeBootstrap;
        this.isIncludeSubClasses = isIncludeSubClasses;
    }

    @Override
    public boolean doClassNameFilter(String javaClassName) {
        return true;
    }

    @Override
    public boolean doClassFilter(ClassDescriptor classDescriptor) {
        String[] interfaceNames = classDescriptor.getInterfaceTypeJavaClassNameArray();
        for (String interfaceName : interfaceNames) {
            boolean matches = StringUtil.matching(interfaceName, interfaceJavaNameRegex);
            if (matches) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<BuildingForListeners> doMethodFilter(MethodDescriptor methodDescriptor) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<BuildingForListeners> getAllListeners() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Set<String> getAllListeningTypes() {
        return new HashSet<String>(Arrays.asList(interfaceJavaNameRegex));
    }

    @Override
    public boolean isIncludeSubClasses() {
        return isIncludeSubClasses;
    }

    @Override
    public boolean isIncludeBootstrap() {
        return isIncludeBootstrap;
    }
}
