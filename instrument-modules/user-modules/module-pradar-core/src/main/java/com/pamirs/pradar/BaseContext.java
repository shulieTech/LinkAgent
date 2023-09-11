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
package com.pamirs.pradar;

import com.google.gson.JsonObject;
import com.pamirs.pradar.gson.GsonFactory;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * 基础上下文信息
 */
abstract class BaseContext {

    final String traceId;
    final String traceAppName;
    String remoteAppName = PradarCoreUtils.EMPTY_STRING;
    String upAppName = PradarCoreUtils.EMPTY_STRING;
    String traceServiceName = PradarCoreUtils.EMPTY_STRING;
    String traceMethod = PradarCoreUtils.EMPTY_STRING;
    /**
     * 调用 ID
     */
    final String invokeId;

    String traceName = PradarCoreUtils.EMPTY_STRING;

    String serviceName = PradarCoreUtils.EMPTY_STRING;
    String methodName = PradarCoreUtils.EMPTY_STRING;
    String callBackMsg = null;

    /**
     * mock内容
     */
    Object mockResponse;

    /**
     * 中间件名称
     */
    String middlewareName = null;

    /**
     * 请求内容
     */
    Object request = null;
    /**
     * 响应内容
     */
    Object response = null;
    /**
     * 是否是压测流量
     */
    boolean isClusterTest;

    /**
     * 是否是调试压测流量
     */
    boolean isDebug;

    /**
     * 下一次白名单呢校验是否直接通行
     */
    boolean passCheck;

    public Object getExt() {
        return ext;
    }

    public void setExt(Object ext) {
        this.ext = ext;
    }

    /**
     * 附加属性
     */
    Object ext;

    public Object getIndex() {
        return index;
    }

    public void setIndex(Object index) {
        this.index = index;
    }

    /**
     * 属性索引
     */
    Object index;

    long logTime = 0L;

    /**
     * 日志类型
     *
     * @see Pradar#LOG_TYPE_TRACE
     * @see Pradar#LOG_TYPE_INVOKE_CLIENT
     * @see Pradar#LOG_TYPE_INVOKE_SERVER
     */
    int logType = 0;

    /**
     * 调用类型
     *
     * @see MiddlewareType
     */
    int invokeType = 0;

    BaseContext(String _traceId, String _traceAppName, String _invokeId) {
        this.traceId = _traceId;
        this.invokeId = _invokeId;
        this.traceAppName = _traceAppName;
    }

    BaseContext(String _traceId, String _traceAppName, String _invokeId
            , String traceMethod, String traceServiceName) {
        this.traceId = _traceId;
        this.invokeId = _invokeId;
        this.traceAppName = _traceAppName;
        this.traceMethod = traceMethod;
        this.traceServiceName = traceServiceName;
    }

    // log control event ctx
    BaseContext(int logType) {
        this(PradarCoreUtils.EMPTY_STRING, PradarCoreUtils.EMPTY_STRING, PradarCoreUtils.EMPTY_STRING);
        this.logType = logType;
    }

    public String getMiddlewareName() {
        return middlewareName;
    }

    public void setMiddlewareName(String middlewareName) {
        this.middlewareName = middlewareName;
    }

    /**
     *
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     *
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     *
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     *
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     *
     */
    public long getLogTime() {
        return logTime;
    }

    /**
     *
     */
    public void setLogTime(long logTime) {
        this.logTime = logTime;
    }

    /**
     *
     */
    public int getInvokeType() {
        return invokeType;
    }

    /**
     *
     */
    public void setInvokeType(int invokeType) {
        this.invokeType = invokeType;
    }

    /**
     * 获取上下文的 TraceId
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * 获取上下文的 invokeId
     */
    public String getInvokeId() {
        return invokeId;
    }

    public String getTraceAppName() {
        return traceAppName;
    }

    /**
     * 获取附加信息
     */
    public String getCallBackMsg() {
        return callBackMsg;
    }

    public int getLogType() {
        return logType;
    }

    public void setLogType(int logType) {
        this.logType = logType;
    }

    /**
     * 设置附加信息
     */
    public void setCallBackMsg(String callBackMsg) {
        this.callBackMsg = callBackMsg;
    }

    public void setRemoteAppName(String remoteAppName) {
        this.remoteAppName = remoteAppName;
    }

    public boolean isClusterTest() {
        return isClusterTest;
    }

    public void setClusterTest(boolean clusterTest) {
        isClusterTest = clusterTest;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    public String getUpAppName() {
        return upAppName;
    }

    public Object getRequest() {
        return request;
    }

    public Object getMockResponse() {
        return mockResponse;
    }

    public void setMockResponse(String mockResponse) {
        this.mockResponse = mockResponse;
    }

    /**
     * 脱敏
     *
     * @param arg
     * @return
     */
    public Object encript(Object arg) {

        if (PradarSwitcher.securityFieldOpen()) {
            if (arg != null && String.class.isAssignableFrom(arg.getClass())) {
                String context = (String) arg;
                if (context.startsWith("{") && context.endsWith("}")
                        || (context.startsWith("[") && context.endsWith("]"))) {
                    try {
                        JsonObject jsonObject = GsonFactory.getGson().fromJson((String) arg, JsonObject.class);
                        List<String> fieldCollection = GlobalConfig.getInstance().getSimulatorDynamicConfig().getSecurityFieldCollection();
                        for (String field : fieldCollection) {
                            if (jsonObject.has(field)) {
                                jsonObject.addProperty(field, CRIPTTEXT);
                            }
                        }
                        return jsonObject.toString();
                    } catch (Throwable e) {
                        return arg;
                    }
                }
            }
        }
        return arg;
    }

    private static final String CRIPTTEXT = "******";

    public void setRequest(Object request) {
        if (GlobalConfig.getInstance().allowTraceRequestResponse()) {
            this.request = encript(request);
        }
    }


    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = encript(response);
    }

    /**
     * 兼容老版本探针没有传输上游应用名异常
     *
     * @param upAppName
     */
    public void setUpAppName(String upAppName) {
        if (StringUtils.isBlank(upAppName)) {
            upAppName = AppNameUtils.appName();
        }
        if (StringUtils.isBlank(upAppName)) {
            upAppName = PradarCoreUtils.DEFAULT_STRING;
        }
        this.upAppName = upAppName;
    }

    public abstract String getTraceNode();

    public abstract String getNodeId();

    public boolean isPassCheck() {
        //fixme,yuhan之前加的跳过白面单校验的逻辑，这块代码逻辑要整个干掉，先临时这么搞
        return passCheck;
    }

    public void setPassCheck(boolean passCheck) {
        this.passCheck = passCheck;
    }
}


