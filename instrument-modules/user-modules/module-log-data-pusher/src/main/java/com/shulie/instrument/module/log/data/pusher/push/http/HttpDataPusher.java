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

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/3/1 2:12 ??????
 */
public class HttpDataPusher implements DataPusher {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpDataPusher.class.getName());

    private CloseableHttpClient httpClient = null;
    private AtomicBoolean isStarted = new AtomicBoolean(false);
    private final HttpPushOptions httpPushOptions;

    /**
     * ??????????????????
     */
    private final String url = "/log/link/upload";

    /**
     * ??????????????????
     */
    private final String healthCheckUrl = "/health";

    /**
     * ??????IP
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
            // ??????????????????
            connectionManager.setMaxTotal(httpPushOptions.getMaxHttpPoolSize());
            connectionManager.setDefaultMaxPerRoute(httpPushOptions.getMaxHttpPoolSize());
            // setConnectTimeout???????????????????????????????????????
            // setConnectionRequestTimeout???????????????????????????????????????????????????
            // setSocketTimeout??????????????????????????????????????????????????????
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(serverOptions.getTimeout())
                    .setConnectionRequestTimeout(serverOptions.getTimeout())
                    .setSocketTimeout(serverOptions.getTimeout())
                    .build();
            // ??????????????????StandardHttpRequestRetryHandler????????????????????????????????????????????????????????????????????????????????????????????????HttpRequestRetryHandler????????????
            HttpRequestRetryHandler retryHandler = new StandardHttpRequestRetryHandler();

            httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    .setRetryHandler(retryHandler)
                    .build();

            // ???????????????????????????????????????????????????????????????HttpClient?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            // ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // ??????????????????????????????????????????
                        connectionManager.closeExpiredConnections();
                        // ??????60??????????????????????????????????????????????????????????????????????????????????????????????????????
                        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
                    } catch (Throwable t) {
                        LOGGER.error("closeExpiredConnections error", t);
                    }
                }
            }, 0, 1000 * 5);

            // jvm ??????????????????????????????????????????????????????
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        httpClient.close();
                    } catch (IOException e) {
                        LOGGER.error("HttpClient close exception", e);
                    }
                }
            });
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
                //long start = System.currentTimeMillis();
                if (!isStarted.get()) {
                    return false;
                }
                CloseableHttpResponse response = null;
                try {
                    HttpPost httpPost = new HttpPost(httpPushOptions.getHttpPath() + url);
                    httpPost.setHeader("Content-Type", "application/json");
                    httpPost.setHeader("time", String.valueOf(System.currentTimeMillis()));
                    httpPost.setHeader("dataType", String.valueOf(dataType));
                    httpPost.setHeader("version", String.valueOf(version));
                    httpPost.setHeader("hostIp", hostIp);

                    MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, position, length);
                    byte[] data = new byte[bb.remaining()];
                    bb.get(data);
                    ByteArrayEntity byteArrayEntity = new ByteArrayEntity(data);

                    if (httpPushOptions.isEnableGzip()) {
                        httpPost.setHeader("Accept-Encoding", "gzip,deflate,sdch");
                        GzipCompressingEntity gzipEntity = new GzipCompressingEntity(byteArrayEntity);
                        httpPost.setEntity(gzipEntity);
                    } else {
                        httpPost.setEntity(byteArrayEntity);
                    }

                    response = httpClient.execute(httpPost);

                    if (response == null) {
                        return false;
                    }

                    int httpCode = response.getStatusLine().getStatusCode();
                    if (httpCode != HttpStatus.SC_OK) {
                        return false;
                    }
                    String content = EntityUtils.toString(response.getEntity());
                    if (StringUtil.isEmpty(content)) {
                        return false;
                    }
                    JSONObject jsonObject = JSON.parseObject(content);
                    Integer responseCode = jsonObject.getInteger("responseCode");
                    if (responseCode == CommandCode.SUCCESS) {
                        return true;
                    }
                } catch (Throwable e) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("http log push error", e);
                    }
                    if (response != null) {
                        try {
                            EntityUtils.consume(response.getEntity());
                        } catch (Exception e1) {
                            LOGGER.error("callback consume response entity exception", e);
                        }
                    }
                }
                //finally {
                //    long end = System.currentTimeMillis();
                //    writeFile(
                //        String.format("date:%s, time:%d, type:%d, length:%d\n", sdf.format(new Date()),
                //            end - start, dataType, length), dataType);
                //}
                return false;
            }
        };
    }

    @Override
    public boolean start() {
        if (httpClient == null) {
            return false;
        }

        // ??????
        HttpGet httpGet = new HttpGet(httpPushOptions.getHttpPath() + healthCheckUrl);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                LOGGER.error("http health check error, url {}", httpPushOptions.getHttpPath() + healthCheckUrl);
                return false;
            }
        } catch (Throwable e) {
            LOGGER.error("http health check error", e);
            return false;
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    LOGGER.error("start consume response entity exception", e);
                }
            }
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
    //
    //private void writeFile(String str, int dataType) {
    //    try {
    //        BufferedWriter out = new BufferedWriter(
    //            new FileWriter("/Users/ocean_wll/httpPerf-" + dataType + ".txt", true));
    //        out.write(str);
    //        out.close();
    //    } catch (Exception e) {
    //        // ignore
    //    }
    //}
}
