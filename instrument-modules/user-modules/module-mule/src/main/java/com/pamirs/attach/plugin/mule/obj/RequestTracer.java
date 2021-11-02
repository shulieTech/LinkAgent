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
package com.pamirs.attach.plugin.mule.obj;

import com.pamirs.attach.plugin.mule.obj.resource.StaticFileFilter;
import com.pamirs.pradar.*;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import org.apache.commons.lang.StringUtils;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Create by xuyh at 2020/6/19 11:54.
 */
public class RequestTracer {
    private final static Logger LOGGER = LoggerFactory.getLogger(RequestTracer.class.getName());
    public static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP", // 优先获取其他代理设置的真实用户ip
            "X-Real-IP",          // enginx 设置 remoteIP，如果没有拿到 NS-Client-IP，那么这就是真实的用户 ip
            "NS-Client-IP",       // NAT 方式设置的ip
    };

    /**
     * 用于入口分类的 UserData key
     */
    public static final String PRADAR_URL_CLASSIFIER_KEY = "i";

    /**
     * 用于跟踪来源应用的 UserData key
     */
    public static final String PRADAR_ROOT_CLASSIFIER_KEY = "r";

    /**
     * 用于跟踪来源IP的 UserData key
     */
    public static final String PRADAR_REMOTE_CLASSIFIER_KEY = "re";

    private static boolean isNeedFilter(HttpRequestPacket request) {
        String checkTest = request.getRequestURI();
        return StaticFileFilter.needFilter(checkTest) || request.getAttribute(StaticFileFilter.PRADAR_FILTER) != null;
    }

    public static void doBeforeTrace(HttpRequestPacket request) {
        try {
            if (PradarSwitcher.isTraceEnabled()) {
                if (isNeedFilter(request)) {
                    request.setAttribute("Pradar-Set", false);
                    return;
                }
                request.setAttribute("Pradar-Set", true);
                request.setAttribute(StaticFileFilter.PRADAR_FILTER, true);

                String ip = null;
                if (!StaticFileFilter.USE_LOCAL_IP) {
                    ip = getRemoteAddress(request);
                }
                String traceId = getTraceId(request);
                startTrace(traceId, request, ip);
            }
        } catch (Throwable e) {
            //do nothing
        }
    }

    public static void doAfterTrace(HttpRequestPacket request, HttpResponsePacket response, Throwable throwable) {
        try {
            if (request == null) {
                return;
            }
            if (isNeedFilter(request) && !getPradarSet(request)) {
                return;
            }
            endTrace(request, response, throwable);
        } catch (Throwable e) {
            LOGGER.error("", e);
        }
    }

    private static boolean getPradarSet(HttpRequestPacket request) {
        Object value = request.getAttribute("Pradar-Set");
        if (value != null && (value instanceof Boolean)) {
            return (Boolean) value;
        }
        return false;
    }

    /**
     * 获取 TraceId。根据以下步骤： <ol> <li>从 ThreadLocal 获取</li> <li>从 URL 参数中 获取</li> <li>从 header 中
     * 获取</li> <li>如果上述都没有，则自动生成，如果 ip 不为 <code>null</code>，则基于指定的 ip， 否则，使用本机 ip </li> </ol>
     */
    public static final String getTraceId(HttpRequestPacket request) {
        String traceId = getThreadLocalTraceId();
        if (request == null) {
            return null;
        }
        if (!PradarCoreUtils.isBlank(traceId)) {
            return traceId;
        }
        // 检查 header 中的调用链配置
        traceId = PradarCoreUtils.trim(request.getHeader(PradarService.PRADAR_TRACE_ID_KEY));
        if (traceId != null && traceId.length() < 64) {
            return traceId;
        }
        return traceId;
    }

    /**
     * 获取 TraceId，如果 ThreadLocal 没有，返回 <code>null</code>。
     */
    public static final String getThreadLocalTraceId() {
        return Pradar.getTraceId();
    }

    /**
     * 判断是否是压测流量
     *
     * @param servletRequest
     * @return
     */
    public static boolean isClusterTestRequest(HttpRequestPacket servletRequest) {
        String value = getProperty(servletRequest, PradarService.PRADAR_CLUSTER_TEST_KEY);
        if (StringUtils.isBlank(value)) {
            value = getProperty(servletRequest, PradarService.PRADAR_HTTP_CLUSTER_TEST_KEY);
        }
        return ClusterTestUtils.isClusterTestRequest(value);
    }

    /**
     * 判断是否是压测流量
     *
     * @param request
     * @return
     */
    public static boolean isDebugRequest(HttpRequestPacket request) {
        String value = getProperty(request, PradarService.PRADAR_DEBUG_KEY);
        return ClusterTestUtils.isClusterTestRequest(value);
    }

    /**
     * 开始调用链，注意，开始之后，不管后续处理是否正常，都需要调用。
     */
    public static final void startTrace(String traceId,
                                        HttpRequestPacket request,
                                        String ip) {
        if (request == null) {
            return;
        }
        String url = request.getRequestURI();

        boolean isClusterTestRequest = isClusterTestRequest(request);
        boolean isDebug = isDebugRequest(request);

        Map<String, String> context = null;
        if (StringUtils.isNotBlank(traceId)) {
            context = new HashMap<String, String>();
            List<String> keys = Pradar.getInvokeContextTransformKeys();
            for (String key : keys) {
                context.put(key, getProperty(request, key));
            }
            context.put(PradarService.PRADAR_CLUSTER_TEST_KEY, String.valueOf(isClusterTestRequest));
            Pradar.startServerInvoke(request.getRequestURI(), "http", context);
            request.setAttribute("isTrace", false);
        } else {
            Pradar.clearInvokeContext();
            traceId = TraceIdGenerator.generate(ip, isClusterTestRequest);
            Pradar.startTrace(traceId, url, StringUtils.upperCase(request.getMethod().getMethodString()));
            request.setAttribute("isTrace", true);
        }
        InvokeContext invokeContext = Pradar.getInvokeContext();
        if (invokeContext == null) {
            return;
        }

        invokeContext.setClusterTest(isClusterTestRequest);
        invokeContext.setDebug(isDebug);


        invokeContext.setRequestSize(request.getContentLength() < 0 ? 0 : request.getContentLength());
        String remoteIp = getRemoteAddress(request);
        if (StringUtils.isNotBlank(remoteIp)) {
            invokeContext.setRemoteIp(remoteIp);
        }
        invokeContext.setPort(String.valueOf(request.getServerPort()));

        invokeContext.setMiddlewareName("mule");
    }

    public static String getResult(HttpResponsePacket response) {
        if (response == null) {
            return "500";
        }
        return String.valueOf(response.getStatus());
    }

    /**
     * 结束调用链。 注意：需要假定 response 已经提交，因此只能做只读操作，不能修改
     */
    public static final void endTrace(HttpRequestPacket request, HttpResponsePacket response, Throwable throwable) {
        String resultCode = getResultCode(response, throwable);
        endTrace(request, response, throwable, resultCode);
    }

    private static String getResultCode(HttpResponsePacket response, Throwable throwable) {
        if (throwable != null) {
            return "500";
        }
        if (response == null) {
            return "200";
        }
        return String.valueOf(response.getStatus());
    }

    /**
     * 结束调用链。 注意：需要假定 response 已经提交，因此只能做只读操作，不能修改
     */
    public static final void endTrace(HttpRequestPacket request, HttpResponsePacket response, Throwable throwable, String resultCode) {
        Object value = request.getAttribute("isTrace");
        boolean isTrace = true;
        if (value != null && (value instanceof Boolean)) {
            isTrace = (Boolean) value;
        }

        String params = getParams(request);
        String result = getResult(response);
        InvokeContext invokeContext = Pradar.getInvokeContext();
        if (invokeContext == null) {
            return;
        }

        invokeContext.setRequest(params);
        invokeContext.setRequestSize(request.getContentLength());
        if (throwable != null) {
            invokeContext.setResponse(throwable);
        } else {
            invokeContext.setResponse(result);
        }

        if (isTrace) {
            if (throwable != null) {
                Pradar.endTrace(resultCode, MiddlewareType.TYPE_WEB_SERVER);
            } else {
                Pradar.endTrace(resultCode, MiddlewareType.TYPE_WEB_SERVER);
            }
        } else {
            if (throwable == null) {
                Pradar.endServerInvoke(resultCode, MiddlewareType.TYPE_WEB_SERVER);
            } else {
                Pradar.endServerInvoke(resultCode, MiddlewareType.TYPE_WEB_SERVER);
            }
        }
    }


    /**
     * 获取远程客户端的 IP
     */
    public static final String getRemoteAddress(HttpRequestPacket request) {
        String ip = null;
        boolean valid = false;


        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            valid = checkIP(ip);
            if (valid) {
                break;
            }
        }
        if (PradarCoreUtils.isBlank(ip)) {
            ip = request.getRemoteAddress();
        }
        // 代理时会有逗号分隔的 ip，获取第一个即可
        int index = ip.indexOf(',');
        if (index != -1) {
            String firstIp = ip.substring(0, index).trim();
            if (checkIP(ip)) {
                ip = firstIp;
            }
        }
        return ip;
    }

    private static final boolean checkIP(String ip) {
        if (ip == null || ip.length() == 0 || "unknown".equals(ip) || "UNKNOWN".equals(ip)) {
            return false;
        } else {
            return true;
        }
    }

    private static String getProperty(HttpRequestPacket httpRequest, String... keys) {

        for (String key : keys) {
            String value = PradarCoreUtils.trim(httpRequest.getHeader(key));
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }

        return null;
    }

    public static String getParams(HttpRequestPacket request) {
        String queryStr = request.getQueryString();
        Map<String, String> paramMap = new HashMap<String, String>();
        if (queryStr != null && !queryStr.isEmpty()) {
            String[] queryPairs = StringUtils.split(queryStr, '&');
            if (queryPairs != null && queryPairs.length != 0) {
                for (String queryPair : queryPairs) {
                    String[] pair = StringUtils.split(queryPair, '=');
                    if (pair != null && pair.length == 2) {
                        paramMap.put(pair[0], pair[1]);
                    }
                }
            }
        }

        StringBuilder stringBuilder = new StringBuilder(128).append("parameterString={");
        if (null != paramMap) {
            int index = 0;
            for (Object key : paramMap.keySet()) {
                stringBuilder.append(key);
                stringBuilder.append(":");
                stringBuilder.append(paramMap.get(key));
                if (index++ < (paramMap.keySet().size() - 1)) {
                    stringBuilder.append(",");
                }
            }
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    /**
     * 从 URL 解析出 URI，如果解析不到 URI，就用域名
     */
    static String getUriFromUrl(String url) {
        int start;
        final int len = url.length();
        if (len <= 7) {
            return url;
        }
        if (url.startsWith("http://")) {
            start = 7;
        } else if ((start = url.indexOf("://")) != -1) {
            start += 3;
        } else {
            start = 0;
        }

        // 去掉末尾的 ‘/’
        final int end = (url.charAt(len - 1) == '/') ? (len - 1) : len;
        final int istart = url.indexOf('/', start);
        if (istart >= 0 && istart < end) {
            return url.substring(istart, end);
        }
        return url.substring(start, end);
    }

}
