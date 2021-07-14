package com.pamirs.attach.plugin.jetty.interceptor;

import com.pamirs.attach.plugin.common.web.BufferedServletRequestWrapper;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author fabing.zhaofb
 */
public class Jetty6xServerHandleInterceptor extends AbstractServerHandleInterceptor {
    @Override
    HttpServletRequest toHttpServletRequest(Object[] args) {
        if (args == null || args.length < 4) {
            return null;
        }

        if (args[1] instanceof HttpServletRequest) {
            return (HttpServletRequest) args[1];
        }
        return null;
    }

    @Override
    HttpServletResponse toHttpServletResponse(Object[] args) {
        if (args == null || args.length < 4) {
            return null;
        }

        if (args[2] instanceof HttpServletResponse) {
            return (HttpServletResponse) args[2];
        }
        return null;
    }

    @Override
    void doWrapRequest(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (!(args[1] instanceof HttpServletRequest)) {
            return;
        }
        advice.changeParameter(1, new BufferedServletRequestWrapper((HttpServletRequest) args[1]));
    }
}
