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

import java.util.Set;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/12 11:23 下午
 */
public interface SwitcherManager {
    /**
     * init switcher manager
     */
    void init();

    /**
     * get switcher is turn on
     *
     * @param switcherName
     * @return
     */
    boolean isSwitchOn(String switcherName);

    /**
     * get all switcher is turn on
     *
     * @param switcherNames
     * @return
     */
    boolean isAllSwitchOn(Set<String> switcherNames);

    /**
     * turn on switcher
     *
     * @param switcherName
     */
    void switchOn(final String switcherName);

    /**
     * turn off switcher
     *
     * @param switcherName
     */
    void switchOff(final String switcherName);

    /**
     * register switcher on callback function
     *
     * @param switcherName
     * @param callback
     */
    void registerMultiSwitchOnCallback(String switcherName, Runnable callback);

    /**
     * multi register switcher on callback function
     *
     * @param switcherName
     * @param callback
     */
    void registerMultiSwitchOnCallback(Set<String> switcherName, Runnable callback);

    /**
     * close switcher manager
     */
    void close();
}
