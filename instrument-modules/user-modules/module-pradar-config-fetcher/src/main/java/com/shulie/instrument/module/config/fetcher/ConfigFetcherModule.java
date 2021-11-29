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
package com.shulie.instrument.module.config.fetcher;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.pamirs.pradar.internal.PradarInternalService;
import com.shulie.instrument.module.config.fetcher.config.ConfigManager;
import com.shulie.instrument.module.config.fetcher.config.DefaultConfigFetcher;
import com.shulie.instrument.module.config.fetcher.config.event.model.*;
import com.shulie.instrument.module.config.fetcher.config.resolver.zk.ZookeeperOptions;
import com.shulie.instrument.module.config.fetcher.utils.CommandTaskStatus;
import com.shulie.instrument.module.config.fetcher.utils.FtpUtils;
import com.shulie.instrument.module.config.fetcher.utils.OssUtil;
import com.shulie.instrument.simulator.api.CommandResponse;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.annotation.Command;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import com.shulie.instrument.simulator.api.resource.SwitcherManager;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/1 12:45 上午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = ConfigFetcherConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io", description = "配置拉取模块,定时1分钟拉取一次配置", switchAuto = false)
public class ConfigFetcherModule extends ModuleLifecycleAdapter implements ExtensionModule {
    private final static Logger logger = LoggerFactory.getLogger(ConfigFetcherModule.class);
    private volatile boolean isActive;

    @Resource
    private SimulatorConfig simulatorConfig;

    @Resource
    private SwitcherManager switcherManager;

    private ScheduledFuture future;

    private ConfigManager configManager;


    private final String checkStorageCommandId = "100100";


    private final String commandId_key = "commandId";
    private final String taskId_key = "taskId";
    private volatile ConcurrentHashMap<String, Future<CommandResponse<Object>>> taskKeySet = new ConcurrentHashMap<String, Future<CommandResponse<Object>>>(16, 1);


    private String generateTaskKey(String commandId, String taskId){
        return commandId + ":" + taskId;
    }

    public final ExecutorService commandExecutorService = new ThreadPoolExecutor(1, 3,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(10), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("command-executor-" + t.getId());
            return t;
        }
    });






    @Command("getCommandResult")
    public CommandResponse<Object> getCommandResult(Map<String, String> args){
        CommandResponse commandResponse = new CommandResponse();
        commandResponse.setSuccess(true);
        String requests = args.get("requests");
        if (StringUtils.isBlank(requests)){
            commandResponse.setMessage("requests is null");
            return commandResponse;
        }
        List<String> list = JSON.parseObject(args.get("requests"), List.class);
        ConcurrentHashMap<String, Future<CommandResponse<Object>>> temp = taskKeySet;

        if (temp.isEmpty()){
            commandResponse.setMessage("当前没有任务可供查询");
            commandResponse.setResult(Collections.EMPTY_MAP);
            return commandResponse;
        }
        List<String> successList = new ArrayList<String>();
        Map<String, String> failedMap = new HashMap<String, String>();
        Set<String> removeList = new HashSet<String>();
        for (String s : list){
            Future<CommandResponse<Object>> future = temp.get(s);
            if (future.isDone()){
                try {
                    CommandResponse<Object> c = future.get();
                    if (CommandTaskStatus.FINISHED.name().equals(c.getCommandStatus())){
                        successList.add(s);
                        removeList.add(s);
                    }
                    if (CommandTaskStatus.FAILED.name().equals(c.getCommandStatus())){
                        failedMap.put(s, c.getMessage());
                        removeList.add(s);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    //TODO
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    //TODO
                }
            }
        }
        Map<String, Object> map = new HashMap<String, Object>(2, 1);
        map.put("success", successList);
        map.put("failed", failedMap);
        commandResponse.setResult(map);
        commandResponse.setCommandStatus(CommandTaskStatus.FINISHED.name());
        for (String rk : removeList){
            taskKeySet.remove(rk);
        }
        return commandResponse;
    }


    /**
     *
     * @param args
     * @return
     */
    @Command("doCommand")
    public CommandResponse<Object> doCommand(Map<String, String> args) {
        String commandId = args.get(commandId_key);
        String taskId = args.get(taskId_key);
        String sync = args.get("sync");
        CommandResponse<Object> commandResponse = new CommandResponse();
        if (StringUtils.isBlank(commandId) || StringUtils.isBlank(taskId) || StringUtils.isBlank(sync)){
            commandResponse.setSuccess(true);
            commandResponse.setMessage("任务缺少必要参数，commandId, taskId, sync!");
            commandResponse.setResult("false");
            return commandResponse;
        }
        String taskKey = generateTaskKey(commandId, taskId);
        try {
            if (taskKeySet.get(taskKey) != null){
                return replayExistsCommandTask(commandResponse, taskKey);
            }
            synchronized (taskKeySet){
                if (taskKeySet.get(taskKey) != null){
                    return replayExistsCommandTask(commandResponse, taskKey);
                } else {
                    if (Boolean.valueOf(sync)){
                        CommandTask commandTask = new CommandTask(args);
                        return commandTask.call();
                    } else {
                        CommandTask commandTask = new CommandTask(args);
                        Future<CommandResponse<Object>> commandTaskfuture = null;
                        try {
                            commandTaskfuture = commandExecutorService.submit(commandTask);
                            taskKeySet.put(taskKey, commandTaskfuture);
                            commandResponse.setSuccess(true);
                            commandResponse.setMessage("提交的为异步任务，当前正在运行，请发起查询请求获取结果!");
                            commandResponse.setCommandStatus(CommandTaskStatus.INEXECUTE.name());
                            return commandResponse;
                        }catch (Throwable e){
                            if (commandTaskfuture != null){
                                commandTaskfuture.cancel(true);
                            }
                            commandResponse.setSuccess(true);
                            commandResponse.setMessage(e.getMessage());
                            commandResponse.setCommandStatus(CommandTaskStatus.FAILED.name());
                            return commandResponse;
                        }
                    }
                }
            }
        }catch (Throwable e){
            commandResponse.setSuccess(true);
            commandResponse.setCommandStatus(CommandTaskStatus.FAILED.name());
            commandResponse.setMessage(e.getMessage());
            return commandResponse;
        }
    }

    private CommandResponse<Object> replayExistsCommandTask(CommandResponse<Object> commandResponse, String taskKey) throws ExecutionException, InterruptedException {
        commandResponse.setSuccess(true);
        commandResponse.setMessage("当前任务有正在运行的,请勿提交相同任务!");
        commandResponse.setCommandStatus(CommandTaskStatus.INEXECUTE.name());
        return commandResponse;
    }

    private class CommandTask implements Callable <CommandResponse<Object>>{

        private final Map<String, String> commandArgs;
        public CommandTask(Map<String, String> map){
            commandArgs = map;
        }

        @Override
        public CommandResponse<Object> call() throws Exception {
            //校验oss、ftp地址是否可用
            if (checkStorageCommandId.equals(commandArgs.get(commandId_key))){
                CommandResponse<Object> commandResponse = new CommandResponse<Object>();
                commandResponse.setSuccess(true);
                return checkStorage(commandResponse, commandArgs.get("extrasString"));
            }
            return null;
        }
    }






    private CommandResponse checkStorage(CommandResponse commandResponse, String extrasString){
        try {
            Map<String, Object> map = JSON.parseObject(extrasString, Map.class);
            String salt = (String) map.get("salt");
            String context = (String) map.get("context");
            Integer pathType = (Integer) map.get("pathType");
            if (StringUtils.isBlank(salt) || StringUtils.isBlank(context) || null == pathType){
                throw new IllegalArgumentException("ftp参数不完整");
            }
            Map<String, Object> storageMap = JSON.parseObject(context, Map.class);
            switch (pathType){
                case 1:
                    String basePath = (String) storageMap.get("basePath");
                    String ftpHost = (String) storageMap.get("ftpHost");
                    Integer ftpPort = (Integer) storageMap.get("ftpPort");
                    String passwd = (String)storageMap.get("passwd");
                    String username = (String) storageMap.get("username");
                    String s = SecureUtil.aes(salt.getBytes()).decryptStr(passwd);
                    FtpUtils.checkFtp(ftpHost, Integer.valueOf(ftpPort), username, s, basePath);
                    commandResponse.setCommandStatus(CommandTaskStatus.FINISHED.name());
                    break;
                case 0:
                    String accessKeyIdTemp = (String) storageMap.get("accessKeyId");
                    String accessKeySecretTemp = (String) storageMap.get("accessKeySecret");
                    String endpoint = (String) storageMap.get("endpoint");
                    String bucketName = (String) storageMap.get("bucketName");
                    String accessKeyId = SecureUtil.aes(salt.getBytes()).decryptStr(accessKeyIdTemp);
                    String accessKeySecret = SecureUtil.aes(salt.getBytes()).decryptStr(accessKeySecretTemp);
                    OssUtil.checkOss(endpoint, accessKeyId, accessKeySecret, bucketName);
                    commandResponse.setCommandStatus(CommandTaskStatus.FINISHED.name());
                    break;
                default:
                    commandResponse.setCommandStatus(CommandTaskStatus.FAILED.name());
                    commandResponse.setMessage("unknown pathType:" + pathType);
            }
            return commandResponse;
        }catch (Throwable e){
            commandResponse.setCommandStatus(CommandTaskStatus.FAILED.name());
            commandResponse.setResult(false);
            commandResponse.setMessage("checkStorage 异常" + e.getMessage());
            return commandResponse;
        }


    }

    private ZookeeperOptions buildZookeeperOptions() {
        ZookeeperOptions zookeeperOptions = new ZookeeperOptions();
        zookeeperOptions.setName("zookeeper");
        zookeeperOptions.setZkServers(simulatorConfig.getZkServers());
        zookeeperOptions.setConnectionTimeoutMillis(simulatorConfig.getZkConnectionTimeout());
        zookeeperOptions.setSessionTimeoutMillis(simulatorConfig.getZkSessionTimeout());
        return zookeeperOptions;
    }

    @Override
    public boolean onActive() throws Throwable {
        isActive = true;
        this.future = ExecutorServiceFactory.getFactory().schedule(new Runnable() {
            @Override
            public void run() {
                if (!isActive) {
                    return;
                }
                try {
                    int interval = simulatorConfig.getIntProperty("pradar.config.fetch.interval", 60);
                    String unit = simulatorConfig.getProperty("pradar.config.fetch.unit", "SECONDS");
                    TimeUnit timeUnit = TimeUnit.valueOf(unit);
                    configManager = ConfigManager.getInstance(switcherManager, interval, timeUnit);
                    configManager.initAll();
                } catch (Throwable e) {
                    logger.warn("SIMULATOR: Config Fetch module start failed. log data can't push to the server.", e);
                    future = ExecutorServiceFactory.getFactory().schedule(this, 5, TimeUnit.SECONDS);
                }
            }
        }, 10, TimeUnit.SECONDS);

        PradarInternalService.registerConfigFetcher(new DefaultConfigFetcher());
        return true;
    }

    @Override
    public void onFrozen() throws Throwable {
        isActive = false;
        if (configManager != null) {
            configManager.destroy();
        }
        if (this.future != null && !this.future.isDone() && !this.future.isCancelled()) {
            this.future.cancel(true);
        }
    }

    @Override
    public void onUnload() throws Throwable {
        PradarInternalService.registerConfigFetcher(null);
        CacheKeyAllowList.release();
        ContextPathBlockList.release();
        EsShadowServerConfig.release();
        GlobalSwitch.release();
        MaxRedisExpireTime.release();
        MockConfigChanger.release();
        MQWhiteList.release();
        RedisShadowServerConfig.release();
        RpcAllowList.release();
        SearchKeyWhiteList.release();
        ShadowDatabaseConfigs.release();
        ShadowHbaseConfigs.release();
        ShadowJobConfig.release();
        UrlWhiteList.release();
        WhiteListSwitch.release();
    }
}
