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
package com.shulie.instrument.simulator.api.listener.ext;

import com.shulie.instrument.simulator.api.ProcessControlEntity;
import com.shulie.instrument.simulator.api.ProcessController;
import com.shulie.instrument.simulator.api.annotation.Interrupted;
import com.shulie.instrument.simulator.api.event.*;
import com.shulie.instrument.simulator.api.listener.EventListener;
import com.shulie.instrument.simulator.api.listener.Interruptable;
import com.shulie.instrument.simulator.api.util.SimulatorStack;
import com.shulie.instrument.simulator.api.util.StringUtil;
import com.shulie.instrument.simulator.api.util.ThreadUnsafeSimulatorStack;

import static com.shulie.instrument.simulator.api.event.EventType.*;

/**
 * 通知监听器
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/23 10:45 下午
 */
public class AdviceAdapterListener extends EventListener implements Interruptable {

    private final AdviceListener adviceListener;
    private final boolean isInterruptable;

    public AdviceAdapterListener(final AdviceListener adviceListener) {
        this.adviceListener = adviceListener;
        if (adviceListener.getBizClassLoader() != null) {
            setBizClassLoader(adviceListener.getBizClassLoader());
        }
        this.isInterruptable = isInterruptEventHandler(adviceListener);
    }

    private final ThreadLocal<OpStack> opStackRef = new ThreadLocal<OpStack>() {
        @Override
        protected OpStack initialValue() {
            return new OpStack();
        }
    };

    @Override
    final public ProcessControlEntity onEvent(final Event event) throws Throwable {
        final OpStack opStack = opStackRef.get();
        try {
            return switchEvent(opStack, event);
        } finally {
            // 如果执行到TOP的最后一个事件，则需要主动清理占用的资源
            if (opStack.isEmpty()) {
                opStackRef.remove();
            }
        }

    }

    @Override
    public void clean() {
        opStackRef.remove();
        adviceListener.clean();
    }


    // 执行事件
    private ProcessControlEntity switchEvent(final OpStack opStack,
                             final Event event) throws Throwable {

        switch (event.getType()) {
            case BEFORE: {
                final BeforeEvent bEvent = (BeforeEvent) event;
                final ClassLoader loader = toClassLoader(bEvent.getJavaClassLoader());
                final Advice advice = new Advice(
                        bEvent.getProcessId(),
                        bEvent.getInvokeId(),
                        bEvent.getJavaMethodName(),
                        toBehavior(bEvent.getClazz(),bEvent.getJavaMethodName(),bEvent.getJavaMethodDesc()),
                        bEvent.getClazz(),
                        loader,
                        bEvent.getArgumentArray(),
                        bEvent.getTarget()
                );

                final Advice top;
                final Advice parent;

                // 顶层调用
                if (opStack.isEmpty()) {
                    top = parent = advice;
                }

                // 非顶层
                else {
                    parent = opStack.peek();
                    top = parent.getProcessTop();
                }

                advice.applyBefore(top, parent);

                opStack.pushForBegin(advice);
                adviceListener.before(advice);
                return ProcessControlEntity.fromAdvice(advice);
            }

            /**
             * 这里需要感知到IMMEDIATELY，因为如果 IMMEDIATELY 事件在 BEFORE中触发时
             * 需要清空后续的调用栈
             */
            case IMMEDIATELY_THROWS:
            case IMMEDIATELY_RETURN: {
                final InvokeEvent invokeEvent = (InvokeEvent) event;
                Advice advice = opStack.popByExpectInvokeId(invokeEvent.getInvokeId());
                try {
                    return ProcessControlEntity.fromAdvice(advice);
                } finally {
                    if (advice != null) {
                        advice.destroy();
                    }
                }
            }

            case RETURN: {
                final ReturnEvent rEvent = (ReturnEvent) event;
                final Advice wrapAdvice = opStack.popByExpectInvokeId(rEvent.getInvokeId());
                if (null != wrapAdvice) {
                    Advice advice = wrapAdvice.applyReturn(rEvent.getReturnObj());
                    try {
                        adviceListener.afterReturning(advice);
                    } finally {
                        adviceListener.after(advice);
                    }
                    try {
                        return ProcessControlEntity.fromAdvice(wrapAdvice);
                    } finally {
                        wrapAdvice.destroy();
                    }
                }
            }
            case THROWS: {
                final ThrowsEvent tEvent = (ThrowsEvent) event;
                final Advice wrapAdvice = opStack.popByExpectInvokeId(tEvent.getInvokeId());
                if (null != wrapAdvice) {
                    Advice advice = wrapAdvice.applyThrows(tEvent.getThrowable());
                    try {
                        adviceListener.afterThrowing(advice);
                    } finally {
                        adviceListener.after(advice);
                    }
                    try {
                        return ProcessControlEntity.fromAdvice(wrapAdvice);
                    } finally {
                        wrapAdvice.destroy();
                    }
                }
            }

            case CALL_BEFORE: {
                final CallBeforeEvent cbEvent = (CallBeforeEvent) event;
                final Advice advice = opStack.peekByExpectInvokeId(cbEvent.getInvokeId());
                if (null == advice) {
                    return ProcessControlEntity.none();
                }
                final CallTarget target;
                advice.setCallTarget(target = new CallTarget(
                        cbEvent.getLineNumber(),
                        cbEvent.isInterface(),
                        toJavaClassName(cbEvent.getOwner()),
                        cbEvent.getName(),
                        cbEvent.getDesc()
                ));
                adviceListener.beforeCall(
                        advice,
                        target.callLineNum,
                        target.isInterface,
                        target.callJavaClassName,
                        target.callJavaMethodName,
                        target.callJavaMethodDesc
                );
                return ProcessControlEntity.fromAdvice(advice);
            }

            case CALL_RETURN: {
                final CallReturnEvent crEvent = (CallReturnEvent) event;
                final Advice advice = opStack.peekByExpectInvokeId(crEvent.getInvokeId());
                if (null == advice) {
                    return ProcessControlEntity.none();
                }
                final CallTarget target = (CallTarget) advice.getCallTarget();
                if (null == target) {
                    // 这里做一个容灾保护，防止在callBefore()中发生什么异常导致beforeCall()之前失败
                    return ProcessControlEntity.none();
                }
                try {
                    adviceListener.afterCallReturning(
                            advice,
                            target.callLineNum,
                            target.isInterface,
                            target.callJavaClassName,
                            target.callJavaMethodName,
                            target.callJavaMethodDesc
                    );
                } finally {
                    adviceListener.afterCall(
                            advice,
                            target.callLineNum,
                            target.callJavaClassName,
                            target.callJavaMethodName,
                            target.callJavaMethodDesc,
                            null
                    );
                }
                return ProcessControlEntity.fromAdvice(advice);
            }

            case CALL_THROWS: {
                final CallThrowsEvent ctEvent = (CallThrowsEvent) event;
                final Advice advice = opStack.peekByExpectInvokeId(ctEvent.getInvokeId());
                if (null == advice) {
                    return ProcessControlEntity.none();
                }
                final CallTarget target = (CallTarget) advice.getCallTarget();
                if (null == target) {
                    // 这里做一个容灾保护，防止在callBefore()中发生什么异常导致beforeCall()之前失败
                    return ProcessControlEntity.none();
                }
                try {
                    adviceListener.afterCallThrowing(
                            advice,
                            target.callLineNum,
                            target.isInterface,
                            target.callJavaClassName,
                            target.callJavaMethodName,
                            target.callJavaMethodDesc,
                            ctEvent.getThrowException()
                    );
                } finally {
                    adviceListener.afterCall(
                            advice,
                            target.callLineNum,
                            target.callJavaClassName,
                            target.callJavaMethodName,
                            target.callJavaMethodDesc,
                            ctEvent.getThrowException()
                    );
                }
                return ProcessControlEntity.fromAdvice(advice);
            }

            case LINE: {
                final LineEvent lEvent = (LineEvent) event;
                final Advice advice = opStack.peekByExpectInvokeId(lEvent.getInvokeId());
                if (null == advice) {
                    return ProcessControlEntity.none();
                }
                adviceListener.beforeLine(advice, lEvent.getLineNumber());
                return ProcessControlEntity.fromAdvice(advice);
            }

            default:
                //ignore
        }//switch
        return ProcessControlEntity.none();
    }

    @Override
    public boolean isInterrupted() {
        return isInterruptable;
    }

    /**
     * 判断是否是中断式事件处理器
     *
     * @param adviceListener 事件监听器
     * @return TRUE:中断式;FALSE:非中断式
     */
    private static boolean isInterruptEventHandler(final AdviceListener adviceListener) {
        if (adviceListener.getClass().isAnnotationPresent(Interrupted.class)) {
            return true;
        }
        if (adviceListener instanceof Interruptable) {
            return ((Interruptable) adviceListener).isInterrupted();
        }
        return false;
    }


    // --- 以下为内部操作实现 ---


    /**
     * 通知操作堆栈
     */
    private class OpStack {

        private final SimulatorStack<Advice> adviceStack = new ThreadUnsafeSimulatorStack<Advice>();

        boolean isEmpty() {
            return adviceStack.isEmpty();
        }

        Advice peek() {
            return adviceStack.peek();
        }

        void pushForBegin(final Advice advice) {
            adviceStack.push(advice);
        }

        Advice pop() {
            return !adviceStack.isEmpty()
                    ? adviceStack.pop()
                    : null;
        }

        /**
         * 在通知堆栈中，BEFORE:[RETURN/THROWS]的invokeId是配对的，
         * 如果发生错位则说明BEFORE的事件没有被成功压入堆栈，没有被正确的处理，外界没有正确感知BEFORE
         * 所以这里也要进行修正行的忽略对应的[RETURN/THROWS]
         *
         * @param expectInvokeId 期待的invokeId
         *                       必须要求和BEFORE的invokeId配对
         * @return 如果invokeId配对成功，则返回对应的Advice，否则返回null
         */
        Advice popByExpectInvokeId(final int expectInvokeId) {
            return !adviceStack.isEmpty()
                    && adviceStack.peek().getInvokeId() == expectInvokeId
                    ? adviceStack.pop()
                    : null;
        }

        Advice peekByExpectInvokeId(final int expectInvokeId) {
            return !adviceStack.isEmpty()
                    && adviceStack.peek().getInvokeId() == expectInvokeId
                    ? adviceStack.peek()
                    : null;
        }

    }

    // change internalClassName to javaClassName
    private String toJavaClassName(final String internalClassName) {
        if (StringUtil.isEmpty(internalClassName)) {
            return internalClassName;
        } else {
            return internalClassName.replace('/', '.');
        }
    }

    // 提取ClassLoader，从BeforeEvent中获取到的ClassLoader
    private ClassLoader toClassLoader(ClassLoader loader) {
        return null == loader
                // 如果此处为null，则说明遇到了来自Bootstrap的类，
                ? AdviceAdapterListener.class.getClassLoader()
                : loader;
    }

    /**
     * CALL目标对象
     */
    private static class CallTarget {

        final int callLineNum;
        final boolean isInterface;
        final String callJavaClassName;
        final String callJavaMethodName;
        final String callJavaMethodDesc;

        CallTarget(int callLineNum, boolean isInterface, String callJavaClassName, String callJavaMethodName, String callJavaMethodDesc) {
            this.callLineNum = callLineNum;
            this.isInterface = isInterface;
            this.callJavaClassName = callJavaClassName;
            this.callJavaMethodName = callJavaMethodName;
            this.callJavaMethodDesc = callJavaMethodDesc;
        }
    }

    /**
     * 根据提供的行为名称、行为描述从指定的Class中获取对应的行为
     *
     * @param clazz          指定的Class
     * @param javaMethodName 行为名称
     * @param javaMethodDesc 行为参数声明
     * @return 匹配的行为
     * @throws NoSuchMethodException 如果匹配不到行为，则抛出该异常
     */
    private Behavior toBehavior(final Class<?> clazz,
                                final String javaMethodName,
                                final String javaMethodDesc) throws NoSuchMethodException {
        if ("<init>".equals(javaMethodName)) {
            return new Constructor(clazz, javaMethodDesc);
        } else {
            return new Method(clazz, javaMethodName, javaMethodDesc);
        }
    }

}
