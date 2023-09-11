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
package com.pamirs.attach.plugin.lettuce.interceptor.spring;

import com.pamirs.attach.plugin.dynamic.Attachment;
import com.pamirs.attach.plugin.dynamic.ResourceManager;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.dynamic.template.RedisTemplate;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.interceptor.ResultInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import io.lettuce.core.AbstractRedisClient;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * @Auther: vernon
 * @Date: 2021/9/10 13:17
 * @Description:
 */
public class SpringRedisClientInfoCollector extends ResultInterceptorAdaptor {
    Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 业务流量的信息采集
     *
     * @param advice
     */
    protected void attachment(Advice advice) {

        try {
            if (Pradar.isClusterTest()) {
                return;
            }
            LettuceConnectionFactory biz = (LettuceConnectionFactory) advice.getTarget();

            Object standaloneConfiguration = biz.getStandaloneConfiguration();
            Object clusterConfiguration = biz.getClusterConfiguration();
            Object sentinelConfiguration = biz.getSentinelConfiguration();

            if (clusterConfiguration != null) {
                Set<RedisNode> nodeSet = ((RedisClusterConfiguration) clusterConfiguration).getClusterNodes();
                StringBuilder builder = new StringBuilder();
                for (RedisNode node : nodeSet) {
                    String host = node.getHost();
                    String port = String.valueOf(node.getPort());
                    builder.append(host.concat(":").concat(port))
                            .append(",");
                }
                String nodes = builder.deleteCharAt(builder.length() - 1).toString();

                ResourceManager.set(new Attachment(Arrays.asList(nodes.split(","))
                        , "redis-lettuce"
                        , new String[]{"redis"},
                        new RedisTemplate.LettuceClusterTemplate()
                                .setNodes(nodes)
                ));

            } else if (sentinelConfiguration != null) {
                RedisSentinelConfiguration redisSentinelConfiguration = (RedisSentinelConfiguration) sentinelConfiguration;
                String masterName = redisSentinelConfiguration.getMaster().getName();
                Integer database = Integer.parseInt(String.valueOf(redisSentinelConfiguration.getDatabase()));
                Set<RedisNode> nodeSet = redisSentinelConfiguration.getSentinels();
                StringBuilder builder = new StringBuilder();
                for (RedisNode node : nodeSet) {
                    String host = node.getHost();
                    String port = String.valueOf(node.getPort());
                    builder.append(host.concat(":").concat(port))
                            .append(",");
                }
                String nodes = builder.deleteCharAt(builder.length() - 1).toString();
                List<String> sentinelRemoteAddress = new ArrayList<String>();
                try {
                    AbstractRedisClient client = ReflectionUtils.get(biz, "client");
                    ChannelGroup channelGroup = ReflectionUtils.get(client, "channels");
                    Iterator iterator = channelGroup.iterator();
                    while (iterator.hasNext()) {
                        Object o = iterator.next();
                        if (o instanceof NioSocketChannel) {
                            NioSocketChannel channel = (NioSocketChannel) o;
                            InetSocketAddress inetSocketAddress = channel.remoteAddress();
                            String host = inetSocketAddress.getAddress().getHostName();
                            String port = String.valueOf(inetSocketAddress.getPort());
                            sentinelRemoteAddress.add(host.concat(":").concat(port));
                        }
                    }

                } catch (Throwable t) {
                    logger.error(Throwables.getStackTraceAsString(t));
                }
                sentinelRemoteAddress.addAll(Arrays.asList(nodes.split(",")));
                ResourceManager.set(new Attachment(sentinelRemoteAddress
                        , "redis-lettuce"
                        , new String[]{"redis"},
                        new RedisTemplate.LettuceSentinelTemplate()
                                .setNodes(nodes)
                                .setMaster(masterName)
                                .setDatabase(database)
                ));


            }
            /**
             *standalone肯定不为空 放后面 因为可能为localhost
             */
            else if (standaloneConfiguration != null &&
                    !("localhost".equals(((RedisStandaloneConfiguration) standaloneConfiguration).getHostName()))) {
                RedisStandaloneConfiguration configuration = (RedisStandaloneConfiguration) standaloneConfiguration;

                String host = configuration.getHostName();
                Integer port = configuration.getPort();
                Integer db = configuration.getDatabase();
                String node = host.concat(":").concat(String.valueOf(port));

                ResourceManager.set(new Attachment(Arrays.asList(node)
                        , "redis-lettuce"
                        , new String[]{"redis"},
                        new RedisTemplate.LettuceSingleTemplate()
                                .setNodes(node)
                                .setDatabase(db)

                ));


            }
        } catch (Throwable t) {
            logger.error("[redis-lettuce] collector spring biz info error , {}", Throwables.getStackTraceAsString(t));
        }
    }

}
