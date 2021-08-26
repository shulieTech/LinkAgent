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
package com.ibm.websphere.servlet.request;

import com.ibm.websphere.servlet.response.IResponse;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;

public interface IRequest {
    String getMethod();

    String getRequestURI();

    String getRemoteUser();

    String getAuthType();

    String getHeader(String paramString);

    Enumeration getHeaders(String paramString);

    long getDateHeader(String paramString);

    int getIntHeader(String paramString);

    void clearHeaders();

    Enumeration getHeaderNames();

    int getContentLength();

    String getContentType();

    String getProtocol();

    String getServerName();

    int getServerPort();

    String getRemoteHost();

    String getRemoteAddr();

    int getRemotePort();

    String getScheme();

    InputStream getInputStream()
            throws IOException;

    String getLocalAddr();

    String getLocalName();

    int getLocalPort();

    boolean isSSL();

    byte[] getSSLSessionID();

    String getSessionID();

    boolean isProxied();

    IResponse getWCCResponse();

    String getCipherSuite();

    X509Certificate[] getPeerCertificates();

    String getQueryString();

    Cookie[] getCookies();

    byte[] getCookieValue(String paramString);

    List getAllCookieValues(String paramString);

    boolean getShouldDestroy();

    void setShouldDestroy(boolean paramBoolean);

    void setShouldReuse(boolean paramBoolean);

    void setShouldClose(boolean paramBoolean);

    void removeHeader(String paramString);

    void startAsync();

    boolean isStartAsync();

    void lock();

    void unlock();
}
