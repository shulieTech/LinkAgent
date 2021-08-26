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

import com.shulie.instrument.simulator.api.ModuleException;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.event.Event;
import com.shulie.instrument.simulator.api.event.EventType;
import com.shulie.instrument.simulator.api.listener.*;
import com.shulie.instrument.simulator.api.listener.ext.AdviceAdapterListener;
import com.shulie.instrument.simulator.api.listener.ext.AdviceListener;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.util.ObjectIdUtils;
import com.shulie.instrument.simulator.core.CoreModule;
import com.shulie.instrument.simulator.core.classloader.BizClassLoaderHolder;
import com.shulie.instrument.simulator.core.extension.ExtensionAdviceWrapContainer;
import com.shulie.instrument.simulator.core.extension.GlobalAdviceWrapBuilders;
import com.shulie.instrument.simulator.core.util.ReflectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 延迟加载的事件监听器代理
 * 每个延迟加载的对象持有多个EventListener实例，因为要考虑到多类加载器问题
 * 增强的目标类可能会多个
 *
 * @author xiaobin.zfb
 * @since 2020/9/18 12:48 上午
 */
public class LazyEventListenerProxy extends EventListener implements Interruptable {
    private final Logger logger = LoggerFactory.getLogger(LazyEventListenerProxy.class.getName());
    private final CoreModule coreModule;
    private final Listeners listeners;
    /**
     * 所有的EventListener,一个业务类加载器对应一个EventListener实例
     * key -> 业务classloader id : EventListener
     * <p>
     * 测试过 jctools 的 NonBlockingHashMap,性能比 ConcurrentHashMap 大概低30-40%左右
     */
    private final ConcurrentHashMap<Integer, EventListenerWrapper> eventListeners;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    private final AtomicBoolean isInitInterrupt = new AtomicBoolean(false);
    /**
     * 是否是可中断的
     */
    private boolean isInterrupt;

    /**
     * 监听器耗时
     */
    private boolean listenerCostEnabled;

    public LazyEventListenerProxy(final CoreModule coreModule, final Listeners listeners) {
        this.coreModule = coreModule;
        this.listeners = listeners;
        this.eventListeners = new ConcurrentHashMap<Integer, EventListenerWrapper>();
        this.listenerCostEnabled = Boolean.valueOf(System.getProperty("simulator.listener.cost.enabled", "false"));
    }

    @Override
    public void onEvent(Event event) throws Throwable {
        if (listeners == null || StringUtils.isBlank(listeners.getClassName())) {
            return;
        }
        if (!isRunning.get()) {
            return;
        }

        EventListenerWrapper listener = getEventListenerWrapper();
        if (listener == null) {
            logger.error("SIMULATOR: event listener onEvent failed, cause by event listener init failed.");
        } else {
            listener.setBizClassLoader(BizClassLoaderHolder.getBizClassLoader());
            if (!listenerCostEnabled) {
                listener.onEvent(event);
            } else {
                long start = System.nanoTime();
                try {
                    listener.onEvent(event);
                } finally {
                    long end = System.nanoTime();
                    logger.info("{} execute {} cost {} ns", listeners.getClassName(), EventType.name(event.getType()), (end - start));
                }
            }
        }
    }

    @Override
    public void clean() {
        if (!isRunning.compareAndSet(true, false)) {
            return;
        }
        /**
         * 执行所有的资源的清理
         */
        Iterator<Map.Entry<Integer, EventListenerWrapper>> it = eventListeners.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, EventListenerWrapper> entry = it.next();
            EventListenerWrapper eventListenerWrapper = entry.getValue();
            it.remove();
            ClassLoader bizClassLoader = eventListenerWrapper.getBizClassLoader();
            if (bizClassLoader != null) {
                ClassLoader classLoader = BizClassLoaderHolder.getBizClassLoader();
                try {
                    BizClassLoaderHolder.setBizClassLoader(bizClassLoader);
                    eventListenerWrapper.clean();
                } finally {
                    BizClassLoaderHolder.setBizClassLoader(classLoader);
                }
            } else {
                eventListenerWrapper.clean();
            }
        }
        eventListeners.clear();
    }

    /**
     * 如果是非中间件模块，则不会根据业务类加载器生成多实例，这是为了支持
     * 基础的全局模块被多个模块同时依赖时需要保证类一致，对象一致
     *
     * @return
     */
    private int getBizClassLoaderId() {
        if (!coreModule.isMiddlewareModule()) {
            return 0;
        }
        return ObjectIdUtils.identity(BizClassLoaderHolder.getBizClassLoader());
    }

    /**
     * 获取事件监听器，会涉及到初始化的动作
     * <p>
     * 非中间件模块使用单实例，不会根据业务类加载器生成多实例
     *
     * @return 返回事件监听器
     */
    private EventListenerWrapper getEventListenerWrapper() {
        int id = getBizClassLoaderId();
        EventListenerWrapper eventListenerWrapper = this.eventListeners.get(id);
        if (eventListenerWrapper != null) {
            return eventListenerWrapper;
        }
        synchronized (this) {
            eventListenerWrapper = this.eventListeners.get(id);
            if (eventListenerWrapper != null) {
                return eventListenerWrapper;
            }
            ClassLoader classLoader = coreModule.getClassLoader(BizClassLoaderHolder.getBizClassLoader());
            try {
                Object listener = null;
                if (listeners.getArgs() == null || listeners.getArgs().length == 0) {
                    listener = Reflect.on(listeners.getClassName(), classLoader).create().get();
                } else {
                    listener = Reflect.on(listeners.getClassName(), classLoader).create(listeners.getArgs()).get();
                }

                /**
                 * 设置是否可中断
                 */
                if (isInitInterrupt.compareAndSet(false, true)) {
                    isInterrupt = ReflectUtils.isInterruptEventHandler(listener);
                }

                if (listener != null) {
                    eventListenerWrapper = new EventListenerWrapper();
                    try {
                        coreModule.injectResource(listener);
                    } catch (ModuleException e) {
                        logger.warn("SIMULATOR: can't inject resource into event listener. by module={} listener:{}", coreModule.getModuleId(), listeners, e);
                    }
                    if (listener instanceof InitializingBean) {
                        ((InitializingBean) listener).init();
                    }
                    EventListener eventListener = null;
                    if (listener instanceof EventListener) {
                        eventListener = (EventListener) listener;

                        if (listeners.getScopeName() != null && listeners.getEventListenerCallback() != null) {
                            eventListener = listeners.getEventListenerCallback().onCall(eventListener, listeners.getScopeName(), listeners.getExecutionPolicy());
                        }
                        eventListener.setBizClassLoader(BizClassLoaderHolder.getBizClassLoader());
                    } else if (listener instanceof AdviceListener) {
                        AdviceListener adviceListener = (AdviceListener) listener;
                        adviceListener = new ExtensionAdviceWrapContainer(adviceListener);
                        if (listeners.getScopeName() != null && listeners.getAdviceListenerCallback() != null) {
                            adviceListener = listeners.getAdviceListenerCallback().onCall(adviceListener, listeners.getScopeName(), listeners.getExecutionPolicy());
                        }
                        adviceListener.setBizClassLoader(BizClassLoaderHolder.getBizClassLoader());
                        eventListener = new AdviceAdapterListener(adviceListener);
                    }
                    eventListenerWrapper.setEventListener(eventListener);

                    Destroyable destroyable = listener.getClass().getAnnotation(Destroyable.class);
                    if (destroyable != null) {
                        Destroyed destroyed = Reflect.on(destroyable.value()).create().get();
                        eventListenerWrapper.setDestroyed(destroyed);
                    }
                }

                if (eventListenerWrapper != null) {
                    EventListenerWrapper old = this.eventListeners.putIfAbsent(id, eventListenerWrapper);
                    if (old != null) {
                        eventListenerWrapper = old;
                    }
                }
            } catch (Throwable e) {
                logger.error("SIMULATOR: event listener onEvent failed, cause by event listener init failed:{}.", listeners.getClassName(), e);
                return null;
            }
        }
        return eventListenerWrapper;
    }

    @Override
    public boolean isInterrupted() {
        if (isInitInterrupt.get()) {
            return this.isInterrupt;
        }
        try {
            if (!eventListeners.isEmpty()) {
                EventListener eventListener = eventListeners.values().iterator().next();
                boolean isInterrupt = ReflectUtils.isInterruptEventHandler(eventListener);
                if (isInitInterrupt.compareAndSet(false, true)) {
                    this.isInterrupt = isInterrupt;
                    return this.isInterrupt;
                }
            }
            Class clazz = coreModule.getClassLoaderFactory().getDefaultClassLoader().loadClass(listeners.getClassName());
            boolean isInterrupt = ReflectUtils.isInterruptEventHandler(clazz);
            if (isInitInterrupt.compareAndSet(false, true)) {
                this.isInterrupt = isInterrupt;
            }
            return this.isInterrupt;
        } catch (ClassNotFoundException e) {
            logger.error("SIMULATOR: can't found class {} by ModuleClassLoader:{}.", listeners.getClassName(), coreModule.getClassLoaderFactory().getDefaultClassLoader(), e);
            return false;
        } catch (Throwable e) {
            logger.error("SIMULATOR: can't found class {} by ModuleClassLoader:{}.", listeners.getClassName(), coreModule.getClassLoaderFactory().getDefaultClassLoader(), e);
            return false;
        }
    }
}
