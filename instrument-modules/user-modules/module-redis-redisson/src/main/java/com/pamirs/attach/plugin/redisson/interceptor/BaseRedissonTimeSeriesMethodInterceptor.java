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
package com.pamirs.attach.plugin.redisson.interceptor;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import com.pamirs.attach.plugin.dynamic.Attachment;
import com.pamirs.attach.plugin.dynamic.template.AbstractTemplate;
import com.pamirs.attach.plugin.dynamic.template.RedisTemplate;
import com.pamirs.attach.plugin.dynamic.template.RedisTemplate.RedissionSentinelTemplate;
import com.pamirs.attach.plugin.dynamic.template.RedisTemplate.RedissionSingleTemplate;
import com.pamirs.attach.plugin.dynamic.template.RedisTemplate.RedissonMasterSlaveTemplate;
import com.pamirs.attach.plugin.dynamic.template.RedisTemplate.RedissonReplicatedTemplate;
import com.pamirs.attach.plugin.redisson.RedissonConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.apache.commons.lang.StringUtils;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.MasterSlaveServersConfig;
import org.redisson.config.ReplicatedServersConfig;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/9/8 2:07 下午
 */
public abstract class BaseRedissonTimeSeriesMethodInterceptor extends TraceInterceptorAdaptor {
    private final static Logger LOGGER = LoggerFactory.getLogger(
            BaseRedissonTimeSeriesMethodInterceptor.class.getName());
    private Method singleServerConfigGetAddressMethod;
    private Method masterSlaveServersConfigGetAddressMethod;

    @Resource
    protected DynamicFieldManager manager;

    public BaseRedissonTimeSeriesMethodInterceptor() {
        try {
            singleServerConfigGetAddressMethod = SingleServerConfig.class.getDeclaredMethod("getAddress");
            singleServerConfigGetAddressMethod.setAccessible(true);
        } catch (Throwable e) {
        }
        try {
            masterSlaveServersConfigGetAddressMethod = MasterSlaveServersConfig.class.getDeclaredMethod(
                    "getMasterAddress");
            masterSlaveServersConfigGetAddressMethod.setAccessible(true);
        } catch (Throwable e) {
        }
    }

    private String getAddress(SingleServerConfig config) {
        if (singleServerConfigGetAddressMethod != null) {
            try {
                Object result = singleServerConfigGetAddressMethod.invoke(config);
                if (result != null) {
                    return result.toString();
                }
                return null;
            } catch (Throwable e) {
                try {
                    Object result = Reflect.on(config).call("getAddress").get();
                    if (result != null) {
                        return result.toString();
                    }
                    return null;
                } catch (Throwable ex) {
                    LOGGER.warn("SIMULATOR: can't found getAddress method from {}, check {} compatibility.",
                            SingleServerConfig.class.getName(), getClass().getName());
                }
            }
        } else {
            try {
                Object result = Reflect.on(config).call("getAddress").get();
                if (result != null) {
                    return result.toString();
                }
                return null;
            } catch (Throwable ex) {
                LOGGER.warn("SIMULATOR: can't found getAddress method from {}, check {} compatibility.",
                        SingleServerConfig.class.getName(), getClass().getName());
            }
        }
        return null;
    }

    private String getMasterAddress(MasterSlaveServersConfig config) {
        if (masterSlaveServersConfigGetAddressMethod != null) {
            try {
                Object result = masterSlaveServersConfigGetAddressMethod.invoke(config);
                if (result != null) {
                    return result.toString();
                }
                return null;
            } catch (Throwable e) {
                try {
                    Object result = Reflect.on(config).call("getMasterAddress").get();
                    if (result != null) {
                        return result.toString();
                    }
                    return null;
                } catch (Throwable ex) {
                    LOGGER.warn("SIMULATOR: can't found getAddress method from {}, check {} compatibility.",
                            MasterSlaveServersConfig.class.getName(), getClass().getName());
                }
            }
        } else {
            try {
                Object result = Reflect.on(config).call("getMasterAddress").get();
                if (result != null) {
                    return result.toString();
                }
                return null;
            } catch (Throwable ex) {
                LOGGER.warn("SIMULATOR: can't found getAddress method from {}, check {} compatibility.",
                        SingleServerConfig.class.getName(), getClass().getName());
            }
        }
        return null;
    }

    protected void attachment(Object target, String methodName, Object[] args) {
        Config config = null;
        config = manager.getDynamicField(target, RedissonConstants.DYNAMIC_FIELD_CONFIG);
        if (config == null) {
            return;
        }

        try {
            Object sentinelServersConfig = Reflect.on(config)
                    .get(RedissonConstants.DYNAMIC_FIELD_SENTINEL_SERVERS_CONFIG);
            attachment(sentinelServersConfig);

        } catch (Throwable t) {

        }

        try {
            Object masterSlaveServersConfig = Reflect.on(config).get(
                    RedissonConstants.DYNAMIC_FIELD_MASTER_SLAVE_SERVERS_CONFIG);
            attachment(masterSlaveServersConfig);

        } catch (Throwable t) {

        }


        try {
            Object singleServerConfig = Reflect.on(config).get(
                    RedissonConstants.DYNAMIC_FIELD_SINGLE_SERVER_CONFIG);
            attachment(singleServerConfig);
        } catch (Throwable t) {
        }
        try {
            Object clusterServersConfig = Reflect.on(config).get
                    (RedissonConstants.DYNAMIC_FIELD_CLUSTER_SERVERS_CONFIG);
            attachment(clusterServersConfig);

        } catch (Throwable t) {

        }
        try {
            Object replicatedServersConfig = Reflect.on(config).get(
                    RedissonConstants.DYNAMIC_FIELD_REPLICATED_SERVERS_CONFIG);
            attachment(replicatedServersConfig);

        } catch (Throwable t) {

        }
    }

    @Override
    public String getPluginName() {
        return RedissonConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return RedissonConstants.PLUGIN_TYPE;
    }

    public int getDatabase(Object target, String methodName, Object[] args) {
        Config config = null;
        try {
            config = manager.getDynamicField(target, RedissonConstants.DYNAMIC_FIELD_CONFIG);
        } catch (Throwable e) {
            return 0;
        }
        if (config == null) {
            return 0;
        }
        SentinelServersConfig sentinelServersConfig = getSentinelServersConfig(config);
        if (sentinelServersConfig != null) {
            int database = sentinelServersConfig.getDatabase();
            return database;
        }

        MasterSlaveServersConfig masterSlaveServersConfig = getMasterSlaveServersConfig(config);
        if (masterSlaveServersConfig != null) {
            return masterSlaveServersConfig.getDatabase();
        }

        SingleServerConfig singleServerConfig = getSingleServerConfig(config);
        if (singleServerConfig != null) {
            return singleServerConfig.getDatabase();
        }

        ClusterServersConfig clusterServersConfig = getClusterServersConfig(config);
        if (clusterServersConfig != null) {
            return 0;
        }

        ReplicatedServersConfig replicatedServersConfig = getReplicatedServersConfig(config);
        if (replicatedServersConfig != null) {
            return replicatedServersConfig.getDatabase();
        }

        return 0;
    }


    /**
     * 入参是org.redisson.config.BaseConfig
     * 但是有的版本是私有类
     *
     * @param config
     */
    private void attachment(Object config) {
        try {
            if (Pradar.isClusterTest() || config == null) {
                return;
            }
            ClusterServersConfig config1 = null;
            Reflect reflect = Reflect.on(config);

            Integer database = null;
            try {
                database = Integer.parseInt(String.valueOf(reflect.get("database")));
            } catch (Throwable t) {

            }
            AbstractTemplate template;
            if (config instanceof ClusterServersConfig) {
                List nodeAddresses = reflect.get("nodeAddresses");
                StringBuilder nodeBuilder = new StringBuilder();
                for (Object object : nodeAddresses) {
                    if (URI.class.isAssignableFrom(object.getClass())) {
                        URI uri = (URI) object;
                        nodeBuilder
                                .append(uri.getHost().concat(":").concat(String.valueOf(uri.getPort())))
                                .append(",");
                    } else if (String.class.isAssignableFrom(object.getClass())) {
                        nodeBuilder
                                .append(removePre(object.toString()))
                                .append(",");
                    }

                }
                String nodes = nodeBuilder.deleteCharAt(nodeBuilder.length() - 1).toString();
                template = new RedisTemplate.RedissionClusterTemplate().setNodes(nodes);
            } else if (config instanceof MasterSlaveServersConfig) {
                Set nodeAddresses = reflect.get("slaveAddresses");
                StringBuilder nodeBuilder = new StringBuilder();

                for (Object object : nodeAddresses) {
                    if (URI.class.isAssignableFrom(object.getClass())) {
                        URI uri = (URI) object;
                        nodeBuilder
                                .append(uri.getHost().concat(":").concat(String.valueOf(uri.getPort())))
                                .append(",");
                    } else if (String.class.isAssignableFrom(object.getClass())) {
                        nodeBuilder
                                .append(removePre(object.toString()))
                                .append(",");
                    }

                }
                String nodes = nodeBuilder.deleteCharAt(nodeBuilder.length() - 1).toString();

                Object masterNodeObj = reflect.get("masterAddress");
                String masterAddr = "";
                if (masterNodeObj instanceof URI) {
                    masterAddr = ((URI) masterNodeObj).getHost()
                            .concat(":")
                            .concat(String.valueOf(((URI) masterNodeObj).getPort()));
                    ;
                } else {
                    masterAddr = removePre(String.valueOf(masterNodeObj));
                }
                template = new RedissonMasterSlaveTemplate().setMaster(
                        masterAddr).setNodes(nodes)
                        .setDatabase(database);
            } else if (config instanceof SingleServerConfig) {

                String nodes = "";
                Object address = Reflect.on(config).get("address");
                if (String.class.isAssignableFrom(address.getClass())) {
                    nodes = removePre((String) address);
                } else if (URI.class.isAssignableFrom(address.getClass())) {
                    URI uri = (URI) address;
                    nodes = uri.getHost().concat(":").concat(String.valueOf(uri.getPort()));

                }
                template = new RedissionSingleTemplate().setNodes(nodes)
                        .setDatabase(database);
            } else if (config instanceof SentinelServersConfig) {


                List sentinelAddresses = reflect.get("sentinelAddresses");
                StringBuilder nodeBuilder = new StringBuilder();
                for (Object object : sentinelAddresses) {
                    if (URI.class.isAssignableFrom(object.getClass())) {
                        URI uri = (URI) object;
                        nodeBuilder
                                .append(uri.getHost().concat(":").concat(String.valueOf(uri.getPort())))
                                .append(",");
                    } else if (String.class.isAssignableFrom(object.getClass())) {
                        nodeBuilder
                                .append(removePre(object.toString()))
                                .append(",");
                    }

                }
                String nodes = nodeBuilder.deleteCharAt(nodeBuilder.length() - 1).toString();

                template = new RedissionSentinelTemplate().setNodes(nodes)
                        .setMaster(removePre(((SentinelServersConfig) config).getMasterName())).setNodes(nodes)
                        .setDatabase((database));
            } else if (config instanceof ReplicatedServersConfig) {

                List nodeAddresses = reflect.get("nodeAddresses");
                StringBuilder nodeBuilder = new StringBuilder();
                for (Object object : nodeAddresses) {
                    if (URI.class.isAssignableFrom(object.getClass())) {
                        URI uri = (URI) object;
                        nodeBuilder
                                .append(uri.getHost().concat(":").concat(String.valueOf(uri.getPort())))
                                .append(",");
                    } else if (String.class.isAssignableFrom(object.getClass())) {
                        nodeBuilder
                                .append(removePre(object.toString()))
                                .append(",");
                    }

                }
                String nodes = nodeBuilder.deleteCharAt(nodeBuilder.length() - 1).toString();


                template = new RedissonReplicatedTemplate().setDatabase(database)
                        .setNodes(nodes);
            } else {
                LOGGER.error("Redisson not instanceof any know config:{}", config);
                return;
            }
            final Attachment attachment = new Attachment(null, RedissonConstants.PLUGIN_NAME,
                    new String[]{RedissonConstants.MIDDLEWARE_NAME}
                    , template
            );
            // ResourceManager.set(attachment);
            Pradar.getInvokeContext().setExt(attachment);
        } catch (Throwable t) {
            LOGGER.error("Redisson attachment error", t);
        }
    }

    public static String removePre(String obj) {
        if (obj == null) {
            return null;
        }
        if (obj.startsWith("redis://")) {
            return obj.replaceAll("redis://", "");
        }
        return obj;

    }

    private SentinelServersConfig getSentinelServersConfig(Config config) {
        SentinelServersConfig sentinelServersConfig = null;
        try {
            sentinelServersConfig = Reflect.on(config).get(RedissonConstants.DYNAMIC_FIELD_SENTINEL_SERVERS_CONFIG);
        } catch (ReflectException e) {
        }
        return sentinelServersConfig;
    }

    private MasterSlaveServersConfig getMasterSlaveServersConfig(Config config) {
        MasterSlaveServersConfig masterSlaveServersConfig = null;
        try {
            masterSlaveServersConfig = Reflect.on(config).get(
                    RedissonConstants.DYNAMIC_FIELD_MASTER_SLAVE_SERVERS_CONFIG);
        } catch (ReflectException e) {
        }
        return masterSlaveServersConfig;
    }


    private SingleServerConfig getSingleServerConfig(Config config) {
        SingleServerConfig singleServerConfig = null;
        try {
            singleServerConfig = Reflect.on(config).get(RedissonConstants.DYNAMIC_FIELD_SINGLE_SERVER_CONFIG);
        } catch (ReflectException e) {
        }
        return singleServerConfig;
    }

    private ClusterServersConfig getClusterServersConfig(Config config) {
        ClusterServersConfig clusterServersConfig = null;
        try {
            clusterServersConfig = Reflect.on(config).get(RedissonConstants.DYNAMIC_FIELD_CLUSTER_SERVERS_CONFIG);
        } catch (ReflectException e) {
        }
        return clusterServersConfig;
    }

    private ReplicatedServersConfig getReplicatedServersConfig(Config config) {
        ReplicatedServersConfig replicatedServersConfig = null;
        try {
            replicatedServersConfig = Reflect.on(config).get(RedissonConstants.DYNAMIC_FIELD_REPLICATED_SERVERS_CONFIG);
        } catch (ReflectException e) {
        }
        return replicatedServersConfig;
    }

    public String getHost(Object target, String methodName, Object[] args) {
        Config config = manager.getDynamicField(target, RedissonConstants.DYNAMIC_FIELD_CONFIG);
        if (config == null) {
            return null;
        }

        SentinelServersConfig sentinelServersConfig = getSentinelServersConfig(config);
        if (sentinelServersConfig != null) {
            List list = sentinelServersConfig.getSentinelAddresses();
            if (list != null && !list.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (Object obj : list) {
                    if (obj instanceof String) {
                        String str = (String) obj;
                        if (StringUtils.isBlank(str)) {
                            continue;
                        }
                        if (StringUtils.indexOf(str, ":") != -1) {
                            String[] arr = StringUtils.split(str, ':');
                            builder.append(arr[1]).append(',');
                        }
                    } else if (obj instanceof URI) {
                        builder.append(((URI) obj).getHost()).append(",");
                    }

                }
                if (builder.length() > 0) {
                    builder.deleteCharAt(builder.length() - 1);
                }
                if (builder.length() > 0) {
                    return builder.toString();
                } else {
                    return "6379";
                }
            }
        }

        MasterSlaveServersConfig masterSlaveServersConfig = getMasterSlaveServersConfig(config);
        if (masterSlaveServersConfig != null) {
            Object masterAddress = masterSlaveServersConfig.getMasterAddress();

            StringBuilder builder = new StringBuilder();
            if (String.class.isAssignableFrom(masterAddress.getClass())) {
                if (StringUtils.isNotBlank((String) masterAddress)) {
                    if (StringUtils.indexOf((String) masterAddress, ":") != -1) {
                        String[] arr = StringUtils.split((String) masterAddress, ':');
                        builder.append(arr[0]).append(',');
                    }
                }
            } else if (URI.class.isAssignableFrom(masterAddress.getClass())) {
                builder.append(((URI) masterAddress).getHost()).append(',');
            }


            Set set = masterSlaveServersConfig.getSlaveAddresses();
            if (set != null && !set.isEmpty()) {
                for (Object obj : set) {
                    if (obj instanceof String) {
                        String str = (String) obj;
                        if (StringUtils.isBlank(str)) {
                            continue;
                        }
                        if (StringUtils.indexOf(str, ":") != -1) {
                            String[] arr = StringUtils.split(str, ':');
                            builder.append(arr[0]).append(',');
                        }
                    } else if (URI.class.isAssignableFrom(obj.getClass())) {
                        builder.append(((URI) obj).getHost()).append(",");

                    }

                }
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            if (builder.length() > 0) {
                return builder.toString();
            } else {
                return "6379";
            }
        }

        SingleServerConfig singleServerConfig = getSingleServerConfig(config);
        if (singleServerConfig != null) {
            String address = getAddress(singleServerConfig);
            if (StringUtils.isNotBlank(address)) {
                if (StringUtils.indexOf(address, ":") == -1) {
                    return "6379";
                } else {
                    String[] arr = StringUtils.split(address, ':');
                    return StringUtils.trim(arr[1]);
                }
            }
        }

        ClusterServersConfig clusterServersConfig = getClusterServersConfig(config);
        if (clusterServersConfig != null) {
            List list = clusterServersConfig.getNodeAddresses();
            if (list != null && !list.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (Object obj : list) {
                    String str = obj == null ? null : obj.toString();
                    if (StringUtils.isBlank(str)) {
                        continue;
                    }
                    if (StringUtils.indexOf(str, ":") != -1) {
                        String[] arr = StringUtils.split(str, ':');
                        builder.append(arr[0]).append(',');
                    }
                }
                if (builder.length() > 0) {
                    builder.deleteCharAt(builder.length() - 1);
                }
                if (builder.length() > 0) {
                    return builder.toString();
                } else {
                    return "6379";
                }
            }
        }

        ReplicatedServersConfig replicatedServersConfig = getReplicatedServersConfig(config);
        if (replicatedServersConfig != null) {
            List list = replicatedServersConfig.getNodeAddresses();
            StringBuilder builder = new StringBuilder();
            for (Object obj : list) {
                String str = obj == null ? null : obj.toString();
                if (StringUtils.isBlank(str)) {
                    continue;
                }
                if (StringUtils.indexOf(str, ":") != -1) {
                    String[] arr = StringUtils.split(str, ':');
                    builder.append(arr[0]).append(',');
                }
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            if (builder.length() > 0) {
                return builder.toString();
            } else {
                return "6379";
            }
        }

        return null;
    }

    public String getPort(Object target, String methodName, Object[] args) {
        Config config = manager.getDynamicField(target, RedissonConstants.DYNAMIC_FIELD_CONFIG);
        if (config == null) {
            return null;
        }
        SentinelServersConfig sentinelServersConfig = getSentinelServersConfig(config);
        if (sentinelServersConfig != null) {
            List list = sentinelServersConfig.getSentinelAddresses();
            if (list != null && !list.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (Object obj : list) {
                    String str = obj == null ? null : obj.toString();
                    if (StringUtils.isBlank(str)) {
                        continue;
                    }
                    if (StringUtils.indexOf(str, ":") == -1) {
                        builder.append(str).append(',');
                    } else {
                        String[] arr = StringUtils.split(str, ':');
                        builder.append(arr[0]).append(',');
                    }
                }
                if (builder.length() > 0) {
                    builder.deleteCharAt(builder.length() - 1);
                }
                if (builder.length() > 0) {
                    return builder.toString();
                }
            }
        }

        MasterSlaveServersConfig masterSlaveServersConfig = getMasterSlaveServersConfig(config);
        if (masterSlaveServersConfig != null) {
            String masterAddress = getMasterAddress(masterSlaveServersConfig);
            StringBuilder builder = new StringBuilder();
            if (StringUtils.isNotBlank(masterAddress)) {
                if (StringUtils.indexOf(masterAddress, ":") == -1) {
                    builder.append(masterAddress).append(',');
                } else {
                    String[] arr = StringUtils.split(masterAddress, ':');
                    builder.append(arr[0]).append(',');
                }
            }

            Set set = masterSlaveServersConfig.getSlaveAddresses();
            if (set != null && !set.isEmpty()) {
                for (Object obj : set) {
                    String str = obj == null ? null : obj.toString();
                    if (StringUtils.isBlank(str)) {
                        continue;
                    }
                    if (StringUtils.indexOf(str, ":") == -1) {
                        builder.append(str).append(',');
                    } else {
                        String[] arr = StringUtils.split(str, ':');
                        builder.append(arr[0]).append(',');
                    }
                }
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            if (builder.length() > 0) {
                return builder.toString();
            }
        }

        SingleServerConfig singleServerConfig = getSingleServerConfig(config);
        if (singleServerConfig != null) {
            String address = getAddress(singleServerConfig);
            if (StringUtils.isNotBlank(address)) {
                if (StringUtils.indexOf(address, ":") == -1) {
                    return address;
                } else {
                    String[] arr = StringUtils.split(address, ':');
                    return StringUtils.trim(arr[0]);
                }
            }
        }

        ClusterServersConfig clusterServersConfig = getClusterServersConfig(config);
        if (clusterServersConfig != null) {
            List list = clusterServersConfig.getNodeAddresses();
            if (list != null && !list.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (Object obj : list) {
                    String str = obj == null ? null : obj.toString();
                    if (StringUtils.isBlank(str)) {
                        continue;
                    }
                    if (StringUtils.indexOf(str, ":") == -1) {
                        builder.append(str).append(',');
                    } else {
                        String[] arr = StringUtils.split(str, ':');
                        builder.append(arr[0]).append(',');
                    }
                }
                if (builder.length() > 0) {
                    builder.deleteCharAt(builder.length() - 1);
                }
                if (builder.length() > 0) {
                    return builder.toString();
                }
            }
        }

        ReplicatedServersConfig replicatedServersConfig = getReplicatedServersConfig(config);
        if (replicatedServersConfig != null) {
            List list = replicatedServersConfig.getNodeAddresses();
            StringBuilder builder = new StringBuilder();
            for (Object obj : list) {
                String str = obj == null ? null : obj.toString();
                if (StringUtils.isBlank(str)) {
                    continue;
                }
                if (StringUtils.indexOf(str, ":") == -1) {
                    builder.append(str).append(',');
                } else {
                    String[] arr = StringUtils.split(str, ':');
                    builder.append(arr[0]).append(',');
                }
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            if (builder.length() > 0) {
                return builder.toString();
            }
        }

        return null;
    }

    protected Object[] toArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return args;
        }
        Object[] ret = new Object[args.length];
        for (int i = 0, len = args.length; i < len; i++) {
            Object arg = args[i];
            if (arg instanceof byte[]) {
                ret[i] = new String((byte[]) arg);
            } else if (arg instanceof char[]) {
                ret[i] = new String((char[]) arg);
            } else {
                ret[i] = arg;
            }
        }
        return ret;
    }
}
