package com.pamirs.attach.plugin.async.http.client.plugin.utils;

import com.alibaba.fastjson.JSON;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import org.asynchttpclient.Response;
import org.asynchttpclient.uri.Uri;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

public class DefaultAsyncResponse implements Response {

    private Object result;

    public DefaultAsyncResponse(Object result) {
        this.result = result;
    }

    @Override
    public int getStatusCode() {
        return 200;
    }

    @Override
    public String getStatusText() {
        return null;
    }

    @Override
    public byte[] getResponseBodyAsBytes() {
        return JSON.toJSONBytes(result);
    }

    @Override
    public ByteBuffer getResponseBodyAsByteBuffer() {
        return null;
    }

    @Override
    public InputStream getResponseBodyAsStream() {
        return new ByteArrayInputStream(JSON.toJSONBytes(result));
    }

    @Override
    public String getResponseBody(Charset charset) {
        return result instanceof String ? (String) result : JSON.toJSONString(result);
    }

    @Override
    public String getResponseBody() {
        return result instanceof String ? (String) result : JSON.toJSONString(result);
    }

    @Override
    public Uri getUri() {
        return null;
    }

    @Override
    public String getContentType() {
        return "json";
    }

    @Override
    public String getHeader(CharSequence name) {
        return null;
    }

    @Override
    public List<String> getHeaders(CharSequence name) {
        return null;
    }

    @Override
    public HttpHeaders getHeaders() {
        return null;
    }

    @Override
    public boolean isRedirected() {
        return false;
    }

    @Override
    public List<Cookie> getCookies() {
        return null;
    }

    @Override
    public boolean hasResponseStatus() {
        return false;
    }

    @Override
    public boolean hasResponseHeaders() {
        return false;
    }

    @Override
    public boolean hasResponseBody() {
        return false;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return null;
    }
}
