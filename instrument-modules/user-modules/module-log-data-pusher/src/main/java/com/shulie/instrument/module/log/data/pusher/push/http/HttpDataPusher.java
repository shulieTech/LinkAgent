/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shulie.instrument.module.log.data.pusher.push.http;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.pamirs.pradar.PradarCoreUtils;
import com.pamirs.pradar.remoting.protocol.CommandCode;
import com.shulie.instrument.module.log.data.pusher.enums.DataPushEnum;
import com.shulie.instrument.module.log.data.pusher.log.callback.LogCallback;
import com.shulie.instrument.module.log.data.pusher.push.DataPusher;
import com.shulie.instrument.module.log.data.pusher.push.ServerOptions;
import com.shulie.instrument.module.log.data.pusher.server.HttpPushOptions;
import com.shulie.instrument.module.log.data.pusher.server.ServerAddrProvider;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/3/1 2:12 下午
 */
public class HttpDataPusher implements DataPusher {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpDataPusher.class.getName());

    private CloseableHttpClient httpClient = null;
    private AtomicBoolean isStarted = new AtomicBoolean(false);
    private final HttpPushOptions httpPushOptions;

    /**
     * 日志上传接口
     */
    private final String url = "/log/link/upload";

    /**
     * 服务探活接口
     */
    private final String healthCheckUrl = "/health";

    /**
     * 本机IP
     */
    private String hostIp;

    public HttpDataPusher(HttpPushOptions httpOptions) {
        this.httpPushOptions = httpOptions;
    }

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public DataPushEnum getType() {
        return DataPushEnum.HTTP;
    }

    @Override
    public void setServerAddrProvider(ServerAddrProvider provider) {
        // do nothing
    }

    @Override
    public boolean init(ServerOptions serverOptions) {
        try {
            if (StringUtil.isEmpty(httpPushOptions.getHttpPath())) {
                LOGGER.error(
                    "File: 'simulator-agent/agent/simulator/config/simulator.properties',"
                        + "config: 'pradar.push.server.http.path' is empty!!");
                return false;
            }
            hostIp = PradarCoreUtils.getLocalAddress();
            final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
            // 总连接池数量
            connectionManager.setMaxTotal(httpPushOptions.getMaxHttpPoolSize());
            // setConnectTimeout表示设置建立连接的超时时间
            // setConnectionRequestTimeout表示从连接池中拿连接的等待超时时间
            // setSocketTimeout表示发出请求后等待对端应答的超时时间
            RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(serverOptions.getTimeout())
                .setConnectionRequestTimeout(serverOptions.getTimeout())
                .setSocketTimeout(serverOptions.getTimeout())
                .build();
            // 重试处理器，StandardHttpRequestRetryHandler这个是官方提供的，看了下感觉比较挫，很多错误不能重试，可自己实现HttpRequestRetryHandler接口去做
            HttpRequestRetryHandler retryHandler = new StandardHttpRequestRetryHandler();

            httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(retryHandler)
                .build();

            // 服务端假设关闭了连接，对客户端是不透明的，HttpClient为了缓解这一问题，在某个连接使用前会检测这个连接是否过时，如果过时则连接失效，但是这种做法会为每个请求
            // 增加一定额外开销，因此有一个定时任务专门回收长时间不活动而被判定为失效的连接，可以某种程度上解决这个问题
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // 关闭失效连接并从连接池中移除
                        connectionManager.closeExpiredConnections();
                        // 关闭60秒钟内不活动的连接并从连接池中移除，空闲时间从交还给连接管理器时开始
                        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
                    } catch (Throwable t) {
                        LOGGER.error("closeExpiredConnections error", t);
                    }
                }
            }, 0, 1000 * 5);
            return true;
        } catch (Throwable e) {
            LOGGER.error("httpDataPush init error", e);
            return false;
        }

    }

    @Override
    public LogCallback buildLogCallback() {
        return new LogCallback() {
            @Override
            public boolean call(FileChannel fc, long position, long length, byte dataType, int version) {
                long start = System.currentTimeMillis();
                if (!isStarted.get()) {
                    return false;
                }
                try {
                    HttpPost httpPost = new HttpPost(httpPushOptions.getHttpPath() + url);
                    httpPost.setHeader("Content-Type", "application/json");
                    httpPost.setHeader("time", String.valueOf(System.currentTimeMillis()));
                    httpPost.setHeader("dataType", String.valueOf(dataType));
                    httpPost.setHeader("version", String.valueOf(version));
                    httpPost.setHeader("hostIp", hostIp);
                    httpPost.setHeader("Accept-Encoding", "gzip,deflate,sdch");

                    MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, position, length);
                    byte[] data = new byte[bb.remaining()];
                    bb.get(data);
                    ByteArrayEntity byteArrayEntity = new ByteArrayEntity(data);

                    if (httpPushOptions.isEnableGzip()) {
                        GzipCompressingEntity gzipEntity = new GzipCompressingEntity(byteArrayEntity);
                        httpPost.setEntity(gzipEntity);
                    } else {
                        httpPost.setEntity(byteArrayEntity);
                    }

                    CloseableHttpResponse response = httpClient.execute(httpPost);

                    int httpCode = response.getStatusLine().getStatusCode();
                    if (httpCode != HttpStatus.SC_OK) {
                        return false;
                    }
                    String content = EntityUtils.toString(response.getEntity());
                    if (StringUtil.isEmpty(content)) {
                        return false;
                    }
                    JSONObject jsonObject = JSON.parseObject(content);
                    int responseCode = jsonObject.getIntValue("responseCode");
                    if (responseCode == CommandCode.SUCCESS) {
                        return true;
                    } else if (responseCode == CommandCode.SYSTEM_ERROR
                        || responseCode == CommandCode.SYSTEM_BUSY
                        || responseCode == CommandCode.COMMAND_CODE_NOT_SUPPORTED) {
                        return false;
                    }
                } catch (Throwable e) {
                    LOGGER.error("http log push error", e);
                } finally {
                    long end = System.currentTimeMillis();
                    writeFile(
                        String.format("date:%s, time:%d, type:%d, length:%d\n", sdf.format(new Date()),
                            end - start, dataType, length), dataType);
                }
                return false;
            }
        };
    }

    @Override
    public boolean start() {
        if (httpClient == null) {
            return false;
        }

        // 探活
        HttpGet httpGet = new HttpGet(httpPushOptions.getHttpPath() + healthCheckUrl);
        try {
            CloseableHttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                LOGGER.error("http health check error, url {}", httpPushOptions.getHttpPath() + healthCheckUrl);
                return false;
            }
        } catch (Throwable e) {
            LOGGER.error("http health check error", e);
            return false;
        }

        return isStarted.compareAndSet(false, true);
    }

    @Override
    public void stop() {
        if (!isStarted.compareAndSet(true, false)) {
            return;
        }
        try {
            this.httpClient.close();
        } catch (Throwable e) {
            LOGGER.error("close httpClient err!", e);
        }
    }

    private void writeFile(String str, int dataType) {
        try {
            BufferedWriter out = new BufferedWriter(
                new FileWriter("/Users/ocean_wll/httpPerf-" + dataType + ".txt", true));
            out.write(str);
            out.close();
        } catch (Exception e) {
            // ignore
        }
    }
}
