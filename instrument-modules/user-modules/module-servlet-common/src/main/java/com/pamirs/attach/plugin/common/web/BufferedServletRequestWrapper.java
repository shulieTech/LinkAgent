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
package com.pamirs.attach.plugin.common.web;

import com.pamirs.attach.plugin.common.web.utils.Constants;
import com.pamirs.pradar.PradarService;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Request 包装类
 * 注意：这种使用方法并不是最佳实践，很容易带来类找不到的问题，所以在重写方法的时候需要注意避免加载新的业务类
 * 因为这个时候Simulator 已经获取不到业务类加载器了，就会报找不到类的错误
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/13 8:02 下午
 */
public class BufferedServletRequestWrapper extends HttpServletRequestWrapper implements IBufferedServletRequestWrapper {

    private byte[] buffer;
    private HttpServletRequest request;

    private boolean isClusterTest = false;
    private Map<String, String> traceContext = new HashMap<String, String>(8, 1);


    public BufferedServletRequestWrapper(HttpServletRequest request) {
        super(request);
        this.request = request;
    }

    private void initBuffer() {
        try {
            InputStream is = request.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buff = new byte[1024];
            int read;
            while ((read = is.read(buff)) != -1) {
                baos.write(buff, 0, read);
            }
            byte[] reqBuffer = baos.toByteArray();

            if (reqBuffer != null && reqBuffer.length > 0){
                String paramData = new String(reqBuffer);
                if (paramData.contains(Constants.PRADAR_CLUSTER_FLAG_GW)){
                    isClusterTest = true;
                    int startIndex = paramData.indexOf(Constants.PRADAR_CLUSTER_FLAG_GW);
                    int endIndex = paramData.indexOf(Constants.extendParamEnd);

                    String agentParam = paramData.substring(startIndex + Constants.PRADAR_CLUSTER_FLAG_GW.length(), endIndex);

                    String businessParam = paramData.substring(0, startIndex) + paramData.substring(endIndex + Constants.extendParamEnd.length(), paramData.length());

                    String[] rpcInfo = agentParam.split(",");
                    traceContext.put(PradarService.PRADAR_TRACE_APPNAME_KEY, rpcInfo[0]);
                    traceContext.put(PradarService.PRADAR_UPSTREAM_APPNAME_KEY, rpcInfo[1]);
                    traceContext.put(PradarService.PRADAR_TRACE_ID_KEY, rpcInfo[2]);
                    traceContext.put(PradarService.PRADAR_INVOKE_ID_KEY, rpcInfo[3]);
                    traceContext.put(PradarService.PRADAR_TRACE_NODE_KEY, rpcInfo[4]);
                    this.buffer = businessParam.getBytes();
                } else {
                    this.buffer = reqBuffer;
                }
            } else {
                this.buffer = reqBuffer;
            }
        } catch (IOException e) {
            //ignore
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (this.buffer == null) {
            initBuffer();
        }
        return new BufferedServletInputStream(this.buffer);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (this.buffer == null) {
            initBuffer();
        }
        String characterEncoding = request.getCharacterEncoding();
        if (characterEncoding == null) {
            characterEncoding = "ISO8859-1";
        }
        return new BufferedReader(new InputStreamReader(getInputStream(), characterEncoding));
    }

    @Override
    public byte[] getBody() {
        if (null == this.buffer) {
            return null;
        }
        return this.buffer;
    }

    static class BufferedServletInputStream extends ServletInputStream {
        private ByteArrayInputStream inputStream;

        public BufferedServletInputStream(byte[] buffer) {
            this.inputStream = new ByteArrayInputStream(buffer);
        }

        @Override
        public int available() throws IOException {
            return inputStream.available();
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return inputStream.read(b, off, len);
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

        }

    }

    public boolean isClusterTest() {
        return isClusterTest;
    }

    public Map<String, String> getTraceContext() {
        return traceContext;
    }
}
