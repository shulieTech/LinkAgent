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
package com.pamirs.attach.plugin.command;

import com.pamirs.attach.plugin.command.handler.DispatcherCommandHandler;
import com.pamirs.pradar.Pradar;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.resource.ModuleCommandInvoker;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import io.shulie.takin.channel.ClientChannel;
import io.shulie.takin.channel.protocal.JsonChannelProtocol;
import io.shulie.takin.channel.router.zk.DefaultClientChannel;
import io.shulie.takin.channel.router.zk.ZkClientConfig;
import io.shulie.takin.channel.type.CustomCommand;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 命令管道插件，负责与控制台之前的命令下发与执行
 *
 * @author wangjian
 * @since 2021/1/5 11:26
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = CommandChannelConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io",
    description = "命令下发模块,负责与控制台下发命令集成")
public class CommandChannelPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private ClientChannel channel;
    @Resource
    private ModuleCommandInvoker commandInvoker;
    @Resource
    private SimulatorConfig simulatorConfig;

    @Override
    public boolean onActive() throws Throwable {
        // 启动时，注册zk
        // zk配置命令参数
        ZkClientConfig config = new ZkClientConfig();
        config.setZkServers(simulatorConfig.getZkServers());
        // 实例化客户端通道
        channel = new DefaultClientChannel()
            .setChannelProtocol(new JsonChannelProtocol())
            .setScheduledExecutorService(Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "Command-Channel-Heartbeat");
                    t.setDaemon(true);
                    return t;
                }
            }))
            .registerTenantAndEnv(simulatorConfig.getProperty(Pradar.TENANT_APP_KEY),
                simulatorConfig.getProperty(Pradar.ENV_CODE))
            .registerHandler(new CustomCommand("DEFAULT_CHANNEL"),
                new DispatcherCommandHandler(commandInvoker))
            .build(config);
        // 注册监听AgendId 客户端唯一标识
        channel.register(Pradar.AGENT_ID_NOT_CONTAIN_USER_INFO);
        return true;
    }

    @Override
    public void onFrozen() throws Throwable {
        if (this.channel != null) {
            this.channel.close();
        }
    }
}
