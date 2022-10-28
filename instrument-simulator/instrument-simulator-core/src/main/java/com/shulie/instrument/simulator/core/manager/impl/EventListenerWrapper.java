/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.simulator.core.manager.impl;

import com.shulie.instrument.simulator.api.ProcessControlEntity;
import com.shulie.instrument.simulator.api.event.Event;
import com.shulie.instrument.simulator.api.listener.Destroyed;
import com.shulie.instrument.simulator.api.listener.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/16 11:27 上午
 */
public class EventListenerWrapper extends EventListener {
    private static final Logger logger = LoggerFactory.getLogger(EventListenerWrapper.class);
    private EventListener eventListener;
    private Destroyed destroyed;

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setDestroyed(Destroyed destroyed) {
        this.destroyed = destroyed;
    }

    @Override
    public void setBizClassLoader(ClassLoader classLoader) {
        eventListener.setBizClassLoader(classLoader);
    }

    @Override
    public ClassLoader getBizClassLoader() {
        return eventListener.getBizClassLoader();
    }

    @Override
    public ProcessControlEntity onEvent(Event event) throws Throwable {
        return eventListener.onEvent(event);
    }

    @Override
    public void clean() {
        if (destroyed != null) {
            try {
                // destroy的类里可能会加载业务的类，如果业务类不存在会导致 destroyed类加载失败，从而影响其他插件卸载
                destroyed.destroy();
            } catch (Throwable t) {
                logger.error("[destroy]" + destroyed.getClass() + " ERROR", t);
            }
        }
        eventListener.clean();
    }
}
