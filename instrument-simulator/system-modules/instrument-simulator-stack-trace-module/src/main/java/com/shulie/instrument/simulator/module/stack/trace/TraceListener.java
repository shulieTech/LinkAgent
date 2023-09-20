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
package com.shulie.instrument.simulator.module.stack.trace;

import com.shulie.instrument.simulator.api.filter.ClassDescriptor;
import com.shulie.instrument.simulator.api.filter.ExtFilter;
import com.shulie.instrument.simulator.api.filter.Filter;
import com.shulie.instrument.simulator.api.filter.MethodDescriptor;
import com.shulie.instrument.simulator.api.listener.Listeners;
import com.shulie.instrument.simulator.api.listener.ext.*;
import com.shulie.instrument.simulator.api.resource.LoadedClassDataSource;
import com.shulie.instrument.simulator.api.resource.ModuleEventWatcher;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import com.shulie.instrument.simulator.module.model.trace2.StackEvent;
import com.shulie.instrument.simulator.module.model.trace2.TraceNode;
import com.shulie.instrument.simulator.module.model.trace2.TraceView;
import com.shulie.instrument.simulator.module.model.utils.ResultSerializer;
import com.shulie.instrument.simulator.module.stack.trace.express.ExpressException;
import com.shulie.instrument.simulator.module.stack.trace.util.PatternMatchUtils;
import com.shulie.instrument.simulator.module.stack.trace.util.ThreadUtil;
import com.shulie.instrument.simulator.module.stack.trace.util.TraceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import static com.shulie.instrument.simulator.api.listener.ext.PatternType.WILDCARD;

/**
 * @author xiaobin.zfb
 * @since 2020/9/18 3:26 下午
 */
public class TraceListener extends AdviceListener {
    private final Logger logger = LoggerFactory.getLogger(TraceListener.class);

    @Resource
    private ModuleEventWatcher moduleEventWatcher;
    @Resource
    private LoadedClassDataSource loadedClassDataSource;

    /**
     * 方法执行耗时
     */
    public static final String COST_VARIABLE = "cost";
    public static final String TARGET = "target";
    public static final String TARGET_CLASS = "targetClass";
    public static final String PARAMS = "params";
    public static final String RETURN_OBJ = "returnObj";
    public static final String THROWABLE = "throwExp";
    private final int level;
    private final int limits;
    private final int wait;
    private final int stopInMills;
    private final Map<String, Queue<TraceView>> traceViews;
    private Set<String> traceMethods;
    private Set<EventWatcher> eventWatchers;
    private CountDownLatch latch;
    private String[] classPatterns;
    private long startMills = System.currentTimeMillis();
    private final static ThreadLocal<TraceView> traceViewThreadLocal = new ThreadLocal<TraceView>();

    public TraceListener(String[] classPatterns,
        CountDownLatch latch,
        Map<String, Queue<TraceView>> traceViews,
        Set<String> traceMethods,
        Set<EventWatcher> eventWatchers,
        final int level,
        final int limits,
        final int stopInMills,
        final int wait) {
        this.classPatterns = classPatterns;
        this.latch = latch;
        this.traceViews = traceViews;
        this.eventWatchers = eventWatchers;
        this.traceMethods = traceMethods;
        this.level = level;
        this.limits = limits;
        this.stopInMills = stopInMills;
        this.wait = wait;
    }

    @Override
    public void beforeCall(final Advice advice,
        final int callLineNum,
        final boolean isInterface,
        final String callJavaClassName,
        final String callJavaMethodName,
        final String callJavaMethodDesc) {
        TraceView traceView = traceViewThreadLocal.get();
        traceView.begin(callJavaClassName, callJavaMethodName, advice.getClassLoader().toString(),
                StackEvent.LINE_CALL)
            .setLine(callLineNum);
        //addNextLevelTrace(traceView.getCurrent());
    }

    @Override
    public void before(Advice advice) throws Throwable {
        TraceView traceView;
        if ((traceView = traceViewThreadLocal.get()) == null) {
            traceView = new TraceView(advice.getTargetClass().getName(), advice.getBehavior().getName(),
                advice.getClassLoader().toString());
            TraceInfo traceInfo = ThreadUtil.getTraceInfo(Thread.currentThread());
            if (traceInfo != null) {
                traceView.setTraceId(traceInfo.getTraceId());
                traceView.setRpcId(traceInfo.getRpcId());
                traceView.setClusterTestBefore(traceInfo.isClusterTest());
            }
            Object[] args = advice.getParameterArray();
            //beforeCall在这之前调用，已经设置了current了
            traceView.getCurrent().setArgs(ResultSerializer.serializeObject(args, 2));
            traceViewThreadLocal.set(traceView);
        } else {
            if (traceView.getCurrent().getClassName().equals(advice.getTargetClass().getName()) &&
                traceView.getCurrent().getMethodName().equals(advice.getBehavior().getName()) &&
                StackEvent.LINE_CALL.equals(traceView.lastStage())) {
                traceView.appendMethodCall();
            } else {
                traceView.begin(advice.getTargetClass().getName(), advice.getBehavior().getName(),
                    advice.getClassLoader().toString(), StackEvent.METHOD_CALL);
            }
        }
        Object[] args = advice.getParameterArray();
        //beforeCall在这之前调用，已经设置了current了
        traceView.getCurrent().setArgs(ResultSerializer.serializeObject(args, 2));
    }

    @Override
    public void afterReturning(Advice advice) throws Throwable {
        Object resultObj = advice.getReturnObj();
        Throwable throwable = advice.getThrowable();
        TraceView traceView = traceViewThreadLocal.get();
        if (throwable != null) {
            traceView.getCurrent().setErrorMsg(ResultSerializer.serializeObject(throwable, 2));
        }else if (resultObj != null) {
            traceView.getCurrent().setResult(ResultSerializer.serializeObject(resultObj, 2));
        }
        if (traceView.currentIsRoot()) {
            traceView.setTotalCost(traceView.getRootCost());
            traceView.setClassloader(traceView.getRoot() == null ? null : traceView.getRoot().getClassloader());
            TraceInfo traceInfo = ThreadUtil.getTraceInfo(Thread.currentThread());
            if (traceInfo != null) {
                traceView.setClusterTestAfter(traceInfo.isClusterTest());
            }
            rootTrace(advice, traceView);
            traceViewThreadLocal.remove();
        } else {
            traceView.end();
        }
    }

    @Override
    public void afterThrowing(Advice advice) throws Throwable {
        afterReturning(advice);
    }

    @Override
    public void afterCallReturning(final Advice advice,
        final int callLineNum,
        final boolean isInterface,
        final String callJavaClassName,
        final String callJavaMethodName,
        final String callJavaMethodDesc) {
        //afterReturning有可能在这之前调用，已经设置了current了
        TraceView traceView = traceViewThreadLocal.get();
        traceView.end();
    }

    @Override
    public void afterCallThrowing(final Advice advice,
        final int callLineNum,
        final boolean isInterface,
        final String callJavaClassName,
        final String callJavaMethodName,
        final String callJavaMethodDesc,
        final Throwable callThrowable) {
        TraceView traceView = traceViewThreadLocal.get();
        traceView.getCurrent().setErrorMsg(ResultSerializer.serializeObject(callThrowable, 2));
        traceView.end();
    }

    /**
     * 添加下一级的 trace
     *
     * @param traceNode
     */
    private void addNextLevelTrace(final TraceNode traceNode) {
        if (traceNode != null
            && PatternMatchUtils.patternMatching(traceNode.getClassName(), classPatterns, WILDCARD)
            && traceMethods.add(traceNode.getClassName() + "." + traceNode.getMethodName())
            && !traceNode.isSkip(stopInMills)) {
            /**
             * 如果是接口则找出所有的实现类并增强
             */
            String methodName = traceNode.getMethodName();
            Set<Class> allClassForEnhance = new HashSet<Class>();
            if (traceNode.isInterface()) {
                Set<Class<?>> classes = findImplClasses(traceNode.getClassName());
                if (CollectionUtils.isNotEmpty(classes)) {
                    /**
                     * 增强所有接口的实现类
                     */
                    for (Class clazz : classes) {
                        allClassForEnhance.add(clazz);
                        allClassForEnhance.addAll(findSuperClass(clazz));
                    }
                }
            } else {
                Set<Class<?>> classes = findClasses(traceNode.getClassName());
                allClassForEnhance.addAll(classes);
                for (Class<?> aClass : classes) {
                    allClassForEnhance.addAll(findSuperClass(aClass));
                }
            }
            for (Class aClass : allClassForEnhance) {
                enhance(methodName, aClass.getName());
            }
        }
    }

    private Collection<Class> findSuperClass(Class clazz) {
        Class superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.getName().startsWith("java") && PatternMatchUtils.patternMatching(
            superClass.getName(),
            classPatterns, WILDCARD)) {
            Set<Class> result = new HashSet<Class>();
            result.add(superClass);
            result.addAll(findSuperClass(superClass));
            return result;
        }
        return Collections.emptySet();
    }

    private void enhance(String methodName, String name) {
        final EventWatcher watcher = new EventWatchBuilder(moduleEventWatcher)
            .onClass(name).includeSubClasses()
            .onAnyBehavior(methodName)
            .withInvoke().withCall()
            .onListener(Listeners.of(getClass(),
                new Object[] {classPatterns, latch, traceViews, traceMethods, eventWatchers,
                    level - 1, limits, stopInMills, wait}))
            .onClass().onWatch();
        eventWatchers.add(watcher);
    }

    private Set<Class<?>> findImplClasses(final String className) {
        return loadedClassDataSource.find(new ExtFilter() {
            @Override
            public boolean isIncludeSubClasses() {
                return false;
            }

            @Override
            public boolean isIncludeBootstrap() {
                return true;
            }

            @Override
            public boolean doClassNameFilter(String javaClassName) {
                return true;
            }

            @Override
            public boolean doClassFilter(ClassDescriptor classDescriptor) {
                String[] interfaces = classDescriptor.getInterfaceTypeJavaClassNameArray();
                return indexOf(interfaces, className,0) >= 0;
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
                return Collections.EMPTY_SET;
            }
        });
    }

    private Set<Class<?>> findClasses(final String className) {
        return loadedClassDataSource.find(new Filter() {

            @Override
            public boolean doClassNameFilter(String javaClassName) {
                return javaClassName.equals(className);
            }

            @Override
            public boolean doClassFilter(ClassDescriptor classDescriptor) {
                return classDescriptor.getClassName().equals(className);
            }

            @Override
            public List<BuildingForListeners> doMethodFilter(MethodDescriptor methodDescriptor) {
                return Collections.emptyList();
            }

            @Override
            public List<BuildingForListeners> getAllListeners() {
                return Collections.emptyList();
            }

            @Override
            public Set<String> getAllListeningTypes() {
                return Collections.EMPTY_SET;
            }
        });
    }

    /**
     * 只有根节点才会进行追踪
     *
     * @param advice    advice
     * @param traceView traceView
     * @throws ExpressException 如果表达式出现错误会抛出 ExpressException
     */
    private void rootTrace(Advice advice, TraceView traceView) throws ExpressException {
        if (limits <= 0 || traceViews.size() < limits) {
            traceView.setDaemon(Thread.currentThread().isDaemon());
            traceView.setPriority(Thread.currentThread().getPriority());
            traceView.setThreadId(Thread.currentThread().getId());
            traceView.setThreadName(Thread.currentThread().getName());
            if (advice.getReturnObj() != null) {
                traceView.setResult(ResultSerializer.serializeObject(advice.getReturnObj(), 2));
            }
            /**
             * 如果是顶点则获取监测的数据
             */
            String traceId = traceView.getTraceId();
            if (traceId == null || traceId.length() == 0) {
                traceId = "NONE";
            }
            Queue<TraceView> traceViewQueue = traceViews.get(traceId);
            if (traceViewQueue == null) {
                synchronized (traceViews) {
                    traceViewQueue = traceViews.get(traceId);
                    if (traceViewQueue == null) {
                        traceViewQueue = new ConcurrentLinkedQueue<TraceView>();
                        traceViews.put(traceId, traceViewQueue);
                    }
                }
            }
            traceViewQueue.add(traceView);
        }
        if (limits > 0 && traceViews.size() >= limits
            || (wait > 0 && (System.currentTimeMillis() - startMills) >= wait)) {
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    public static int indexOf(Object[] array, Object objectToFind, int startIndex) {
        if (array == null) {
            return -1;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        if (objectToFind == null) {
            for (int i = startIndex; i < array.length; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        } else if (array.getClass().getComponentType().isInstance(objectToFind)) {
            for (int i = startIndex; i < array.length; i++) {
                if (objectToFind.equals(array[i])) {
                    return i;
                }
            }
        }
        return -1;
    }
}
