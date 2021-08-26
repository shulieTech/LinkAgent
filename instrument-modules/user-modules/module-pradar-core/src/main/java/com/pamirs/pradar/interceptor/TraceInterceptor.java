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
package com.pamirs.pradar.interceptor;

import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.TraceIdGenerator;
import com.pamirs.pradar.exception.PradarException;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Map;

import static com.pamirs.pradar.interceptor.TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS;

/**
 * 实例方法埋点的环绕拦截器抽象实现,可实现追踪埋点与压测增强的混合逻辑
 * 适用于方法开始进行 trace 追踪，方法结果进行 trace 提交的场景
 * <p>
 * 例如:
 * 需要追踪的方法为 void test(String value) {
 * System.out.println("xxxxx");
 * }
 * <p>
 * 需要追踪 test 方法的执行耗时、入参、出参等,其他场景并不适用
 *
 * <pre>
 *     实现的这些方法不支持重载,否则会抛出异常RuntimeException
 * </pre>
 * Created by xiaobin on 2017/2/6.
 */
abstract class TraceInterceptor extends BaseInterceptor {
    protected final static Logger LOGGER = LoggerFactory.getLogger(TraceInterceptor.class);

    @Resource
    protected SimulatorConfig simulatorConfig;

    /**
     * 是否是调用端
     *
     * @return
     */
    protected boolean isClient(Advice advice) {
        return true;
    }

    /**
     * 是否是入口
     *
     * @return
     */
    protected boolean isTrace(Advice advice) {
        return false;
    }

    /**
     * 是否是trace 入口
     *
     * @param advice 切点
     * @return
     */
    private boolean isTrace0(Advice advice) {
        try {
            return isTrace(advice);
        } catch (Throwable e) {
            LOGGER.error("TraceAroundInterceptor: {} isTrace throw a exception, return default result[isTrace=false] instead.", getClass().getName(), e);
            return false;
        }
    }

    /**
     * pradar的Plugin名称，与{@link #getPluginType()} 对应,对应名称
     */
    public abstract String getPluginName();

    /**
     * pradar的Plugin类型,参见 {@link com.pamirs.pradar.MiddlewareType#TYPE_CACHE} {@link
     * com.pamirs.pradar.MiddlewareType#TYPE_DB} {@link com.pamirs.pradar.MiddlewareType#TYPE_WEB_SERVER} {@link
     * com.pamirs.pradar.MiddlewareType#TYPE_LOCAL} {@link com.pamirs.pradar.MiddlewareType#TYPE_FS} {@link
     * com.pamirs.pradar.MiddlewareType#TYPE_SEARCH}
     */
    public abstract int getPluginType();

    /**
     * 前置埋点的前置操作
     * <p>
     * 如果该方法抛出异常不会阻塞流程，异常会以日志形式记录下来，后续流程会继续走下去
     */
    public abstract void beforeFirst(Advice advice) throws Exception;

    /**
     * 前置埋点的后置操作
     * <p>
     * 如果该方法抛出异常不会阻塞流程，异常会以日志形式记录下来，后续流程会继续走下去
     * 如果在 {@link #beforeTrace(Advice)} 抛出异常后该方法也会执行
     */
    public abstract void beforeLast(Advice advice) throws ProcessControlException;

    /**
     * 前置埋点
     *
     * @param advice 节点对象
     */
    public abstract SpanRecord beforeTrace(Advice advice);

    /**
     * 后置埋点的前置操作
     * <p>
     * 如果该方法抛出异常不会阻塞流程，异常会以日志形式记录下来，后续流程会继续走下去
     *
     * @param advice 节点对象
     */
    public abstract void afterFirst(Advice advice) throws ProcessControlException;

    /**
     * 后置埋点的后置操作
     * <p>
     * 如果该方法抛出异常不会阻塞流程，异常会以日志形式记录下来，后续流程会继续走下去
     * 如果在 {@link #afterTrace(Advice)} 抛出异常后该方法也会执行
     *
     * @param advice 节点对象
     */
    public abstract void afterLast(Advice advice);

    /**
     * 后置埋点
     *
     * @param advice 切点对象
     */
    public abstract SpanRecord afterTrace(Advice advice);

    /**
     * 异常时埋点的前置操作
     * <p>
     * 如果该方法抛出异常不会阻塞流程，异常会以日志形式记录下来，后续流程会继续走下去
     *
     * @param advice 切点对象
     */
    public abstract void exceptionFirst(Advice advice);

    /**
     * 异常时埋点的后置操作
     * <p>
     * 如果该方法抛出异常不会阻塞流程，异常会以日志形式记录下来，后续流程会继续走下去
     * 如果在 {@link #exceptionTrace(Advice)} 抛出异常后该方法也会执行
     *
     * @param advice 切点对象
     */
    public abstract void exceptionLast(Advice advice);

    /**
     * 异常时埋点
     *
     * @param advice 切点对象
     */
    public abstract SpanRecord exceptionTrace(Advice advice);

    /**
     * 获取传输 context 的转换器
     * 注意，一定需要保证该方法不能出现错误，不然可能就会导致上下文数据丢失，导致数据泄露问题
     * 该方法只有在 {@link #isClient(Advice) } 为 true 时才会被执行
     * 当该方法抛出异常时则不会中断流程，后续日志打印仍会进行，但是不会再进行调用链的传递
     *
     * @param advice 切点对象
     * @return
     */
    protected ContextTransfer getContextTransfer(Advice advice) {
        return null;
    }

    @Override
    public void doBefore(Advice advice) throws Throwable {
        if (!simulatorConfig.getBooleanProperty("plugin." + getPluginName() + ".trace.enabled", true)) {
            return;
        }
        ClusterTestUtils.validateClusterTest();
        Throwable throwable = null;
        try {
            beforeFirst(advice);
        } catch (PradarException e) {
            LOGGER.error("TraceInterceptor beforeFirst exec err:{}", this.getClass().getName(), e);
            throwable = e;
        } catch (PressureMeasureError e) {
            LOGGER.error("TraceInterceptor beforeFirst exec err:{}", this.getClass().getName(), e);
            throwable = e;
        } catch (Throwable t) {
            if (!(t instanceof ProcessControlException)) {
                LOGGER.error("TraceInterceptor beforeFirst exec err:{}", this.getClass().getName(), t);
            }
            throwable = t;
        }
        try {
            if (isClient(advice)) {
                startClientInvoke(advice);
            } else {
                startServerInvoke(advice);
            }
        } catch (PradarException e) {
            LOGGER.error("TraceInterceptor before exec err:{}", this.getClass().getName(), e);
            if (Pradar.isClusterTest()) {
                throw e;
            }
        } catch (PressureMeasureError e) {
            LOGGER.error("TraceInterceptor before exec err:{}", this.getClass().getName(), e);
            if (Pradar.isClusterTest()) {
                throw e;
            }
        } catch (Throwable e) {
            LOGGER.error("TraceInterceptor before exec err:{}", this.getClass().getName(), e);
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(e);
            }
        } finally {
            try {
                beforeLast(advice);
            } catch (PradarException e) {
                LOGGER.error("TraceInterceptor beforeLast exec err:{}", this.getClass().getName(), e);
                throwable = e;
            } catch (PressureMeasureError e) {
                LOGGER.error("TraceInterceptor beforeLast exec err:{}", this.getClass().getName(), e);
                throwable = e;
            } catch (Throwable t) {
                LOGGER.error("TraceInterceptor beforeLast exec err:{}", this.getClass().getName(), t);
                throwable = t;
            }
        }
        if (throwable != null) {
            if (advice.hasMark(BEFORE_TRACE_SUCCESS)) {
                try {
                    if (Pradar.isExceptionOn()) {
                        Pradar.response(throwable);
                    }
                    boolean isClient = true;
                    try {
                        isClient = isClient(advice);
                    } catch (Throwable e) {
                        LOGGER.error("Trace {} isClient execute error. use default value instead. {}", getClass().getName(), isClient, e);
                    }
                    if (isClient) {
                        Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_FAILED, getPluginType());
                    } else {
                        Pradar.endServerInvoke(ResultCode.INVOKE_RESULT_FAILED, getPluginType());
                    }
                } finally {
                    advice.unMark(BEFORE_TRACE_SUCCESS);
                }
            }else{
                LOGGER.error("trace throw exception, but not BEFORE_TRACE_SUCCESS in {}.beforeTrace(...). loss trace log!", getClass().getName(), throwable);
            }

            //压测流量抛出异常，  业务流量只做记录
            if (Pradar.isClusterTest()) {
                throw throwable;
            }
        }

    }


    /**
     * 开始服务端调用
     *
     * @param advice
     */
    private void startServerInvoke(Advice advice) {
        SpanRecord record = beforeTrace(advice);
        if (record == null) {
            return;
        }
        if (record.getContext() != null) {
            if (record.getContext() == SpanRecord.CLEAN_CONTEXT) {
                Pradar.clearInvokeContext();
            }
        }
        if (isTrace0(advice)) {
            String traceId = TraceIdGenerator.generate(record.getRemoteIp());
            Pradar.clearInvokeContext();
            Pradar.startTrace(traceId, record.getService(), record.getMethod());
        } else {
            Pradar.startServerInvoke(record.getService(), record.getMethod(), null, record.getContext());
        }

        InvokeContext invokeContext = Pradar.getInvokeContext();
        if (invokeContext == null) {
            return;
        }
        if (record.getClusterTest() != null) {
            invokeContext.setClusterTest(record.getClusterTest());
        }
        if (Pradar.isRequestOn()) {
            invokeContext.setRequest(record.getRequest());
        }
        advice.mark(BEFORE_TRACE_SUCCESS);
        if (record.getRequestSize() != 0) {
            invokeContext.setRequestSize(record.getRequestSize());
        }

        if (StringUtils.isNotBlank(record.getRemoteIp())) {
            invokeContext.setRemoteIp(record.getRemoteIp());
        }
        if (StringUtils.isNotBlank(record.getPort())) {
            invokeContext.setPort(record.getPort());
        }

        if (record.getCallbackMsg() != null) {
            invokeContext.setCallBackMsg(record.getCallbackMsg());
        }

        if (record.getMiddlewareName() == null) {
            invokeContext.setMiddlewareName(getPluginName());
        } else {
            invokeContext.setMiddlewareName(record.getMiddlewareName());
        }
    }

    /**
     * 开始客户端调用
     *
     * @param advice
     */
    private void startClientInvoke(Advice advice) {
        boolean traceEnabled = true;
        try {
            SpanRecord record = beforeTrace(advice);
            if (record == null) {
                traceEnabled = false;
                return;
            }

            Pradar.startClientInvoke(record.getService(), record.getMethod());
            InvokeContext invokeContext = Pradar.getInvokeContext();
            advice.mark(BEFORE_TRACE_SUCCESS);
            if (invokeContext == null) {
                return;
            }
            if (record.getRequestSize() != 0) {
                invokeContext.setRequestSize(record.getRequestSize());
            }
            if (Pradar.isRequestOn()) {
                invokeContext.setRequest(record.getRequest());
            }
            if (StringUtils.isNotBlank(record.getRemoteIp())) {
                invokeContext.setRemoteIp(record.getRemoteIp());
            }
            if (StringUtils.isNotBlank(record.getPort())) {
                invokeContext.setPort(record.getPort());
            }

            invokeContext.setPassCheck(record.isPassedCheck());

            if (record.getCallbackMsg() != null) {
                invokeContext.setCallBackMsg(record.getCallbackMsg());
            }

            if (record.getMiddlewareName() == null) {
                invokeContext.setMiddlewareName(getPluginName());
            } else {
                invokeContext.setMiddlewareName(record.getMiddlewareName());
            }
        } finally {
            if (traceEnabled) {
                try {
                    ContextTransfer contextTransfer = getContextTransfer(advice);
                    if (contextTransfer != null) {
                        Map<String, String> contextMap = Pradar.getInvokeContextTransformMap();
                        for (Map.Entry<String, String> entry : contextMap.entrySet()) {
                            contextTransfer.transfer(entry.getKey(), entry.getValue());
                        }
                    }
                } catch (Throwable e) {
                    LOGGER.error("AGENT: {} trace context transfer err, trace context may be lost.", getClass().getName(), e);
                }
            }
        }
    }

    /**
     * client invoke 结束
     *
     * @param advice
     * @throws Throwable
     */
    private void endClientInvoke(Advice advice) throws Throwable {
        try {
            if (!advice.hasMark(BEFORE_TRACE_SUCCESS)) {
                LOGGER.debug("{} before trace not finished.", getClass().getName());
                return;
            }
            SpanRecord record = afterTrace(advice);
            if (record == null) {
                //如果上下文开始了，但是这里没有，则需要强制结束
                Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_SUCCESS, getPluginType());
                return;
            }
            InvokeContext invokeContext = Pradar.getInvokeContext();
            if (invokeContext == null) {
                return;
            }
            if (record.getResponseSize() != 0) {
                invokeContext.setResponseSize(record.getResponseSize());
            }
            Object response = record.getResponse();
            if (response instanceof Throwable) {
                advice.attach(response);
            }
            if (Pradar.isResponseOn()) {
                invokeContext.setResponse(response);
            }
            if (StringUtils.isNotBlank(record.getRemoteIp())) {
                invokeContext.setRemoteIp(record.getRemoteIp());
            }
            if (StringUtils.isNotBlank(record.getPort())) {
                invokeContext.setPort(record.getPort());
            }

            if (record.getMiddlewareName() != null) {
                invokeContext.setMiddlewareName(record.getMiddlewareName());
            }

            if (record.getCallbackMsg() != null) {
                invokeContext.setCallBackMsg(record.getCallbackMsg());
            }

            Pradar.endClientInvoke(record.getResultCode(), getPluginType());
        } catch (Throwable e) {
            /**
             * 如果出错了，则强制将上下文提交
             */
            Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_UNKNOWN, getPluginType());
            throw e;
        } finally {
            advice.unMark(BEFORE_TRACE_SUCCESS);
        }
    }

    /**
     * 结束服务端调用
     *
     * @param advice
     * @throws Throwable
     */
    private void endServerInvoke(Advice advice) throws Throwable {
        if (!advice.hasMark(BEFORE_TRACE_SUCCESS)) {
            LOGGER.debug("{} before trace not finished.", getClass().getName());
            return;
        }
        boolean isTrace = isTrace0(advice);
        try {
            SpanRecord record = afterTrace(advice);
            if (record == null) {
                if (!Pradar.hasInvokeContext() || Pradar.getInvokeContext().isEmpty()) {
                    return;
                }
                //如果上下文开始了，但是这里没有，则需要强制结束
                if (isTrace) {
                    Pradar.endTrace(ResultCode.INVOKE_RESULT_UNKNOWN, getPluginType());
                } else {
                    Pradar.endServerInvoke(ResultCode.INVOKE_RESULT_UNKNOWN, getPluginType());
                }
                return;
            }
            InvokeContext invokeContext = Pradar.getInvokeContext();
            if (invokeContext == null) {
                return;
            }
            if (record.getResponseSize() != 0) {
                invokeContext.setResponseSize(record.getResponseSize());
            }
            Object response = record.getResponse();
            if (response instanceof Throwable) {
                advice.attach(response);
            }
            if (Pradar.isResponseOn()) {
                invokeContext.setResponse(response);
            }
            if (StringUtils.isNotBlank(record.getRemoteIp())) {
                invokeContext.setRemoteIp(record.getRemoteIp());
            }
            if (StringUtils.isNotBlank(record.getPort())) {
                invokeContext.setPort(record.getPort());
            }
            if (record.getMiddlewareName() != null) {
                invokeContext.setMiddlewareName(record.getMiddlewareName());
            }

            if (record.getCallbackMsg() != null) {
                invokeContext.setCallBackMsg(record.getCallbackMsg());
            }

            if (isTrace) {
                Pradar.endTrace(record.getResultCode(), getPluginType());
            } else {
                Pradar.endServerInvoke(record.getResultCode(), getPluginType());
            }
        } catch (Throwable e) {
            if (isTrace) {
                Pradar.endTrace(ResultCode.INVOKE_RESULT_UNKNOWN, getPluginType());
            } else {
                Pradar.endServerInvoke(ResultCode.INVOKE_RESULT_UNKNOWN, getPluginType());
            }
            throw e;
        } finally {
            advice.unMark(BEFORE_TRACE_SUCCESS);
        }
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        if (!simulatorConfig.getBooleanProperty("plugin." + getPluginName() + ".trace.enabled", true)) {
            return;
        }
        ClusterTestUtils.validateClusterTest();
        Throwable throwable = null;
        try {
            afterFirst(advice);
        } catch (PradarException e) {
            LOGGER.error("TraceInterceptor afterFirst exec err:{}", this.getClass().getName(), e);
            throwable = e;
        } catch (PressureMeasureError e) {
            LOGGER.error("TraceInterceptor afterFirst exec err:{}", this.getClass().getName(), e);
            throwable = e;
        } catch (Throwable t) {
            LOGGER.error("TraceInterceptor afterFirst exec err:{}", this.getClass().getName(), t);
            throwable = t;
        }
        try {
            if (isClient(advice)) {
                endClientInvoke(advice);
            } else {
                endServerInvoke(advice);
            }
        } catch (PradarException e) {
            LOGGER.error("TraceInterceptor after exec err:{}", this.getClass().getName(), e);
            if (Pradar.isClusterTest()) {
                throw e;
            }
        } catch (PressureMeasureError e) {
            LOGGER.error("TraceInterceptor after exec err:{}", this.getClass().getName(), e);
            if (Pradar.isClusterTest()) {
                throw e;
            }
        } catch (Throwable e) {
            if (Pradar.isClusterTest()) {
                LOGGER.error("TraceInterceptor after exec err:{}", this.getClass().getName(), e);
                throw new PressureMeasureError(e);
            }
        } finally {
            try {
                afterLast(advice);
            } catch (PradarException e) {
                LOGGER.error("TraceInterceptor afterLast exec err:{}", this.getClass().getName(), e);
                throwable = e;
            } catch (PressureMeasureError e) {
                LOGGER.error("TraceInterceptor afterLast exec err:{}", this.getClass().getName(), e);
                throwable = e;
            } catch (Throwable t) {
                LOGGER.error("TraceInterceptor afterLast exec err:{}", this.getClass().getName(), t);
                throwable = t;
            }
        }
        if (throwable != null && Pradar.isClusterTest()) {
            throw throwable;
        }
    }

    @Override
    public final void doException(Advice advice) throws Throwable {
        if (!simulatorConfig.getBooleanProperty("plugin." + getPluginName() + ".trace.enabled", true)) {
            return;
        }
        Throwable throwable = null;
        try {
            exceptionFirst(advice);
        } catch (PradarException e) {
            LOGGER.error("TraceInterceptor exceptionFirst exec err:{}", this.getClass().getName(), e);
            throwable = e;
        } catch (PressureMeasureError e) {
            LOGGER.error("TraceInterceptor exceptionFirst exec err:{}", this.getClass().getName(), e);
            throwable = e;
        } catch (Throwable t) {
            LOGGER.error("TraceInterceptor exceptionFirst exec err:{}", this.getClass().getName(), t);
            throwable = t;
        }
        try {
            if (isClient(advice)) {
                endClientInvokeException(advice);
            } else {
                endServerInvokeException(advice);
            }
        } catch (PradarException e) {
            LOGGER.error("TraceInterceptor exception exec err:{}", this.getClass().getName(), e);
            if (Pradar.isClusterTest()) {
                throw e;
            }
        } catch (PressureMeasureError e) {
            LOGGER.error("TraceInterceptor exception exec err:{}", this.getClass().getName(), e);
            if (Pradar.isClusterTest()) {
                throw e;
            }
        } catch (Throwable e) {
            if (Pradar.isClusterTest()) {
                LOGGER.error("TraceInterceptor exception exec err:{}", this.getClass().getName(), e);
                throw new PressureMeasureError(e);
            }
        } finally {
            try {
                exceptionLast(advice);
            } catch (PradarException e) {
                LOGGER.error("TraceInterceptor exceptionLast exec err:{}", this.getClass().getName(), e);
                throwable = e;
            } catch (PressureMeasureError e) {
                LOGGER.error("TraceInterceptor exceptionLast exec err:{}", this.getClass().getName(), e);
                throwable = e;
            } catch (Throwable t) {
                LOGGER.error("TraceInterceptor exceptionLast exec err:{}", this.getClass().getName(), t);
                throwable = t;
            }
        }
        if (throwable != null && Pradar.isClusterTest()) {
            throw throwable;
        }
    }

    /**
     * 结束客户端异常调用
     *
     * @param advice
     * @throws Throwable
     */
    private final void endClientInvokeException(Advice advice) throws Throwable {
        try {
            if (!advice.hasMark(BEFORE_TRACE_SUCCESS)) {
                LOGGER.debug("{} before trace not finished.", getClass().getName());
                return;
            }
            SpanRecord record = exceptionTrace(advice);
            if (record == null) {
                Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_SUCCESS, getPluginType());
                return;
            }
            InvokeContext invokeContext = Pradar.getInvokeContext();
            if (invokeContext == null) {
                return;
            }
            Object response = record.getResponse();
            if (response != null && response instanceof Throwable) {
                advice.attach(response);
            }
            if (Pradar.isResponseOn()) {
                invokeContext.setResponse(response);
            }
            if (StringUtils.isNotBlank(record.getRemoteIp())) {
                invokeContext.setRemoteIp(record.getRemoteIp());
            }

            if (StringUtils.isNotBlank(record.getPort())) {
                invokeContext.setPort(record.getPort());
            }

            if (record.getMiddlewareName() != null) {
                invokeContext.setMiddlewareName(record.getMiddlewareName());
            }
            if (record.getCallbackMsg() != null) {
                invokeContext.setCallBackMsg(record.getCallbackMsg());
            }
            Pradar.endClientInvoke(record.getResultCode(), getPluginType());
        } catch (Throwable e) {
            Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_UNKNOWN, getPluginType());
            throw e;
        } finally {
            advice.unMark(BEFORE_TRACE_SUCCESS);
        }
    }

    private final void endServerInvokeException(Advice advice) throws Throwable {
        if (!advice.hasMark(BEFORE_TRACE_SUCCESS)) {
            LOGGER.debug("{} before trace not finished.", getClass().getName());
            return;
        }
        boolean isTrace = isTrace0(advice);
        try {
            SpanRecord record = exceptionTrace(advice);
            if (record == null) {
                if (isTrace) {
                    Pradar.endTrace(ResultCode.INVOKE_RESULT_SUCCESS, getPluginType());
                } else {
                    Pradar.endServerInvoke(ResultCode.INVOKE_RESULT_SUCCESS, getPluginType());
                }
                return;
            }
            InvokeContext invokeContext = Pradar.getInvokeContext();
            if (invokeContext == null) {
                return;
            }
            Object response = record.getResponse();
            if (response != null && response instanceof Throwable) {
                advice.attach(response);
            }

            if (Pradar.isExceptionOn()) {
                invokeContext.setResponse(response);
            }
            if (StringUtils.isNotBlank(record.getRemoteIp())) {
                invokeContext.setRemoteIp(record.getRemoteIp());
            }

            if (StringUtils.isNotBlank(record.getPort())) {
                invokeContext.setPort(record.getPort());
            }

            if (record.getMiddlewareName() != null) {
                invokeContext.setMiddlewareName(record.getMiddlewareName());
            }
            if (record.getCallbackMsg() != null) {
                invokeContext.setCallBackMsg(record.getCallbackMsg());
            }
            if (isTrace) {
                Pradar.endTrace(record.getResultCode(), getPluginType());
            } else {
                Pradar.endServerInvoke(record.getResultCode(), getPluginType());
            }
        } catch (Throwable e) {
            if (isTrace) {
                Pradar.endTrace(ResultCode.INVOKE_RESULT_UNKNOWN, getPluginType());
            } else {
                Pradar.endServerInvoke(ResultCode.INVOKE_RESULT_UNKNOWN, getPluginType());
            }
            throw e;
        } finally {
            advice.unMark(BEFORE_TRACE_SUCCESS);
        }
    }

}

