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
package com.pamirs.attach.plugin.dynamic.template;


/**
 * @Auther: vernon
 * @Date: 2021/8/20 00:41
 * @Description:hbase模版
 */
public class HbaseTemplate extends AbstractTemplate {
    @Info(describe = "zookeeper ip", modifier = Info.ModifierType.UNMODIFIABLE)
    private String zookeeper_quorum;
    @Info(describe = "zookeeper port", modifier = Info.ModifierType.UNMODIFIABLE)
    private String zookeeper_client_port;
    @Info(describe = "/node", modifier = Info.ModifierType.UNMODIFIABLE)
    private String zookeeper_znode_parent;

    public String getZookeeper_quorum() {
        return zookeeper_quorum;
    }

    public HbaseTemplate setZookeeper_quorum(String zookeeper_quorum) {
        this.zookeeper_quorum = zookeeper_quorum;
        return this;
    }

    public String getZookeeper_client_port() {
        return zookeeper_client_port;
    }

    public HbaseTemplate setZookeeper_client_port(String zookeeper_client_port) {
        this.zookeeper_client_port = zookeeper_client_port;
        return this;
    }

    public String getZookeeper_znode_parent() {
        return zookeeper_znode_parent;
    }

    public HbaseTemplate setZookeeper_znode_parent(String zookeeper_znode_parent) {
        this.zookeeper_znode_parent = zookeeper_znode_parent;
        return this;
    }

    @Override
    public String getName() {
        return "hbase";
    }
}
