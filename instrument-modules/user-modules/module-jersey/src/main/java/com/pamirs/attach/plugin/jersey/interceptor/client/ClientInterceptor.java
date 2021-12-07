package com.pamirs.attach.plugin.jersey.interceptor.client;

import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Collections;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.pamirs.attach.plugin.jersey.JerseyConstants;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.adapter.ExecutionForwardCall;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.utils.InnerWhiteListCheckUtil;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/02 3:27 下午
 */
public class ClientInterceptor extends TraceInterceptorAdaptor {
    @Override
    public String getPluginName() {
        return JerseyConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return JerseyConstants.CLIENT_PLUGIN_TYPE;
    }

    @Override
    protected boolean isClient(Advice advice) {
        return true;
    }

    @Override
    public void beforeFirst(Advice advice) throws ProcessControlException, MalformedURLException {
        Object[] args = advice.getParameterArray();
        if (!(args[0] instanceof ClientRequest)) {
            return;
        }
        final ClientRequest clientRequest = (ClientRequest)args[0];
        final String url = clientRequest.getUri().toURL().toString();
        final MatchConfig config = ClusterTestUtils.httpClusterTest(url);
        config.addArgs(PradarService.PRADAR_WHITE_LIST_CHECK,
            clientRequest.getHeaderString(PradarService.PRADAR_WHITE_LIST_CHECK));
        config.addArgs("url", url);
        config.addArgs("isInterface", Boolean.FALSE);
        config.getStrategy().processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config,
            new ExecutionForwardCall() {
                @Override
                public Object forward(Object param) {
                    clientRequest.setUri(URI.create(config.getForwarding()));
                    return null;
                }

                @Override
                public Object call(Object param) {
                    return new ClientResponse(clientRequest, Response.ok(param).build());
                }
            });
    }

    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (!(args[0] instanceof ClientRequest)) {
            return null;
        }
        final ClientRequest clientRequest = (ClientRequest)args[0];
        final MultivaluedMap<String, Object> headers = clientRequest.getHeaders();
        return new ContextTransfer() {
            @Override
            public void transfer(String key, String value) {
                headers.put(key, Collections.<Object>singletonList(value));
            }
        };
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        InnerWhiteListCheckUtil.check();
        Object[] args = advice.getParameterArray();
        if (!(args[0] instanceof ClientRequest)) {
            return null;
        }
        final ClientRequest clientRequest = (ClientRequest)args[0];
        URI uri = clientRequest.getUri();
        SpanRecord record = new SpanRecord();
        record.setRemoteIp(uri.getHost());
        record.setService(uri.getPath());
        record.setMethod(clientRequest.getMethod());
        record.setPort(uri.getPort());
        record.setRequest(uri.getQuery());
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        if (advice.getThrowable() instanceof SocketTimeoutException) {
            record.setResultCode(ResultCode.INVOKE_RESULT_TIMEOUT);
        } else {
            record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        }
        record.setResponse(advice.getThrowable());
        record.setResponseSize(0);
        InnerWhiteListCheckUtil.check();
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        ClientResponse response = (ClientResponse)advice.getReturnObj();
        record.setResultCode(String.valueOf(response.getStatus()));
        InnerWhiteListCheckUtil.check();
        return record;
    }
}
