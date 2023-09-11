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
package com.pamirs.attach.plugin.httpclient.interceptor;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.httpclient.HttpClientConstants;
import com.pamirs.attach.plugin.httpclient.utils.BlackHostChecker;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.common.HeaderMark;
import com.pamirs.pradar.gson.GsonFactory;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ExecutionCall;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by baozi on 2021/8/12.
 */
public class HttpClientv5MethodInterceptor1 extends TraceInterceptorAdaptor {
    private final Logger logger = LoggerFactory.getLogger(HttpClientv5MethodInterceptor1.class);

    @Override
    public String getPluginName() {
        return HttpClientConstants.PLUGIN_NAME;

    }

    @Override
    public int getPluginType() {
        return HttpClientConstants.PLUGIN_TYPE;
    }

    private static String getService(String schema, String host, int port, String path) {
        String url = schema + "://" + host;
        if (port != -1 && port != 80) {
            url = url + ':' + port;
        }
        return url + path;
    }

    @Override
    public void beforeFirst(Advice advice) throws Exception {
        Object[] args = advice.getParameterArray();
        //ClassicHttpRequest
        final ClassicHttpRequest request = (ClassicHttpRequest)args[0];
        String url = getUrl(request);
        if (url == null) {return;}
        advice.attach(url);
        if (BlackHostChecker.isBlackHost(url)) {
            advice.mark(BlackHostChecker.BLACK_HOST_MARK);
        }
    }

    @Override
    public void beforeLast(Advice advice) throws ProcessControlException {
        if (!ClusterTestUtils.enableMock()) {
            return;
        }
        Object[] args = advice.getParameterArray();
        //ClassicHttpRequest
        final ClassicHttpRequest request = (ClassicHttpRequest)args[0];
        String url = advice.attachment();
        if (url == null) {return;}
        httpClusterTest(advice, request, url);
    }

    private String getUrl(ClassicHttpRequest request) {
        if (request == null) {
            return null;
        }
        URI uri = null;
        try {
            uri = request.getUri();
        } catch (URISyntaxException e) {
            logger.error("获取不到url", e);
        }
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();

        //判断是否在白名单中
        String url = getService(uri.getScheme(), host, port, path);
        return url;
    }

    private void httpClusterTest(Advice advice, final ClassicHttpRequest request, String url)
        throws ProcessControlException {
        MatchConfig config = ClusterTestUtils.httpClusterTest(url);
        Header[] headers = request.getHeaders(PradarService.PRADAR_WHITE_LIST_CHECK);
        if (headers != null && headers.length > 0) {
            config.addArgs(PradarService.PRADAR_WHITE_LIST_CHECK, headers[0].getValue());
        }
        config.addArgs("url", url);
        config.addArgs("request", request);
        config.addArgs("method", "uri");
        config.addArgs("isInterface", Boolean.FALSE);
        config.getStrategy().processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config,
            new ExecutionCall() {
                @Override
                public Object call(Object param) {
                    try {
                        HttpEntity entity = null;
                        if (param instanceof String) {
                            entity = new StringEntity(String.valueOf(param));
                        } else {
                            entity = new ByteArrayEntity(GsonFactory.getGson().toJson(param).getBytes(),
                                ContentType.create(request.getEntity().getContentType()));
                        }
                        BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);
                        response.setEntity(entity);

                        if (HttpClientConstants.clazz == null) {
                            HttpClientConstants.clazz = Class.forName(
                                "org.apache.hc.client5.http.impl.classic.CloseableHttpResponse");
                        }
                        return ReflectionUtils.newInstance(HttpClientConstants.clazz,response, null);

                    } catch (Exception e) {
                    }
                    return null;
                }
            });
    }

    private Map toMap(String queryString) {
        Map map = new HashMap();
        if (StringUtils.isBlank(queryString)) {
            return map;
        }
        String[] array = StringUtils.split(queryString, '&');
        if (array == null || array.length == 0) {
            return map;
        }

        for (String str : array) {
            String[] kv = StringUtils.split(str, '=');
            if (kv == null || kv.length != 2) {
                continue;
            }
            if (StringUtils.isBlank(kv[0])) {
                continue;
            }
            map.put(StringUtils.trim(kv[0]), StringUtils.trim(kv[1]));
        }
        return map;
    }

    private String toString(InputStream in) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int length = 0;
        try {
            while ((length = in.read(data)) != -1) {
                os.write(data, 0, length);
            }
        } catch (IOException e) {
        }
        return new String(os.toByteArray());
    }

    private Map getParameters(HttpRequest httpRequest) {
        URI uri = null;
        try {
            uri = httpRequest.getUri();
        } catch (URISyntaxException e) {
            logger.error("获取不到url", e);
        }
        if (httpRequest instanceof HttpGet) {
            HttpGet httpGet = (HttpGet)httpRequest;
            return toMap(uri.getQuery());
        }
        if (httpRequest instanceof HttpPost) {
            HttpPost httpPost = (HttpPost)httpRequest;
            HttpEntity httpEntity = httpPost.getEntity();
            Map parameters = toMap(uri.getQuery());
            InputStream in = null;
            try {
                in = httpEntity.getContent();
                parameters.putAll(toMap(toString(in)));
            } catch (Throwable t) {
            } finally {
                if (in != null) {
                    try {
                        in.reset();
                    } catch (IOException e) {
                    }
                }
            }
            return parameters;
        }

        if (httpRequest instanceof HttpPut) {
            HttpPut httpPut = (HttpPut)httpRequest;
            HttpEntity httpEntity = httpPut.getEntity();
            Map parameters = toMap(uri.getQuery());
            InputStream in = null;
            try {
                in = httpEntity.getContent();
                parameters.putAll(toMap(toString(in)));
            } catch (Throwable t) {
            } finally {
                if (in != null) {
                    try {
                        in.reset();
                    } catch (IOException e) {
                    }
                }
            }
            return parameters;
        }

        if (httpRequest instanceof HttpDelete) {
            HttpDelete httpDelete = (HttpDelete)httpRequest;
            return toMap(uri.getQuery());
        }

        if (httpRequest instanceof HttpHead) {
            HttpHead httpHead = (HttpHead)httpRequest;
            return toMap(uri.getQuery());
        }

        if (httpRequest instanceof HttpOptions) {
            HttpOptions httpOptions = (HttpOptions)httpRequest;
            return toMap(uri.getQuery());
        }

        if (httpRequest instanceof HttpTrace) {
            HttpTrace httpTrace = (HttpTrace)httpRequest;
            return toMap(uri.getQuery());
        }

        if (httpRequest instanceof HttpPatch) {
            HttpPatch httpPatch = (HttpPatch)httpRequest;
            HttpEntity httpEntity = httpPatch.getEntity();
            Map parameters = toMap(uri.getQuery());
            InputStream in = null;
            try {
                in = httpEntity.getContent();
                parameters.putAll(toMap(toString(in)));
            } catch (Throwable t) {
            } finally {
                if (in != null) {
                    try {
                        in.reset();
                    } catch (IOException e) {
                    }
                }
            }
            return parameters;
        }
        return Collections.EMPTY_MAP;
    }

    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        Object[] args = advice.getParameterArray();
        final HttpRequest request = (HttpRequest)args[0];
        if (request == null) {
            return null;
        }
        if (advice.hasMark(BlackHostChecker.BLACK_HOST_MARK)) {
            return null;
        }
        return new ContextTransfer() {
            @Override
            public void transfer(String key, String value) {
                if (request.getHeaders(HeaderMark.DONT_MODIFY_HEADER) == null || request.getHeaders(
                    HeaderMark.DONT_MODIFY_HEADER).length == 0) {
                    request.setHeader(key, value);
                }
            }
        };
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        final HttpRequest request = (HttpRequest)args[0];
        if (request == null) {
            return null;
        }
        URI uri = null;
        try {
            uri = request.getUri();
        } catch (URISyntaxException e) {
            logger.error("获取不到url", e);
        }
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();

        SpanRecord record = new SpanRecord();
        record.setService(path);
        String method = request.getMethod();
        record.setMethod(method);
        record.setRemoteIp(host);
        record.setPort(port);
        record.setMiddlewareName(HttpClientConstants.HTTP_CLIENT_NAME_5X);
        Header[] headers = request.getHeaders("content-length");
        if (headers != null && headers.length != 0) {
            try {
                Header header = headers[0];
                record.setRequestSize(Integer.valueOf(header.getValue()));
            } catch (NumberFormatException e) {
            }
        }
        //OSS 使用httpclient的时候会验证crc，提前读取request的stream流导致crc验证无法通过，先注释
        return record;

    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        HttpRequest request = (HttpRequest)args[0];
        SpanRecord record = new SpanRecord();
        if (advice.getReturnObj() instanceof ClassicHttpResponse) {
            ClassicHttpResponse response = (ClassicHttpResponse)advice.getReturnObj();

            try {
                record.setResponseSize(response == null ? 0 : response.getEntity().getContentLength());
            } catch (Throwable e) {
                record.setResponseSize(0);
            }
            int code = response.getCode();
            record.setResultCode(code + "");
        } else {
            record.setResponse(advice.getReturnObj());
        }
        try {
            if (request.getHeaders(HeaderMark.DONT_READ_INPUT) == null || request.getHeaders(
                HeaderMark.DONT_READ_INPUT).length == 0) {
                record.setRequest(getParameters(request));
            }
        } catch (Throwable e) {
        }

        return record;

    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        HttpRequest request = (HttpRequest)args[0];
        SpanRecord record = new SpanRecord();
        if (advice.getThrowable() instanceof SocketTimeoutException) {
            record.setResultCode(ResultCode.INVOKE_RESULT_TIMEOUT);
        } else {
            record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        }
        record.setResponse(advice.getThrowable());
        try {
            record.setRequest(getParameters(request));
        } catch (Throwable e) {
        }
        return record;
    }
}
