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

import com.pamirs.pradar.PradarCoreUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

public class RequestUtils {

    public static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP", // 优先获取其他代理设置的真实用户ip
            "X-Real-IP",          // enginx 设置 remoteIP，如果没有拿到 NS-Client-IP，那么这就是真实的用户 ip
            "NS-Client-IP",       // NAT 方式设置的ip
    };


    private static final boolean checkIP(String ip) {
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 获取远程客户端的 IP
     */
    public static final String getRemoteAddress(ServletRequest req) {
        String ip = null;
        boolean valid = false;

        HttpServletRequest request = analyzeHttpServletRequest(req);
        if (request == null) {
            return null;
        }

        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            valid = checkIP(ip);
            if (valid) {
                break;
            }
        }
        if (PradarCoreUtils.isBlank(ip)) {
            ip = request.getRemoteAddr();
        }
        // 代理时会有逗号分隔的 ip，获取第一个即可
        int index = ip.indexOf(',');
        if (index != -1) {
            String firstIp = ip.substring(0, index).trim();
            if (checkIP(ip)) {
                ip = firstIp;
            }
        }
        return ip;
    }

    private static HttpServletRequest analyzeHttpServletRequest(ServletRequest httpRequest) {
        HttpServletRequest request = null;
        if (httpRequest instanceof HttpServletRequest) {
            request = (HttpServletRequest) httpRequest;
        }

        if (request == null) {
            return null;
        }
        return request;
    }

    private static String getProperty(ServletRequest httpRequest, String... keys) {
        HttpServletRequest request = analyzeHttpServletRequest(httpRequest);
        if (request == null) {
            return null;
        }

        for (String key : keys) {
            String value = PradarCoreUtils.trim(request.getHeader(key));
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }

        return null;
    }

    public static String getRemoteIp(ServletRequest httpRequest) {
        HttpServletRequest request = analyzeHttpServletRequest(httpRequest);
        if (request == null) {
            return null;
        }
        String remoteIp = request.getHeader("Pradar-Remote-Ip");
        if (StringUtils.isBlank(remoteIp)) {
            return getRemoteAddress(httpRequest);
        }
        return remoteIp;
    }


}
