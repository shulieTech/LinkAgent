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
package com.pamirs.pradar.internal.config;

import java.util.List;

/**
 * 影子 redis 的配置
 *
 * @Author qianfan
 * @package: com.pamirs.attach.plugin.lettuce.factory
 * @Date 2020/11/26 1:24 下午
 */
public class ShadowRedisConfig {

    enum Client {
        jedis,
        lettuce,
        redisson
    }

    enum Mode {
        single,
        cluster,
        sentinel,
        masterSlave,
        replicated;
    }

    /**
     * redis 节点列表
     */
    private String nodes;
    /**
     *
     */
    private List<String> nodeNums;
    /**
     * 密码
     */
    private String password;
    /**
     * 库
     */
    private Integer database;

    /**
     * master 地址
     */
    private String master;

    /**
     * 影子库账号前缀
     */
    private String shadowAccountPrefix;

    /**
     * 影子库账号后缀
     */
    private String shadowAccountSuffix;

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean match(Mode mode, Client client) {
        return mode.name().equals(getModel()) && client.name().equals(getClient());
    }

    /**
     * 客户端
     */
    private String client;
    /**
     * 模式
     */
    private String model;


    public String getMaster() {
        return master;
    }

    public void setMaster(String master) {
        this.master = master;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public String getPassword(String bizPassword) {
        if (stringIsEmpty(bizPassword) || !stringIsEmpty(password)) {
            return password;
        }
        return this.shadowAccountPrefix + bizPassword + this.shadowAccountSuffix;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getDatabase() {
        return database;
    }

    public void setDatabase(Integer database) {
        this.database = database;
    }

    public List<String> getNodeNums() {
        return nodeNums;
    }

    public void setNodeNums(List<String> nodeNums) {
        this.nodeNums = nodeNums;
    }

    public String getShadowAccountPrefix() {
        return shadowAccountPrefix;
    }

    public void setShadowAccountPrefix(String shadowAccountPrefix) {
        this.shadowAccountPrefix = shadowAccountPrefix;
    }

    public String getShadowAccountSuffix() {
        return shadowAccountSuffix;
    }

    public void setShadowAccountSuffix(String shadowAccountSuffix) {
        this.shadowAccountSuffix = shadowAccountSuffix;
    }

    @Override
    public boolean equals(Object obj) {
        ShadowRedisConfig that = (ShadowRedisConfig) obj;
        if (this == that) {
            return true;
        }
        return String.valueOf(this.nodes).equals(String.valueOf(that.nodes))
                && String.valueOf(password).equals(String.valueOf(that.password))
                && String.valueOf(this.database).equals(String.valueOf(that.database))
                && String.valueOf(master).equals(String.valueOf(that.master))
                && String.valueOf(this.model).equals(String.valueOf(that.model))
                && String.valueOf(this.client).equals(String.valueOf(that.client))
                && String.valueOf(this.shadowAccountPrefix).equals(String.valueOf(that.shadowAccountPrefix))
                && String.valueOf(this.shadowAccountSuffix).equals(String.valueOf(that.shadowAccountSuffix))
                ;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private boolean stringIsEmpty(String string) {
        return null == string
            || string.isEmpty();
    }
}
