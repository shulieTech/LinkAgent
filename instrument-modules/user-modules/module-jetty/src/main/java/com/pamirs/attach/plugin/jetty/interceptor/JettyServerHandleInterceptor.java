package com.pamirs.attach.plugin.jetty.interceptor;

import com.pamirs.attach.plugin.common.web.BufferedServletRequestWrapper;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author fabing.zhaofb
 */
public class JettyServerHandleInterceptor extends AbstractServerHandleInterceptor {
    @Override
    HttpServletRequest toHttpServletRequest(Object[] args) {
        if (args == null || args.length < 4) {
            return null;
        }

        if (args[2] instanceof HttpServletRequest) {
            try {
                return (HttpServletRequest) args[2];
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    HttpServletResponse toHttpServletResponse(Object[] args) {
        if (args == null || args.length < 4) {
            return null;
        }

        if (args[3] instanceof HttpServletResponse) {
            try {
                return (HttpServletResponse) args[3];
            } catch (Throwable ignored) {
            }
        }
        return null;

    }

    @Override
    void doWrapRequest(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (!(args[2] instanceof HttpServletRequest)) {
            return;
        }
        advice.changeParameter(2, new BufferedServletRequestWrapper((HttpServletRequest) args[2]));
    }
}
