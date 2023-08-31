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
package com.shulie.instrument.simulator.agent.api.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * 命令包
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/5/22 11:55 上午
 */
public class CommandPacket extends HeartCommandPacket {
    public final static int COMMAND_TYPE_FRAMEWORK = 1;
    public final static int COMMAND_TYPE_MODULE = 2;

    public final static int OPERATE_TYPE_INSTALL = 1;
    public final static int OPERATE_TYPE_UNINSTALL = 2;
    public final static int OPERATE_TYPE_UPGRADE = 3;

    private static final Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

    /**
     * 无任何操作的命令包
     */
    public final static CommandPacket NO_ACTION_PACKET = noActionCommandPacket();
    /**
     * 命令 ID
     */
    private long id;

    /**
     * 命令类型 1：框架命令 2：模块命令
     */
    private int commandType;

    /**
     * 操作类型 1: 安装 2:卸载 3:升级
     */
    private int operateType;

    /**
     * 是否使用本地模式
     */
    private boolean useLocal;

    /**
     * 数据包地址，支持 http/https/文件
     */
    private String dataPath;

    /**
     * 命令发出时间
     */
    private long commandTime;

    /**
     * 命令存活时长,默认无限长
     */
    private int liveTime = -1;

    /**
     * 附加数据
     */
    private Map<String, Object> extras = Collections.EMPTY_MAP;

    public boolean isUseLocal() {
        return useLocal;
    }

    public void setUseLocal(boolean useLocal) {
        this.useLocal = useLocal;
    }

    public int getLiveTime() {
        return liveTime;
    }

    public void setLiveTime(int liveTime) {
        this.liveTime = liveTime;
    }

    public long getCommandTime() {
        return commandTime;
    }

    public void setCommandTime(long commandTime) {
        this.commandTime = commandTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCommandType() {
        return commandType;
    }

    public void setCommandType(int commandType) {
        this.commandType = commandType;
    }

    public int getOperateType() {
        return operateType;
    }

    public void setOperateType(int operateType) {
        this.operateType = operateType;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public Map<String, Object> getExtras() {
        if (extras.isEmpty() && null != getExtrasString() && !"".equals(getExtrasString())){
            extras = gson.fromJson(getExtrasString(), Map.class);
        }
        return extras;
    }

    public Object getExtra(String key) {
        return extras != null ? extras.get(key) : null;
    }

    public void setExtras(Map<String, Object> extras) {
        this.extras = extras;
    }

    public static CommandPacket noActionCommandPacket() {
        CommandPacket commandPacket = new CommandPacket();
        commandPacket.setId(-1);
        commandPacket.setCommandType(COMMAND_TYPE_FRAMEWORK);
        commandPacket.setOperateType(OPERATE_TYPE_INSTALL);
        return commandPacket;
    }
}
