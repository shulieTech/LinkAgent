package com.pamirs.attach.plugin.spring.cloud.gateway.interceptor;

import com.pamirs.attach.plugin.spring.cloud.gateway.filter.RequestHttpHeadersFilter;
import com.pamirs.attach.plugin.spring.cloud.gateway.filter.ResponseHttpHeadersFilter;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author guann1n9
 * @date 2022/6/19 10:01 PM
 */
public class ExternalHeaderFilterInterceptor extends AroundInterceptor {


    @Override
    public void doAfter(Advice advice) {

        List returnObj = advice.getReturnObj() instanceof List ? ((List) advice.getReturnObj()) : null;
        if (returnObj == null) {
            return;
        }
        if (CollectionUtils.isEmpty(returnObj)) {
            return;
        }
        Object element = returnObj.get(0);
        if (!(element.getClass().getName().equals("org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter"))) {
            return;
        }
        RequestHttpHeadersFilter requestHttpHeadersFilter = RequestHttpHeadersFilter.getInstance();
        ResponseHttpHeadersFilter responseHttpHeadersFilter = ResponseHttpHeadersFilter.getInstace();
        if (!returnObj.contains(requestHttpHeadersFilter)) {
            returnObj.add(requestHttpHeadersFilter);
        }
        if (!returnObj.contains(responseHttpHeadersFilter)) {
            returnObj.add(responseHttpHeadersFilter);
        }
    }
}
