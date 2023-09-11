package com.pamirs.attach.plugin.httpclient.utils;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpUriRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/7/15 9:51 上午
 */
public class HttpRequestUtil {

    /**
     * 获取httpRequest对象的path
     *
     * @param request HttpRequest对象
     * @return path
     */
    public static String findPath(HttpRequest request) {
        String path = "";
        if (request == null) {
            return path;
        }
        if (request instanceof HttpUriRequest) {
            path = ((HttpUriRequest) request).getURI().getPath();
        } else {
            path = reflectFieldVal(request);
        }
        return path;
    }

    /**
     * 反射获取path值
     *
     * @param request http请求值
     * @return path
     */
    private static String reflectFieldVal(HttpRequest request) {
        String path = "";
        Object uriField = ReflectionUtils.get(request,"uri");
        if (uriField instanceof String) {
            URI uri = null;
            try {
                uri = new URI((String) uriField);
                path = uri.getPath();
            } catch (URISyntaxException e) {
                // ignore
            }
        } else if (uriField instanceof URI) {
            path = ((URI) uriField).getPath();
        } else if (uriField instanceof URL) {
            path = ((URL) uriField).getPath();
        }
        return path;
    }
}
