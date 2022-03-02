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
package com.shulie.instrument.module.log.data.pusher.push.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.shulie.instrument.module.log.data.pusher.log.reader.impl.LogPusher;
import com.shulie.instrument.module.log.data.pusher.log.reader.impl.LogPusherOptions;
import com.shulie.instrument.module.log.data.pusher.push.DataPushManager;
import com.shulie.instrument.module.log.data.pusher.push.DataPusher;
import com.shulie.instrument.module.log.data.pusher.push.ServerOptions;
import com.shulie.instrument.module.log.data.pusher.push.http.HttpDataPusher;
import com.shulie.instrument.module.log.data.pusher.push.tcp.TcpDataPusher;
import com.shulie.instrument.module.log.data.pusher.server.PusherOptions;
import com.shulie.instrument.module.log.data.pusher.server.ServerAddrProvider;
import com.shulie.instrument.module.log.data.pusher.server.ServerProviderOptions;
import com.shulie.instrument.module.log.data.pusher.server.impl.DefaultServerAddrProvider;
import com.shulie.instrument.module.register.zk.impl.ZkClientSpec;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xiaobin.zfb
 * @since 2020/8/11 5:45 下午
 */
public class DefaultDataPushManager implements DataPushManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultDataPushManager.class.getName());
    private Map<String, DataPusher> dataPushers = new HashMap<String, DataPusher>();
    private DataPusher dataPusher;
    private LogPusher logPusher;
    private ServerAddrProvider provider;
    private PusherOptions pusherOptions;
    private AtomicBoolean isStarted = new AtomicBoolean(false);

    public DefaultDataPushManager(final PusherOptions pusherOptions) {
        loadDataPushers();
        this.pusherOptions = pusherOptions;
        ServerProviderOptions serverProviderOptions = new ServerProviderOptions();
        serverProviderOptions.setServerZkPath(pusherOptions.getServerZkPath());

        ZkClientSpec zkClientSpec = new ZkClientSpec();
        zkClientSpec.setZkServers(pusherOptions.getZkServers());
        zkClientSpec.setConnectionTimeoutMillis(pusherOptions.getConnectionTimeoutMillis());
        zkClientSpec.setSessionTimeoutMillis(pusherOptions.getSessionTimeoutMillis());
        zkClientSpec.setThreadName("dataPusher");
        serverProviderOptions.setSpec(zkClientSpec);
        this.provider = new DefaultServerAddrProvider(serverProviderOptions);
        this.dataPusher = dataPushers.get(pusherOptions.getDataPusher());
        if (this.dataPusher == null) {
            LOGGER.error("can't found log data pusher with name:{}", pusherOptions.getDataPusher());
            return;
        }
    }

    private void loadDataPushers() {
        try {
            DataPusher dataPusher = new TcpDataPusher();
            DataPusher httpDataPusher = new HttpDataPusher();
            dataPushers.put(dataPusher.getName(), dataPusher);
            dataPushers.put(httpDataPusher.getName(), httpDataPusher);
        } catch (Throwable e) {
            LOGGER.error("load log data pusher err!", e);
        }
    }

    /**
     * 添加重试启动任务
     */
    private void addRetryStartTask() {
        ExecutorServiceFactory.getFactory().schedule(new Runnable() {
            @Override
            public void run() {
                boolean isSuccess = start0();
                if (!isSuccess) {
                    ExecutorServiceFactory.getFactory().schedule(this, 5, TimeUnit.SECONDS);
                } else {
                    isStarted.compareAndSet(false, true);
                }
            }
        }, 0, TimeUnit.MICROSECONDS);
    }

    /**
     * 启动
     *
     * @return
     */
    private boolean start0() {
        if (!this.isStarted.compareAndSet(false, true)) {
            return true;
        }
        try {
            final ServerOptions serverOptions = new ServerOptions();
            serverOptions.setTimeout(this.pusherOptions.getTimeout());
            serverOptions.setProtocolCode(this.pusherOptions.getProtocolCode());
            serverOptions.setMaxHttpPoolSize(this.pusherOptions.getMaxHttpPoolSize());
            serverOptions.setHttpPath(this.pusherOptions.getHttpPath());
            dataPusher.setServerAddrProvider(provider);
            boolean isSuccess = dataPusher.init(serverOptions);
            if (!isSuccess) {
                this.isStarted.compareAndSet(true, false);
                LOGGER.error("init log data pusher failed.retry next times. {}", serverOptions);
                return false;
            }

            isSuccess = dataPusher.start();
            if (!isSuccess) {
                this.isStarted.compareAndSet(true, false);
                LOGGER.error("start log data pusher failed.retry next times.");
                return false;
            }

            if (CollectionUtils.isEmpty(pusherOptions.getLogPusherOptions())) {
                LOGGER.error("start log data pusher options is blank. ");
                return false;
            }

            List<LogPusherOptions> logPusherOptionsList = pusherOptions.getLogPusherOptions();
            for (LogPusherOptions logPusherOptions : logPusherOptionsList) {
                logPusherOptions.setLogCallback(dataPusher.buildLogCallback());
            }

            this.logPusher = new LogPusher(logPusherOptionsList);
            logPusher.start();

        } catch (Throwable e) {
            this.isStarted.compareAndSet(true, false);
            LOGGER.error("start log data pusher failed. retry next times", e);
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        if (dataPusher == null) {
            return;
        }
        boolean isSuccess = start0();
        if (!isSuccess) {
            addRetryStartTask();
        }
    }

    @Override
    public void stop() {
        if (this.dataPusher == null) {
            return;
        }
        if (!isStarted.compareAndSet(true, false)) {
            return;
        }
        this.logPusher.stop();
        this.dataPusher.stop();
        this.provider.release();
        dataPushers.clear();
    }

}
