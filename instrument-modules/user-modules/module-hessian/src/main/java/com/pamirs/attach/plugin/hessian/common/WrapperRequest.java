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
package com.pamirs.attach.plugin.hessian.common;


import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/16 4:06 下午
 */
public class WrapperRequest implements HttpServletRequest {
    private byte[] data;
    private HttpServletRequest request;

    public WrapperRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public String getAuthType() {
        return this.request.getAuthType();
    }

    @Override
    public Cookie[] getCookies() {
        return this.request.getCookies();
    }

    @Override
    public long getDateHeader(String s) {
        return this.request.getDateHeader(s);
    }

    @Override
    public String getHeader(String s) {
        return this.request.getHeader(s);
    }

    @Override
    public Enumeration<String> getHeaders(String s) {
        return this.request.getHeaders(s);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return this.request.getHeaderNames();
    }

    @Override
    public int getIntHeader(String s) {
        return this.request.getIntHeader(s);
    }

    @Override
    public String getMethod() {
        return this.request.getMethod();
    }

    @Override
    public String getPathInfo() {
        return this.request.getPathInfo();
    }

    @Override
    public String getPathTranslated() {
        return this.request.getPathTranslated();
    }

    @Override
    public String getContextPath() {
        return this.request.getContextPath();
    }

    @Override
    public String getQueryString() {
        return this.request.getQueryString();
    }

    @Override
    public String getRemoteUser() {
        return this.request.getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String s) {
        return this.request.isUserInRole(s);
    }

    @Override
    public Principal getUserPrincipal() {
        return this.request.getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId() {
        return this.request.getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
        return this.request.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        return this.request.getRequestURL();
    }

    @Override
    public String getServletPath() {
        return this.request.getServletPath();
    }

    @Override
    public HttpSession getSession(boolean b) {
        return this.request.getSession(b);
    }

    @Override
    public HttpSession getSession() {
        return this.request.getSession();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return this.request.isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return this.request.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return this.request.isRequestedSessionIdFromURL();
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return this.request.isRequestedSessionIdFromUrl();
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        return this.request.authenticate(httpServletResponse);
    }

    @Override
    public void login(String s, String s1) throws ServletException {
        this.request.login(s, s1);
    }

    @Override
    public void logout() throws ServletException {
        this.request.logout();
    }

    @Override
    public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException {
        return this.request.getParts();
    }

    @Override
    public Part getPart(String s) throws IOException, IllegalStateException, ServletException {
        return this.request.getPart(s);
    }

    @Override
    public Object getAttribute(String s) {
        return this.request.getAttribute(s);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return this.request.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
        return this.request.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        this.request.setCharacterEncoding(s);
    }

    @Override
    public int getContentLength() {
        return this.request.getContentLength();
    }

    @Override
    public String getContentType() {
        return this.request.getContentType();
    }

    private byte[] readRequest(HttpServletRequest request) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ServletInputStream in = request.getInputStream();
            byte[] data = new byte[1024];
            int len = 0;
            while ((len = in.read(data)) != -1) {
                bos.write(data, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(e);
            }
            return null;
        }
    }

    @Override
    public ServletInputStream getInputStream() {
        if (data != null) {
            return new WrapperServletInputStream(new ByteArrayInputStream(data));
        }
        synchronized (this) {
            if (data != null) {
                return new WrapperServletInputStream(new ByteArrayInputStream(data));
            }
            this.data = readRequest(this.request);
        }
        return new WrapperServletInputStream(new ByteArrayInputStream(data));
    }

    @Override
    public String getParameter(String s) {
        return this.request.getParameter(s);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return this.request.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String s) {
        return this.request.getParameterValues(s);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return this.request.getParameterMap();
    }

    @Override
    public String getProtocol() {
        return this.request.getProtocol();
    }

    @Override
    public String getScheme() {
        return this.request.getScheme();
    }

    @Override
    public String getServerName() {
        return this.request.getServerName();
    }

    @Override
    public int getServerPort() {
        return this.request.getServerPort();
    }

    @Override
    public BufferedReader getReader() {
        if (data != null) {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
        }
        synchronized (this) {
            if (data != null) {
                return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
            }
            this.data = readRequest(this.request);
        }
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
    }

    @Override
    public String getRemoteAddr() {
        return this.request.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return this.request.getRemoteHost();
    }

    @Override
    public void setAttribute(String s, Object o) {
        this.request.setAttribute(s, o);
    }

    @Override
    public void removeAttribute(String s) {
        this.request.removeAttribute(s);
    }

    @Override
    public Locale getLocale() {
        return this.request.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return this.request.getLocales();
    }

    @Override
    public boolean isSecure() {
        return this.request.isSecure();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return this.request.getRequestDispatcher(s);
    }

    @Override
    public String getRealPath(String s) {
        return this.request.getRealPath(s);
    }

    @Override
    public int getRemotePort() {
        return this.request.getRemotePort();
    }

    @Override
    public String getLocalName() {
        return this.request.getLocalName();
    }

    @Override
    public String getLocalAddr() {
        return this.request.getLocalAddr();
    }

    @Override
    public int getLocalPort() {
        return this.request.getLocalPort();
    }

    @Override
    public ServletContext getServletContext() {
        return this.request.getServletContext();
    }

    @Override
    public AsyncContext startAsync() {
        return this.request.startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
        return this.request.startAsync(servletRequest, servletResponse);
    }

    @Override
    public boolean isAsyncStarted() {
        return this.request.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported() {
        return this.request.isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext() {
        return this.request.getAsyncContext();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return this.request.getDispatcherType();
    }
}
