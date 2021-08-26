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
package org.eclipse.jetty.server;

import org.eclipse.jetty.continuation.ContinuationListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/12/25 2:15 下午
 */
public interface AsyncContext
{
    static final String ASYNC_REQUEST_URI = "javax.servlet.async.request_uri";
    static final String ASYNC_CONTEXT_PATH = "javax.servlet.async.context_path";
    static final String ASYNC_PATH_INFO = "javax.servlet.async.path_info";
    static final String ASYNC_SERVLET_PATH = "javax.servlet.async.servlet_path";
    static final String ASYNC_QUERY_STRING = "javax.servlet.async.query_string";

    public ServletRequest getRequest();
    public ServletResponse getResponse();
    public boolean hasOriginalRequestAndResponse();
    public void dispatch();
    public void dispatch(String path);
    public void dispatch(ServletContext context, String path);
    public void complete();
    public void start(Runnable run);
    public void setTimeout(long ms);
    public void addContinuationListener(ContinuationListener listener);
}
