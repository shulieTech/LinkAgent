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
package com.pamirs.attach.plugin.jetty;


import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.pradar.Pradar;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.server.AsyncContext;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class Jetty7xAsyncListener implements ContinuationListener {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();
    private final boolean isInfo = logger.isInfoEnabled();
    private final AsyncContext asyncContext;
    private final Map<String, String> invokeContext_;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final RequestTracer<HttpServletRequest, HttpServletResponse> requestTracer;


    public Jetty7xAsyncListener(final AsyncContext asyncContext, final Map<String, String> invokeContext_, final HttpServletRequest request, final HttpServletResponse response, final RequestTracer<HttpServletRequest, HttpServletResponse> requestTracer) {
        this.asyncContext = asyncContext;
        this.invokeContext_ = invokeContext_;
        this.request = request;
        this.response = response;
        this.requestTracer = requestTracer;
    }

    private int getStatusCode(final Continuation continuation) {
        try {
            if (continuation.getServletResponse() instanceof Response) {
                return ((Response) continuation.getServletResponse()).getStatus();
            }
        } catch (Exception ignored) {
        }
        return 200;
    }

    @Override
    public void onComplete(Continuation continuation) {
        if (isDebug) {
            logger.debug("Complete asynchronous operation. event={}", continuation);
        }

        if (continuation == null) {
            if (isInfo) {
                logger.info("Invalid event. event is null");
            }
            return;
        }

        try {
            HttpServletResponse response = null;
            if (continuation.getServletResponse() instanceof HttpServletResponse) {
                response = ((HttpServletResponse) continuation.getServletResponse());
            } else {
                response = this.response;
            }
            Pradar.setInvokeContext(invokeContext_);
            requestTracer.endTrace(request, response, null, String.valueOf(getStatusCode(continuation)));
        } catch (Throwable t) {
            if (isInfo) {
                logger.info("Failed to async event handle. event={}", continuation, t);
            }
        } finally {
            Pradar.clearInvokeContext();
        }
    }

    @Override
    public void onTimeout(Continuation continuation) {
        if (isDebug) {
            logger.debug("Timeout asynchronous operation. event={}", continuation);
        }

        if (continuation == null) {
            if (isDebug) {
                logger.debug("Invalid event. event is null");
            }
            return;
        }

        try {
            Pradar.setInvokeContext(invokeContext_);
            requestTracer.endTrace(request, response, null, "408");
        } catch (Throwable t) {
            logger.info("Failed to async event handle. event={}", continuation, t);
        } finally {
            Pradar.clearInvokeContext();
        }
    }
}
