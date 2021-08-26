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
//package com.pamirs.attach.plugin.apache.kafka.stream.common;
//
//import com.pamirs.pradar.*;
//import com.pamirs.pradar.common.BytesUtils;
//import com.pamirs.pradar.common.HttpUtils;
//import com.pamirs.pradar.exception.PressureMeasureError;
//import com.pamirs.pradar.pressurement.ClusterTestUtils;
//import com.pamirs.pradar.pressurement.agent.shared.domain.DoorPlank;
//import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
//import com.pamirs.pradar.pressurement.agent.shared.service.impl.ArbiterEntrance;
//import com.pamirs.pradar.pressurement.base.util.PropertyUtil;
//import org.apache.commons.lang.StringUtils;
//import org.apache.kafka.common.header.Header;
//import org.apache.kafka.common.header.Headers;
//import org.apache.kafka.streams.processor.internals.ProcessorRecordContext;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Properties;
//
///**
// * @author angju TODO kafka消费入口调整待定
// * @date 2021/5/8 10:13
// */
//public class KafkaStreamRequestTracer{
//    private final static Logger LOGGER = LoggerFactory.getLogger(KafkaStreamRequestTracer.class);
//
//
//    private static final String FAST_DEBUG_TRACE_ID_UPLOAD = "/api/fast/debug/trace/id/push";
//
//
//    /**
//     * 开始调用链，注意，开始之后，不管后续处理是否正常，都需要调用。
//     */
//    public final void startTrace(Properties properties, String topic, String pluginName, ProcessorRecordContext processorRecordContext) {
//
//
//        String ip = null;
//
//        String traceId = getTraceId(processorRecordContext.headers());
//        boolean isTraceIdBlank = false;
//        if (StringUtils.isBlank(traceId)) {
//            traceId = TraceIdGenerator.generate(ip);
//            isTraceIdBlank = true;
//        }
//
//
//        boolean isClusterTestRequest = isClusterTestRequest(processorRecordContext);
//        boolean isDebug = isDebugRequest(processorRecordContext.headers());
//
//
//        ClusterTestUtils.validateClusterTest(isClusterTestRequest);
//
//        if (isDebug && isTraceIdBlank) {
//            pushTraceId(traceId, processorRecordContext.headers());
//        }
//
//        if (isClusterTestRequest) {
//            final DoorPlank arbiterDp = ArbiterEntrance.shallWePassHttp();
//            if (Pradar.DOOR_CLOSED.equals(arbiterDp.getStatus())) {
//                ErrorReporter.buildError()
//                        .setErrorType(ErrorTypeEnum.AgentError)
//                        .setErrorCode("agent-0008")
//                        .setMessage("压测开关关闭")
//                        .setDetail("data receiver filter is close!")
//                        .printFastDebugLog();
//                throw new PressureMeasureError("data receiver filter is close! " + arbiterDp, isClusterTestRequest);
//            }
//        }
//
//        if (!isTraceIdBlank) {
//            Map<String, String> context = new HashMap<String, String>();
//            for (String key : Pradar.getInvokeContextTransformKeys()) {
//                String value = getProperty(request, key);
//                if (value != null) {
//                    context.put(key, value);
//                }
//            }
//
//            context.put(PradarService.PRADAR_TRACE_ID_KEY, traceId);
//            context.put(PradarService.PRADAR_CLUSTER_TEST_KEY, String.valueOf(isClusterTestRequest));
//            if (LOGGER.isDebugEnabled()) {
//                LOGGER.debug("accept web server rpcServerRecv context:{}, currentContext:{} currentMiddleware:{}", context, Pradar.getInvokeContextMap(), Pradar.getMiddlewareName());
//            }
//            Pradar.startServerInvoke(url, StringUtils.upperCase(getMethod(request)), context);
//        } else {
//            Pradar.clearInvokeContext();
//            Pradar.setDebug(isDebug);
//            Pradar.startTrace(traceId, url, StringUtils.upperCase(getMethod(request)));
//            Pradar.setClusterTest(isClusterTestRequest);
//            Pradar.setDebug(isDebug);
//        }
//
//        final long contentLength = getContentLength(request);
//        Pradar.requestSize(contentLength < 0 ? 0 : contentLength);
//        String remoteIp = Pradar.getRemoteIp();
//
//            Pradar.remoteIp(remoteIp);
//        }
//        String port = getRemotePort(request);
//        if (port != null) {
//            Pradar.remotePort(port);
//        }
//        Pradar.middlewareName(pluginName);
//    }
//
//    /**
//     * 推送TraceId到控制台
//     *
//     * @param traceId traceId
//     * @param headers 请求对象
//     */
//    protected void pushTraceId(String traceId, Headers headers) {
//        String fastDebugId = getFastDebugId(headers);
//        if (traceId == null || fastDebugId == null) {
//            return;
//        }
//        String body = traceId.concat(",").concat(fastDebugId);
//        HttpUtils.HttpResult httpResult = HttpUtils.doPost(PropertyUtil.getTroControlWebUrl() + FAST_DEBUG_TRACE_ID_UPLOAD, body);
//        LOGGER.info("fast debug push trace id:{}, response status:{}", traceId, httpResult.getStatus());
//    }
//
//    /**
//     * 设置 响应头部
//     *
//     * @param resp
//     * @param key
//     * @param value
//     */
//    public abstract void setResponseHeader(RESP resp, String key, Object value);
//
//    /**
//     * 结束 trace
//     *
//     * @param request    请求
//     * @param response   响应
//     * @param throwable  异常
//     * @param resultCode 结果编码
//     */
//    public final void endTrace(REQ request, RESP response, Throwable throwable, String resultCode) {
//        if (isNeedFilter(request)) {
//            return;
//        }
//
//        if (Pradar.isDebug()) {
//            setResponseHeader(response, "traceId", Pradar.getTraceId());
//        }
//
//        Object value = getAttribute(request, "isTrace");
//        boolean isTrace = true;
//        if (value != null && (value instanceof Boolean)) {
//            isTrace = (Boolean) value;
//        }
//
//        /**
//         * 这个放在后面是防止我们获取参数时使用默认编码导致在业务中使用自定义编码时会出现乱码
//         * 但是这可能会导致我们获取到的参数是乱码
//         */
//        if (Pradar.isRequestOn()) {
//            String params = getParams(request);
//            Pradar.request(params);
//        }
//
//        if (Pradar.isResponseOn()) {
//            String result = getResponse(response);
//            if (throwable != null) {
//                Pradar.response(throwable);
//            } else {
//                Pradar.response(result);
//            }
//        }
//
//        if (isTrace) {
//            if (throwable != null) {
//                Pradar.endTrace(resultCode, MiddlewareType.TYPE_WEB_SERVER);
//            } else {
//                Pradar.endTrace(resultCode, MiddlewareType.TYPE_WEB_SERVER);
//            }
//        } else {
//            if (throwable == null) {
//                Pradar.endServerInvoke(resultCode, MiddlewareType.TYPE_WEB_SERVER);
//            } else {
//                Pradar.endServerInvoke(resultCode, MiddlewareType.TYPE_WEB_SERVER);
//            }
//        }
//    }
//
//    /**
//     * 结束调用链。 注意：需要假定 response 已经提交，因此只能做只读操作，不能修改
//     */
//    public final void endTrace(REQ request, RESP response, Throwable throwable) {
//        try {
//            String resultCode = getStatusCode(response, throwable);
//            endTrace(request, response, throwable, resultCode);
//        } finally {
//            Pradar.clearInvokeContext();
//        }
//    }
//
//    /**
//     * 获取 TraceId。根据以下步骤： <ol> <li>从 ThreadLocal 获取</li> <li>从 URL 参数中 获取</li> <li>从 header 中
//     * 获取</li> <li>如果上述都没有，则自动生成，如果 ip 不为 <code>null</code>，则基于指定的 ip， 否则，使用本机 ip </li> </ol>
//     */
//    public final String getTraceId(Headers recordHeaders) {
//        // 检查 header 中的调用链配置
//        if (!PradarSwitcher.isKafkaMessageHeadersEnabled()){
//            return null;
//        }
//        Header header = recordHeaders.lastHeader(PradarService.PRADAR_TRACE_ID_KEY)
//        if (header == null){
//            return null;
//        }
//        String traceId = BytesUtils.toString(header.value());
//        if (!StringUtils.isEmpty(traceId) && traceId.length() < 64) {
//            return traceId;
//        }
//        return null;
//    }
//
//
//    /**
//     * 判断是否是压测流量
//     *
//     * @param processorRecordContext
//     * @return
//     */
//    public boolean isClusterTestRequest(ProcessorRecordContext processorRecordContext) {
//
//        boolean isClusterTest = Pradar.isClusterTestPrefix(processorRecordContext.topic());
//        if (isClusterTest){
//            return true;
//        }
//        if (PradarSwitcher.isKafkaMessageHeadersEnabled()) {
//            Headers headers = processorRecordContext.headers();
//            Header header = headers.lastHeader(PradarService.PRADAR_CLUSTER_TEST_KEY);
//            if (header != null) {
//                isClusterTest = isClusterTest || ClusterTestUtils.isClusterTestRequest(BytesUtils.toString(header.value()));
//            }
//        }
//        return isClusterTest;
//    }
//
//    /**
//     * 判断是否是压测流量(调试流量)
//     *
//     * @param headers
//     * @return
//     */
//    public boolean isDebugRequest(Headers headers) {
//        if (PradarSwitcher.isKafkaMessageHeadersEnabled()) {
//            Header header = headers.lastHeader(PradarService.PRADAR_CLUSTER_TEST_KEY);
//            if (header != null) {
//                return ClusterTestUtils.isClusterTestRequest(BytesUtils.toString(header.value()));
//            }
//        }
//        return false;
//    }
//
//    /**
//     * 获取debugId
//     */
//    public final String getFastDebugId(Headers headers) {
//        if (PradarSwitcher.isKafkaMessageHeadersEnabled()) {
//            LOGGER.warn("config is.kafka.message.headers is false, can not debug model" );
//            Header header = headers.lastHeader(PradarService.PRADAR_FAST_DEBUG_ID);
//            if (header != null) {
//                return BytesUtils.toString(header.value());
//            }
//        }
//        return null;
//    }
//}
//
