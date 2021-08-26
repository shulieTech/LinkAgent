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
package com.shulie.instrument.module.log.data.pusher.push.tcp;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarCoreUtils;
import com.pamirs.pradar.remoting.RemotingClient;
import com.pamirs.pradar.remoting.exception.RemotingConnectException;
import com.pamirs.pradar.remoting.exception.RemotingSendRequestException;
import com.pamirs.pradar.remoting.exception.RemotingTimeoutException;
import com.pamirs.pradar.remoting.netty.NettyClientConfigurator;
import com.pamirs.pradar.remoting.netty.NettyRemotingClient;
import com.pamirs.pradar.remoting.protocol.*;
import com.shulie.instrument.module.log.data.pusher.log.callback.LogCallback;
import com.shulie.instrument.module.log.data.pusher.push.DataPusher;
import com.shulie.instrument.module.log.data.pusher.push.ServerOptions;
import com.shulie.instrument.module.log.data.pusher.server.ConnectInfo;
import com.shulie.instrument.module.log.data.pusher.server.ServerAddrProvider;
import io.netty.channel.DefaultFileRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author xiaobin.zfb
 * @since 2020/8/7 9:55 上午
 */
public class TcpDataPusher implements DataPusher {
    private final static Logger LOGGER = LoggerFactory.getLogger(TcpDataPusher.class.getName());
    private ServerOptions serverOptions;
    private AtomicBoolean isStarted = new AtomicBoolean(false);
    /**
     * 重启回调
     */
    private RemotingClient client;
    private ServerAddrProvider provider;
    private ConnectInfo currentConnectInfo;

    public TcpDataPusher() {
    }

    @Override
    public String getName() {
        return "tcp";
    }

    @Override
    public void setServerAddrProvider(ServerAddrProvider provider) {
        this.provider = provider;
    }

    @Override
    public boolean init(ServerOptions serverOptions) {
        this.serverOptions = serverOptions;
        ConnectInfo connectInfo = this.provider.selectConnectInfo();
        if (connectInfo == null) {
            this.isStarted.compareAndSet(true, false);
            return false;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("client start to use log server addr:{} port:{}", connectInfo.getServerAddr(), connectInfo.getPort());
        }
        currentConnectInfo=connectInfo;
        NettyClientConfigurator config = new NettyClientConfigurator();
        ProtocolFactorySelector protocolFactorySelector = new DefaultProtocolFactorySelector();
        client = new NettyRemotingClient(protocolFactorySelector, config);
        return true;
    }

    @Override
    public LogCallback buildLogCallback() {
        return new LogCallback() {
            /**
             * 请求参数可以复用,因为这个不存在并发的问题，可以减少一些请求对象的产生
             */
            private RemotingCommand requestCommand = new RemotingCommand();

            @Override
            public boolean call(FileChannel fc, long position, long length, byte dataType, int version) {
                if (!isStarted.get()) {
                    return false;
                }
                try {
                    requestCommand.refreshOpaque();
                    requestCommand.setCode(CommandCode.SUCCESS);
                    requestCommand.setVersion(CommandVersion.V2);
                    requestCommand.setProtocolCode(serverOptions.getProtocolCode());
                    requestCommand.setDataType(dataType);
                    requestCommand.setIp(PradarCoreUtils.getLocalAddressNumber());
                    requestCommand.setDataVersion(version);
                    requestCommand.setEncodeType(EncoderType.of(Pradar.DEFAULT_CHARSET.name()).getEncoderType());
                    requestCommand.setLength((int) length);
                    requestCommand.setFile(new DefaultFileRegion(fc, position, length));
                    RemotingCommand responseCommand = client.invokeSync(currentConnectInfo.getAddr(), requestCommand, serverOptions.getTimeout());
                    if (responseCommand.getCode() == CommandCode.SUCCESS) {
                        return true;
                    } else if (responseCommand.getCode() == CommandCode.SYSTEM_ERROR) {
                        return false;
                    } else if (responseCommand.getCode() == CommandCode.SYSTEM_BUSY) {
                        provider.errorConnectInfo(currentConnectInfo);
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("log server is busy {}. attempt to choose another log server.", currentConnectInfo.getAddr());
                        }
                        ConnectInfo c = provider.selectConnectInfo();
                        if (c != null) {
                            currentConnectInfo = c;
                        }
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("log server changed to connect host {}.", currentConnectInfo.getAddr());
                        }
                        return false;
                    } else if (responseCommand.getCode() == CommandCode.COMMAND_CODE_NOT_SUPPORTED) {
                        return false;
                    }
                } catch (InterruptedException e) {
                    return false;
                } catch (RemotingConnectException e) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("client send request to log server {} ,can't connect to server. attempt to choose another log server.", currentConnectInfo.getAddr(), e);
                    }
                    provider.errorConnectInfo(currentConnectInfo);
                    ConnectInfo connectInfo = provider.selectConnectInfo();
                    if (connectInfo != null) {
                        currentConnectInfo = connectInfo;
                    }
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("log server changed to connect host {}.", currentConnectInfo.getAddr());
                    }
                    return false;
                } catch (RemotingSendRequestException e) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("client send request to log server {} occur RemotingSendRequestException. attempt to choose another log server.", currentConnectInfo.getAddr(), e);
                    }
                    provider.errorConnectInfo(currentConnectInfo);
                    ConnectInfo connectInfo = provider.selectConnectInfo();
                    if (connectInfo != null) {
                        currentConnectInfo = connectInfo;
                    }
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("log server changed to connect host {}.", currentConnectInfo.getAddr());
                    }
                    return false;
                } catch (RemotingTimeoutException e) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("client send request to log server {} timeout. attempt to choose another log server.", currentConnectInfo.getAddr(), e);
                    }
                    provider.errorConnectInfo(currentConnectInfo);
                    ConnectInfo connectInfo = provider.selectConnectInfo();
                    if (connectInfo != null) {
                        currentConnectInfo = connectInfo;
                    }
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("log server changed to connect host {}.", currentConnectInfo.getAddr());
                    }
                    return false;
                } finally {
                    requestCommand.setFile(null);
                    requestCommand.setBody(null);
                }
                return false;
            }
        };
    }

    @Override
    public boolean start() {
        if (!isStarted.compareAndSet(false, true)) {
            return true;
        }
        try {
            client.start();
        } catch (Throwable e) {
            LOGGER.error("start log server push client err! host:{} ", currentConnectInfo.getAddr(), e);
            return false;
        }
        return true;
    }

    @Override
    public void stop() {
        if (!isStarted.compareAndSet(true, false)) {
            return;
        }
        try {
            this.client.shutdownSync();
        } catch (Throwable e) {
            LOGGER.error("close client err! host:{} ", currentConnectInfo.getAddr(), e);
        }
    }

}
