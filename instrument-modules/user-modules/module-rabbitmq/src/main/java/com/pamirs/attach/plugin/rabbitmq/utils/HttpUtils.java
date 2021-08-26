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
package com.pamirs.attach.plugin.rabbitmq.utils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.common.ChunkedInputStream;
import com.pamirs.pradar.common.ContentLengthInputStream;
import org.apache.commons.lang.StringUtils;

public abstract class HttpUtils {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public static HttpResult doGet(AdminAccessInfo adminAccessInfo, String url) {
        InputStream input = null;
        OutputStream output = null;
        Socket socket = null;
        try {
            SocketAddress address = new InetSocketAddress(adminAccessInfo.getHost(), adminAccessInfo.getPort());
            String request = "GET " + url + " HTTP/1.1\r\n"
                + "Host: " + adminAccessInfo.getHost() + ":" + adminAccessInfo.getPort() + "\r\n"
                + "Connection: Keep-Alive\r\n"
                + "Authorization: " + adminAccessInfo.credentialsEncode() + "\r\n"
                + "\r\n";
            socket = new Socket();
            // 设置建立连接超时时间 5s
            socket.connect(address, 5000);
            // 设置读取数据超时时间 10s
            socket.setSoTimeout(10000);
            output = socket.getOutputStream();
            output.write(request.getBytes(UTF_8));
            output.flush();
            input = socket.getInputStream();
            String statusStr = StringUtils.trim(readLine(input));
            String[] statusArr = StringUtils.split(statusStr, ' ');
            int status = 500;
            try {
                status = Integer.valueOf(statusArr[1]);
            } catch (Throwable e) {
            }

            Map<String, List<String>> responseHeaders = readHeaders(input);
            input = wrapperInput(responseHeaders, input);
            String result = toString(input);
            return HttpResult.result(status, result);
        } catch (Throwable e) {
            return HttpResult.result(500, Throwables.getStackTraceAsString(e));
        } finally {
            closeQuietly(input);
            closeQuietly(output);

            // JDK 1.6 Socket没有实现Closeable接口
            if (socket != null) {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (final IOException ioe) {
                        // ignore
                    }
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
            String[] elements = StringUtils.split(encodings, ';');
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
            String[] headerPair = StringUtils.split(line, ':');
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

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException ioe) {
                // ignore
            }
        }
    }

    private static Pattern URL_PATTERN = Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) {
        System.out.println(
            doGet(new AdminAccessInfo("192.168.1.95", 15672, "canace", "123456", "/"),
                "/api/exchanges/%2F/direct-exchange/bindings/source").result);
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
