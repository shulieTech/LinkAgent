/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.jedis;

import com.pamirs.attach.plugin.jedis.interceptor.*;
import com.pamirs.attach.plugin.jedis.util.RedisUtils;
import com.pamirs.pradar.interceptor.Interceptors;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import com.shulie.instrument.simulator.api.scope.ExecutionPolicy;
import org.kohsuke.MetaInfServices;


/**
 * @author vincent
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "redis-jedis", version = "1.0.0", author = "xiaobin@shulie.io", description = "redis 的 jedis 客户端")
public class JedisPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() {

        EnhanceCallback jedisEnhanceCallback = new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethods(RedisUtils.get().keySet());
                method.addInterceptor(Listeners.dynamicScope(JedisInterceptor.class, ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                method.addInterceptor(Listeners.of(JedisSentinelShadowServerInterceptor.class, "JEDIS_SENTINEL", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                /**
                 * jedis单机模式客户端路由器
                 */
                method.addInterceptor(Listeners.of(JedisSingleClientCutOffInterceptor.class, "JEDIS_SINGLE", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                method.addInterceptor(Listeners.of(MJedisInterceptor.class));

            }
        };
        this.enhanceTemplate.enhance(this, "redis.clients.jedis.BinaryJedis", jedisEnhanceCallback);
        this.enhanceTemplate.enhance(this, "redis.clients.jedis.Jedis", jedisEnhanceCallback);

        EnhanceCallback pluginMaxRedisExpireTimeEnhanceCallback = new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                target.getDeclaredMethods("expire", "pexpire", "setex", "psetex")
                        .addInterceptor(Listeners.of(PluginMaxRedisExpireTimeInterceptor.class));
                target.getDeclaredMethod("set", "java.lang.String", "java.lang.String", "redis.clients.jedis.params.SetParams")
                        .addInterceptor(Listeners.of(PluginMaxRedisExpireTimeInterceptor.class));
                target.getDeclaredMethod("set", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "long")
                        .addInterceptor(Listeners.of(PluginMaxRedisExpireTimeInterceptor.class));
                target.getDeclaredMethod("set", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "int")
                        .addInterceptor(Listeners.of(PluginMaxRedisExpireTimeInterceptor.class));
            }
        };
        this.enhanceTemplate.enhance(this, "redis.clients.jedis.BinaryJedisCluster", pluginMaxRedisExpireTimeEnhanceCallback);
        this.enhanceTemplate.enhance(this, "redis.clients.jedis.JedisCluster", pluginMaxRedisExpireTimeEnhanceCallback);
        this.enhanceTemplate.enhance(this, "redis.clients.jedis.PipelineBase", pluginMaxRedisExpireTimeEnhanceCallback);
        this.enhanceTemplate.enhance(this, "redis.clients.jedis.Jedis", pluginMaxRedisExpireTimeEnhanceCallback);

        EnhanceCallback jedisClusterEnhanceCallback = new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethods(RedisUtils.get().keySet());
                method.addInterceptor(Listeners.of(MJedisInterceptor.class));
            }
        };
        this.enhanceTemplate.enhance(this, "redis.clients.jedis.BinaryJedisCluster", jedisClusterEnhanceCallback);
        this.enhanceTemplate.enhance(this, "redis.clients.jedis.JedisCluster", jedisClusterEnhanceCallback);

        //redis.clients.jedis.PipelineBase
        enhanceTemplate.enhanceWithSuperClass(this, "redis.clients.jedis.PipelineBase", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                String[] methodName = {"append", "brpop", "blpop", "decr", "decrBy", "del", "echo", "exists", "expire", "expireAt", "get", "getbit", "bitpos", "bitpos", "getrange", "getSet", "getrange", "hdel", "hexists", "hget", "hgetAll", "hincrBy", "hkeys", "hlen", "hmget", "hmset", "hset", "hsetnx", "hvals", "incr", "incrBy", "lindex", "linsert", "llen", "lpop", "lpush", "lpushx", "lrange", "lrem", "lset", "ltrim", "move", "persist", "rpop", "rpush", "rpushx", "sadd", "scard", "set", "setbit", "setex", "setnx", "setrange", "sismember", "smembers", "sort", "sort", "spop", "spop", "srandmember", "srandmember", "srem", "strlen", "substr", "ttl", "type", "zadd", "zadd", "zcard", "zcount", "zincrby", "zrange", "zrangeByScore", "zrangeByScoreWithScores", "zrevrangeByScore", "zrevrangeByScoreWithScores", "zrangeWithScores", "zrank", "zrem", "zremrangeByRank", "zremrangeByScore", "zrevrange", "zrevrangeWithScores", "zrevrank", "zscore", "zlexcount", "zrangeByLex", "zrevrangeByLex", "bitcount", "dump", "migrate", "objectRefcount", "objctRefcount", "objectEncoding", "objectIdletime", "pexpire", "pexpireAt", "pttl", "restore", "incrByFloat", "psetex", "set", "hincrByFloat", "eval", "evalsha", "pfadd", "pfcount", "geoadd", "geodist", "geohash", "geopos", "georadius", "georadiusByMember", "bitfield", "xread", "pttl"};
                InstrumentMethod declaredMethod = target.getDeclaredMethods(methodName);
                declaredMethod.addInterceptor(Listeners.of(JedisPipelineBaseInterceptor.class));
            }
        });

        //兜底
        enhanceTemplate.enhance(this, "redis.clients.jedis.Client", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethods(RedisUtils.get().keySet());
                method.addInterceptor(Listeners.of(RedisDataCheckInterceptor.class));
            }
        });


        enhanceTemplate.enhance(this, "redis.clients.jedis.util.Pool", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod closeMethod = target.getDeclaredMethod("close");
                closeMethod.addInterceptor(Listeners.of(PoolCloseInterceptor.class));
            }
        });


        //redis.clients.jedis.JedisSlotBasedConnectionHandler.getConnectionFromSlot
        //影子cluster Server
        enhanceTemplate.enhance(this, "redis.clients.jedis.JedisSlotBasedConnectionHandler", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method1 = target.getDeclaredMethod("getConnectionFromSlot", "int");
                method1.addInterceptor(Listeners.of(JedisClusterConnectionInterceptor.class, "Jedis_Get_Connection_Scope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                InstrumentMethod method2 = target.getDeclaredMethod("getConnection");
                method2.addInterceptor(Listeners.of(JedisClusterConnectionInterceptor.class, "Jedis_Get_Connection_Scope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

            }
        });


        //redis.clients.jedis.JedisClusterConnectionHandler
        //影子cluster Server
        enhanceTemplate.enhance(this, "redis.clients.jedis.JedisClusterConnectionHandler", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                // jedis 3.1.0
                //redis.clients.jedis.JedisClusterConnectionHandler#JedisClusterConnectionHandler(
                //java.util.Set<redis.clients.jedis.HostAndPort>,
                //org.apache.commons.pool2.impl.GenericObjectPoolConfig, int, int,
                //java.lang.String, java.lang.String, boolean, javax.net.ssl.SSLSocketFactory,
                //javax.net.ssl.SSLParameters, javax.net.ssl.HostnameVerifier,
                //redis.clients.jedis.JedisClusterHostAndPortMap)
                InstrumentMethod closeMethod = target.getDeclaredMethod("close");
                closeMethod.addInterceptor(Listeners.of(PoolCloseInterceptor.class));
            }
        });

        // 目前使用这种方式不支持影子库
        enhanceTemplate.enhance(this, "redis.clients.jedis.Connection", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod sendCommand = target.getDeclaredMethods("sendCommand");
                sendCommand.addInterceptor(Listeners.of(JedisConnectionSendCommandTraceInterceptor.class));
                sendCommand.addInterceptor(Listeners.of(JedisConnectionSendCommandInterceptor.class));
                sendCommand.addInterceptor(Listeners.of(JedisSingleClientCutOffInterceptor.class));
            }
        });


        return true;
    }
}
