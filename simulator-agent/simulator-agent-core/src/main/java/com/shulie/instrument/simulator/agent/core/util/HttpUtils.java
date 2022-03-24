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
package com.shulie.instrument.simulator.agent.core.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public static String doGet(String url, Map<String, String> headers) {
        HostPort hostPort = getHostPortUrlFromUrl(url);
        return doGet(hostPort.host, hostPort.port, headers, hostPort.url);
    }

    public static String doGet(String host, int port, Map<String, String> headers, String url) {
        InputStream input = null;
        OutputStream output = null;
        Socket socket = null;
        StringBuilder request = new StringBuilder("GET ").append(url).append(" HTTP/1.1\r\n")
            .append("Host: ").append(host).append(":").append(port).append("\r\n")
            .append("Connection: Keep-Alive\r\n");

        if (!headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (!StringUtils.isBlank(entry.getValue())) {
                    request.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
                }
            }
        }
        request.append("\r\n");

        try {
            SocketAddress address = new InetSocketAddress(host, port);
            socket = new Socket();
            // 设置建立连接超时时间 1s
            socket.connect(address, 2000);
            // 设置读取数据超时时间 5s
            socket.setSoTimeout(10000);
            output = socket.getOutputStream();
            output.write(request.toString().getBytes(UTF_8));
            output.flush();
            input = socket.getInputStream();
            String status = readLine(input);
            if (status == null || !status.contains("200")) {
                return null;
            }
            Map<String, List<String>> inputHeaders = readHeaders(input);
            input = wrapperInput(inputHeaders, input);
            return toString(input);
        } catch (IOException e) {
            logger.warn("do http request fail!: url=" + url + "; reqeust:" + request, e);
            return null;
        } finally {
            closeQuietly(input);
            closeQuietly(output);

            // JDK 1.6 Socket没有实现Closeable接口
            if (socket != null) {
                try {
                    socket.close();
                } catch (final IOException ioe) {
                    // ignore
                }
            }

        }
    }

    public static HttpResult doPost(String url, Map<String, String> headers, String body) {
        HostPort hostPort = getHostPortUrlFromUrl(url);
        return doPost(hostPort.host, hostPort.port, hostPort.url, headers, body);
    }

    public static HttpResult doPost(String host, int port, String url, Map<String, String> headers, String body) {
        InputStream input = null;
        OutputStream output = null;
        Socket socket = null;

        StringBuilder request = new StringBuilder("POST ").append(url).append(" HTTP/1.1\r\n")
            .append("Host: ").append(host).append(":").append(port).append("\r\n")
            .append("Connection: Keep-Alive\r\n");

        if (!headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (!StringUtils.isBlank(entry.getValue())) {
                    request.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
                }
            }
        }

        try {
            SocketAddress address = new InetSocketAddress(host, port);
            socket = new Socket();
            socket.connect(address, 2000); // 设置建立连接超时时间 1s
            socket.setSoTimeout(10000); // 设置读取数据超时时间 5s
            output = socket.getOutputStream();

            if (body != null && !body.isEmpty()) {
                request.append("Content-Length: ").append(body.getBytes().length).append("\r\n")
                    .append("Content-Type: application/json\r\n");
            }
            request.append("\r\n");
            output.write(request.toString().getBytes(UTF_8));

            if (body != null && !body.isEmpty()) {
                output.write(body.getBytes(UTF_8));
            }
            output.flush();

            input = socket.getInputStream();
            String statusStr = StringUtils.trim(readLine(input));
            String[] statusArr = StringUtils.split(statusStr, ' ');
            int status = 500;
            try {
                status = Integer.parseInt(statusArr[1]);
            } catch (Throwable e) {
                // ignore
            }
            Map<String, List<String>> inputHeaders = readHeaders(input);
            input = wrapperInput(inputHeaders, input);
            String result = toString(input);
            return HttpResult.result(status, result);
        } catch (IOException e) {
            logger.error("do http request fail!: url=" + url + "; reqeust:" + request, e);
            return null;
        } finally {
            closeQuietly(input);
            closeQuietly(output);

            // JDK 1.6 Socket没有实现Closeable接口
            if (socket != null) {
                try {
                    socket.close();
                } catch (final IOException ioe) {
                    // ignore
                }
            }

        }
    }

    public static String toString(InputStream input) throws IOException {
        ByteArrayOutputStream content = null;
        try {
            content = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = input.read(buffer)) > 0) {
                content.write(buffer, 0, len);
            }
            return new String(content.toByteArray(), UTF_8);
        } finally {
            closeQuietly(content);
        }

    }

    public static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream bufdata = new ByteArrayOutputStream();
        int ch;
        while ((ch = input.read()) >= 0) {
            bufdata.write(ch);
            if (ch == '\n') {
                break;
            }
        }
        if (bufdata.size() == 0) {
            return null;
        }
        byte[] rawdata = bufdata.toByteArray();
        int len = rawdata.length;
        int offset = 0;
        if (len > 0) {
            if (rawdata[len - 1] == '\n') {
                offset++;
                if (len > 1) {
                    if (rawdata[len - 2] == '\r') {
                        offset++;
                    }
                }
            }
        }
        return new String(rawdata, 0, len - offset, UTF_8);
    }

    public static InputStream wrapperInput(Map<String, List<String>> headers, InputStream input) {
        List<String> transferEncodings = headers.get("Transfer-Encoding");
        if (transferEncodings != null && !transferEncodings.isEmpty()) {
            String encodings = transferEncodings.get(0);
            String[] elements = encodings.split(";");
            int len = elements.length;
            if (len > 0 && "chunked".equalsIgnoreCase(elements[len - 1])) {
                return new ChunkedInputStream(input);
            }
            return input;
        }
        List<String> contentLengths = headers.get("Content-Length");
        if (contentLengths != null && !contentLengths.isEmpty()) {
            long length = -1;
            for (String contentLength : contentLengths) {
                try {
                    length = Long.parseLong(contentLength);
                    break;
                } catch (final NumberFormatException ignore) {
                    // ignored
                }
            }
            if (length >= 0) {
                return new ContentLengthInputStream(input, length);
            }
        }
        return input;
    }

    public static Map<String, List<String>> readHeaders(InputStream input)
        throws IOException {
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        String line = readLine(input);
        while (line != null && !line.isEmpty()) {
            String[] headerPair = line.split(":");
            String name = headerPair[0].trim();
            String value = headerPair[1].trim();
            List<String> values = headers.get(name);
            if (values == null) {
                values = new ArrayList<String>();
                headers.put(name, values);
            }
            values.add(value);
            line = readLine(input);
        }
        return headers;
    }

    public static void exhaustInputStream(InputStream inStream)
        throws IOException {
        byte buffer[] = new byte[1024];
        while (inStream.read(buffer) >= 0) {
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException ioe) {
                // ignore
            }
        }
    }

    private static final Pattern URL_PATTERN = Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?",
        Pattern.CASE_INSENSITIVE);

    private static HostPort getHostPortUrlFromUrl(String url) {
        String domain = url;
        String restUrl = url;
        Matcher matcher = URL_PATTERN.matcher(url);
        if (matcher.find()) {
            String group = matcher.group();
            domain = group.substring(group.indexOf("//") + 2, group.length());
            restUrl = url.substring(url.indexOf(group) + group.length(), url.length());
        }

        HostPort hostPort = new HostPort();
        hostPort.url = restUrl;
        if (!domain.contains(":")) {
            hostPort.host = domain;
            hostPort.port = 80;
        } else {
            hostPort.host = domain.substring(0, domain.indexOf(":"));
            hostPort.port = Integer.parseInt(domain.substring(domain.indexOf(":") + 1, domain.length()));
        }
        return hostPort;
    }

    private static class HostPort {
        public String host;
        public int port;
        public String url;

        @Override
        public String toString() {
            return "HostPort{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", url='" + url + '\'' +
                '}';
        }
    }

    public static class HttpResult {
        /**
         * 是否成功
         */
        private int status;
        /**
         * 结果
         */
        private String result;

        public static HttpResult result(int status, String result) {
            HttpResult httpResult = new HttpResult();
            httpResult.setStatus(status);
            httpResult.setResult(result);
            return httpResult;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public boolean isSuccess() {
            return status == 200;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

}
