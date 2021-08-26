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

import com.shulie.instrument.simulator.api.event.Event;
import com.shulie.instrument.simulator.api.listener.Destroyed;
import com.shulie.instrument.simulator.api.listener.EventListener;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/16 11:27 上午
 */
public class EventListenerWrapper extends EventListener {
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
    public void onEvent(Event event) throws Throwable {
        eventListener.onEvent(event);
    }

    @Override
    public void clean() {
        if (destroyed != null) {
            destroyed.destroy();
        }
        eventListener.clean();
    }
}
