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
package com.shulie.instrument.simulator.core.manager.impl;

import com.shulie.instrument.simulator.api.resource.ModuleLoader;
import com.shulie.instrument.simulator.api.resource.SwitcherManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/12 11:26 下午
 */
public class DefaultSwitcherManager implements SwitcherManager {
    private final Logger logger = LoggerFactory.getLogger(DefaultSwitcherManager.class);

    private ConcurrentHashMap<String, AtomicBoolean> switches = new ConcurrentHashMap<String, AtomicBoolean>();
    private ConcurrentHashMap<Set<String>, Queue<Runnable>> aggrSwitchCallbacks = new ConcurrentHashMap<Set<String>, Queue<Runnable>>();

    private ModuleLoader moduleLoader;

    public DefaultSwitcherManager(ModuleLoader moduleLoader) {
        this.moduleLoader = moduleLoader;
    }

    @Override
    public void init() {
    }

    @Override
    public boolean isSwitchOn(String switcherName) {
        AtomicBoolean switcher = switches.get(switcherName);
        return switcher == null ? false : switcher.get();
    }

    @Override
    public boolean isAllSwitchOn(Set<String> switcherNames) {
        if (switcherNames == null || switcherNames.isEmpty()) {
            return true;
        }
        for (String switcherName : switcherNames) {
            if (!isSwitchOn(switcherName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void switchOn(final String switcherName) {
        if (moduleLoader == null) {
            //release if null
            return;
        }
        moduleLoader.load(new Runnable() {
            @Override
            public void run() {
                if (switches == null) {
                    //release if null
                    return;
                }
                AtomicBoolean switcher = switches.get(switcherName);
                if (switcher == null) {
                    switcher = new AtomicBoolean(false);
                    AtomicBoolean oldSwitcher = switches.putIfAbsent(switcherName, switcher);
                    if (oldSwitcher != null) {
                        switcher = oldSwitcher;
                    }
                }

                if (switcher.compareAndSet(false, true)) {
                    List<Set<String>> switchers = new ArrayList<Set<String>>();
                    if (aggrSwitchCallbacks == null) {
                        //release if null
                        return;
                    }
                    for (Map.Entry<Set<String>, Queue<Runnable>> entry : aggrSwitchCallbacks.entrySet()) {
                        if (!entry.getKey().contains(switcherName)) {
                            continue;
                        }
                        if (isAllSwitchOn(entry.getKey())) {
                            switchers.add(entry.getKey());
                        }
                    }
                    for (Set<String> switcherNames : switchers) {
                        if (aggrSwitchCallbacks == null) {
                            //release if null
                            return;
                        }
                        Queue<Runnable> queue = aggrSwitchCallbacks.remove(switcherNames);
                        if (queue != null) {
                            while (true) {
                                Runnable runnable = queue.poll();
                                if (runnable == null) {
                                    break;
                                }
                                try {
                                    runnable.run();
                                } catch (Throwable e) {
                                    logger.error("SIMULATOR: execute global switcher[{}] on err! {}", switcherName, runnable, e);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void switchOff(String switcherName) {

    }

    @Override
    public void registerMultiSwitchOnCallback(String switcherName, Runnable callback) {
        Set<String> sets = new HashSet<String>();
        sets.add(switcherName);
        registerMultiSwitchOnCallback(sets, callback);
    }

    @Override
    public void registerMultiSwitchOnCallback(Set<String> switcherName, Runnable callback) {
        if (aggrSwitchCallbacks == null) {
            //release if null
            return;
        }
        Queue<Runnable> callbacks = aggrSwitchCallbacks.get(switcherName);
        if (callbacks == null) {
            callbacks = new ConcurrentLinkedQueue<Runnable>();
            Queue<Runnable> oldCallbacks = aggrSwitchCallbacks.putIfAbsent(switcherName, callbacks);
            if (oldCallbacks != null) {
                callbacks = oldCallbacks;
            }
        }

        callbacks.add(callback);
    }

    @Override
    public void close() {
        switches.clear();
        aggrSwitchCallbacks.clear();
        switches = null;
        aggrSwitchCallbacks = null;
        moduleLoader = null;
    }
}
