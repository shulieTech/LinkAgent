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
package com.pamirs.attach.plugin.websphere.common;

import com.ibm.websphere.servlet.response.IResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/13 5:25 下午
 */
public class HttpServletResponseImpl implements HttpServletResponse {
    private IResponse servletResponse;

    public HttpServletResponseImpl(IResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    @Override
    public void addCookie(Cookie cookie) {
        servletResponse.addCookie(cookie);
    }

    @Override
    public boolean containsHeader(String name) {
        return servletResponse.containsHeader(name);
    }

    @Override
    public String encodeURL(String url) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return null;
    }

    @Override
    public String encodeUrl(String url) {
        return null;
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return null;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {

    }

    @Override
    public void sendError(int sc) throws IOException {

    }

    @Override
    public void sendRedirect(String location) throws IOException {

    }

    @Override
    public void setDateHeader(String name, long date) {
        servletResponse.setDateHeader(name, date);
    }

    @Override
    public void addDateHeader(String name, long date) {
        servletResponse.addDateHeader(name, date);
    }

    @Override
    public void setHeader(String name, String value) {
        servletResponse.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        servletResponse.addHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        servletResponse.setIntHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        servletResponse.addIntHeader(name, value);
    }

    @Override
    public void setStatus(int sc) {
        servletResponse.setStatusCode(sc);
    }

    @Override
    public void setStatus(int sc, String sm) {
        servletResponse.setStatusCode(sc);
    }

    @Override
    public int getStatus() {
        return 200;
    }

    @Override
    public String getHeader(String name) {
        return servletResponse.getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        List<String> list = new ArrayList<String>();
        Enumeration<String> enumeration = servletResponse.getHeaders(name);
        while (enumeration.hasMoreElements()) {
            list.add(enumeration.nextElement());
        }
        return list;
    }

    @Override
    public Collection<String> getHeaderNames() {
        List<String> list = new ArrayList<String>();
        Enumeration<String> enumeration = servletResponse.getHeaderNames();
        while (enumeration.hasMoreElements()) {
            list.add(enumeration.nextElement());
        }
        return list;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStreamImpl(servletResponse.getOutputStream());
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return null;
    }

    @Override
    public void setCharacterEncoding(String charset) {

    }

    @Override
    public void setContentLength(int len) {

    }

    @Override
    public void setContentLengthLong(long len) {

    }

    @Override
    public void setContentType(String type) {
        servletResponse.setContentType(type);
    }

    @Override
    public void setBufferSize(int size) {

    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {

    }

    @Override
    public void resetBuffer() {

    }

    @Override
    public boolean isCommitted() {
        return servletResponse.isCommitted();
    }

    @Override
    public void reset() {

    }

    @Override
    public void setLocale(Locale loc) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }
}
