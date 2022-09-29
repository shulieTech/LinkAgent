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
 * @Date: 2021/8/19 18:46
 * @Description:
 */
public interface RedisTemplate extends Template {

    @Info(describe = "模式")
    enum MODEL {
        @Info(describe = "单机模式")
        single,
        @Info(describe = "集群模式")
        cluster,
        @Info(describe = "哨兵模式")
        sentinel,
        @Info(describe = "主从模式")
        masterSlave,
     /*   @Info(describe = "云托管模式")
        replicated;*/
    }


    @Info(describe = "redis客户端")
    enum Client {
        redisson,
        jedis,
        lettuce,
        ;
    }

    class JedisMasterSlaveTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        Client client = Client.jedis;
        MODEL model = MODEL.masterSlave;
        String master;
        String nodes;
        String password;
        Integer database;

        public Client getClient() {
            return client;
        }

        public JedisMasterSlaveTemplate setClient(Client client) {
            this.client = client;
            return this;
        }

        public MODEL getModel() {
            return model;
        }

        public JedisMasterSlaveTemplate setModel(MODEL model) {
            this.model = model;
            return this;
        }

        public String getMaster() {
            return master;
        }

        public JedisMasterSlaveTemplate setMaster(String master) {
            this.master = master;
            return this;
        }

        public String getNodes() {
            return nodes;
        }

        public JedisMasterSlaveTemplate setNodes(String nodes) {
            this.nodes = nodes;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public JedisMasterSlaveTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        public JedisMasterSlaveTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }
    }

    class JedisSingleTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        Client client = Client.jedis;
        MODEL model = MODEL.single;
        String master;
        String nodes;
        String password;
        Integer database;

        public Client getClient() {
            return client;
        }

        public JedisSingleTemplate setClient(Client client) {
            this.client = client;
            return this;
        }

        public MODEL getModel() {
            return model;
        }

        public JedisSingleTemplate setModel(MODEL model) {
            this.model = model;
            return this;
        }

        public String getMaster() {
            return master;
        }

        public JedisSingleTemplate setMaster(String master) {
            this.master = master;
            return this;
        }

        public String getNodes() {
            return nodes;
        }

        public JedisSingleTemplate setNodes(String nodes) {
            this.nodes = nodes;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public JedisSingleTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        public JedisSingleTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }
    }

    class JedisClusterTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        public MODEL getModel() {
            return model;
        }


        public Client getClient() {
            return client;
        }


        public String getNodes() {
            return nodes;
        }

        public JedisClusterTemplate setNodes(String nodes) {
            this.nodes = nodes;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        public JedisClusterTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public JedisClusterTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        public JedisClusterTemplate setMaster(String master) {
            this.master = master;
            return this;
        }

        public String getMaster() {
            return master;
        }

        private MODEL model = MODEL.cluster;

        private Client client = Client.jedis;
        /**
         * 节点数组
         * "127.0.0.1:5001,127.0.0.1:5002"
         */
        private String master;
        private String nodes;
        private Integer database;
        private String password;

    }


    class JedisSentinelTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        public MODEL getModel() {
            return model;
        }


        public Client getClient() {
            return client;
        }


        public String getMaster() {
            return master;
        }

        public JedisSentinelTemplate setMaster(String master) {
            this.master = master;
            return this;
        }

        public String getNodes() {
            return nodes;
        }

        public JedisSentinelTemplate setNodes(String nodes) {
            this.nodes = nodes;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        public JedisSentinelTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public JedisSentinelTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        private MODEL model = MODEL.sentinel;

        private Client client = Client.jedis;
        /**
         * 集群名字
         */
        private String master;


        /**
         * 节点数组
         * "127.0.0.1:5001,127.0.0.1:5002"
         */

        private String nodes;
        private Integer database;
        private String password;

    }

    class RedissonReplicatedTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        public MODEL getModel() {
            return model;
        }


        public Client getClient() {
            return client;
        }

        public String getNodes() {
            return nodes;
        }

        public RedissonReplicatedTemplate setNodes(String nodes) {
            this.nodes = nodes;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        public RedissonReplicatedTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public RedissonReplicatedTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        public RedissonReplicatedTemplate setMaster(String master) {
            this.master = master;
            return this;
        }

        public String getMaster() {
            return master;
        }

        private MODEL model = MODEL.sentinel;

        private Client client = Client.jedis;
        private String nodes;
        private Integer database;
        private String password;
        private String master;
    }

    class RedissionClusterTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        public Client getClient() {
            return client;
        }

        public RedissionClusterTemplate setClient(Client client) {
            this.client = client;
            return this;
        }

        public MODEL getModel() {
            return model;
        }

        public RedissionClusterTemplate setModel(MODEL model) {
            this.model = model;
            return this;
        }

        public String getNodes() {
            return nodes;
        }

        public RedissionClusterTemplate setNodes(String nodes) {
            this.nodes = nodes;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public RedissionClusterTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        public RedissionClusterTemplate setMaster(String master) {
            this.master = master;
            return this;
        }

        public String getMaster() {
            return master;
        }

        public RedissionClusterTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        private Client client = Client.redisson;
        private MODEL model = MODEL.cluster;
        /**
         * Config config = new Config();
         * config.useClusterServers().addNodeAddress("redis://127.0.0.1:5001")
         * .addNodeAddress("redis://127.0.0.1:5002");
         * client = Redisson.create(config);
         */
        private String master;
        private String nodes;
        private Integer database;

        private String password;
    }

    class RedissionSentinelTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        public Client getClient() {
            return client;
        }

        public RedissionSentinelTemplate setClient(Client client) {
            this.client = client;
            return this;
        }

        public MODEL getModel() {
            return model;
        }

        public RedissionSentinelTemplate setModel(MODEL model) {
            this.model = model;
            return this;
        }

        public String getMaster() {
            return master;
        }

        public RedissionSentinelTemplate setMaster(String master) {
            this.master = master;
            return this;
        }

        public String getNodes() {
            return nodes;
        }

        public RedissionSentinelTemplate setNodes(String nodes) {
            this.nodes = nodes;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        public RedissionSentinelTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public RedissionSentinelTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        private Client client = Client.redisson;
        private MODEL model = MODEL.sentinel;

        private String master;

        private String nodes;

        private Integer database;

        private String password;
    }

    class RedissionSingleTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        public Client getClient() {
            return client;
        }

        public RedissionSingleTemplate setClient(Client client) {
            this.client = client;
            return this;
        }

        public MODEL getModel() {
            return model;
        }

        public RedissionSingleTemplate setModel(MODEL model) {
            this.model = model;
            return this;
        }

        public String getNodes() {
            return nodes;
        }

        public RedissionSingleTemplate setNodes(String nodes) {
            this.nodes = nodes;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        public RedissionSingleTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public RedissionSingleTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        public RedissionSingleTemplate setMaster(String master) {
            this.master = master;
            return this;
        }

        public String getMaster() {
            return master;
        }

        private Client client = Client.redisson;
        private MODEL model = MODEL.single;
        private String master;
        private String nodes;
        private Integer database;
        private String password;
    }

    class RedissonMasterSlaveTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        public Client getClient() {
            return client;
        }

        public RedissonMasterSlaveTemplate setClient(Client client) {
            this.client = client;
            return this;
        }

        public MODEL getModel() {
            return model;
        }

        public RedissonMasterSlaveTemplate setModel(MODEL model) {
            this.model = model;
            return this;
        }

        public String getMaster() {
            return master;
        }

        public RedissonMasterSlaveTemplate setMaster(String master) {
            this.master = master;
            return this;
        }

        public String getNodes() {
            return nodes;
        }

        public RedissonMasterSlaveTemplate setNodes(String slaveNodes) {
            this.nodes = slaveNodes;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        public RedissonMasterSlaveTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public RedissonMasterSlaveTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        private Client client = Client.redisson;
        private MODEL model = MODEL.masterSlave;
        private String master;
        private String nodes;
        private Integer database;
        private String password;
    }

    //lettuce
    class LettuceSingleTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        public String getNodes() {
            return nodes;
        }

        public LettuceSingleTemplate setNodes(String nodes) {
            this.nodes = nodes;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public LettuceSingleTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        public LettuceSingleTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }

        public Client getClient() {
            return client;
        }

        public LettuceSingleTemplate setClient(Client client) {
            this.client = client;
            return this;
        }

        public MODEL getModel() {
            return model;
        }

        public LettuceSingleTemplate setModel(MODEL model) {
            this.model = model;
            return this;
        }

        public LettuceSingleTemplate setMaster(String master) {
            this.master = master;
            return this;
        }

        public String getMaster() {
            return master;
        }

        private String master;
        private String nodes;
        private String password;
        private Integer database;
        private Client client = Client.lettuce;
        private MODEL model = MODEL.single;
    }

    class LettuceClusterTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        public Client getClient() {
            return client;
        }

        public LettuceClusterTemplate setClient(Client client) {
            this.client = client;
            return this;
        }

        public MODEL getModel() {
            return model;
        }

        public LettuceClusterTemplate setModel(MODEL model) {
            this.model = model;
            return this;
        }

        public String getNodes() {
            return nodes;
        }

        public LettuceClusterTemplate setNodes(String nodes) {
            this.nodes = nodes;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        public LettuceClusterTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public LettuceClusterTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        public LettuceClusterTemplate setMaster(String master) {
            this.master = master;
            return this;
        }

        public String getMaster() {
            return master;
        }

        private Client client = Client.lettuce;
        private MODEL model = MODEL.cluster;
        private String nodes;
        private Integer database;
        private String password;
        private String master;
    }

    class LettuceMasterSlaveTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        public Client getClient() {
            return client;
        }

        public LettuceMasterSlaveTemplate setClient(Client client) {
            this.client = client;
            return this;
        }

        public MODEL getModel() {
            return model;
        }

        public LettuceMasterSlaveTemplate setModel(MODEL model) {
            this.model = model;
            return this;
        }

        public String getMaster() {
            return master;
        }

        /**
         * no need for master
         *
         * @param master
         * @return
         */
        public LettuceMasterSlaveTemplate setMaster(String master) {
            this.master = master;
            return this;
        }

        public String getNodes() {
            return nodes;
        }

        public LettuceMasterSlaveTemplate setNodes(String slave) {
            this.nodes = slave;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public LettuceMasterSlaveTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        public LettuceMasterSlaveTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        private Client client = Client.lettuce;
        private MODEL model = MODEL.masterSlave;
        private String master;
        private String nodes;
        private String password;
        private Integer database;
    }

    class LettuceSentinelTemplate extends AbstractTemplate {
        @Override
        public String getName() {
            return client.name();
        }

        public Client getClient() {
            return client;
        }

        public LettuceSentinelTemplate setClient(Client client) {
            this.client = client;
            return this;
        }

        public MODEL getModel() {
            return model;
        }

        public LettuceSentinelTemplate setModel(MODEL model) {
            this.model = model;
            return this;
        }

        public String getNodes() {
            return nodes;
        }

        public LettuceSentinelTemplate setNodes(String nodes) {
            this.nodes = nodes;
            return this;
        }

        public Integer getDatabase() {
            return database;
        }

        public LettuceSentinelTemplate setDatabase(Integer database) {
            this.database = database;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public LettuceSentinelTemplate setPassword(String password) {
            this.password = password;
            return this;
        }

        public String getMaster() {
            return master;
        }

        public LettuceSentinelTemplate setMaster(String master) {
            this.master = master;
            return this;
        }


        private Client client = Client.lettuce;
        private MODEL model = MODEL.sentinel;
        private String nodes;
        private Integer database;
        private String password;
        private String master;

    }
}

