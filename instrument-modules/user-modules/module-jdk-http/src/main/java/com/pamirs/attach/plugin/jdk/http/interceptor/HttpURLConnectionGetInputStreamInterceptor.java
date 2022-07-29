package com.pamirs.attach.plugin.jdk.http.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.mock.JsonMockStrategy;
import com.pamirs.pradar.pressurement.mock.MockStrategy;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author jiangjibo
 * @date 2022/3/3 2:16 下午
 * @description:
 */
public class HttpURLConnectionGetInputStreamInterceptor extends HttpURLConnectionInterceptor {

    @Override
    public void beforeLast(Advice advice) throws ProcessControlException {
        if (!Pradar.isClusterTest()) {
            return;
        }
        Object target = advice.getTarget();
        final HttpURLConnection request = (HttpURLConnection) target;
        final URL url = request.getURL();
        String fullPath = getService(url.getProtocol(),
                url.getHost(),
                url.getPort(),
                url.getPath());
        String whiteList = request.getRequestProperty(PradarService.PRADAR_WHITE_LIST_CHECK);

        MatchConfig config = ClusterTestUtils.httpClusterTest(fullPath);
        ExecutionStrategy strategy = config.getStrategy();

        config.addArgs(PradarService.PRADAR_WHITE_LIST_CHECK, whiteList);
        // 白名单需要的信息
        config.addArgs("url", fullPath);
        // mock转发需要信息
        config.addArgs("request", request);
        config.addArgs("method", "url");
        config.addArgs("isInterface", Boolean.FALSE);
        config.getStrategy().processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config);
    }
}
