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
package com.pamirs.attach.plugin.apache.hbase.interceptor.shadowserver;

import com.pamirs.attach.plugin.common.datasource.hbaseserver.MediatorConnection;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.internal.config.ShadowHbaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.security.User;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Hbase影子库协调者
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.apache.hbase.interceptor.shadowserver
 * @Date 2021/4/19 4:45 下午
 */
public class HbaseMediatorConnection extends MediatorConnection<Connection> implements Connection {

    @Override
    public void closeClusterTest() {
        if (performanceTestConnection != null) {
            try {
                performanceTestConnection.close();
                performanceTestConnection = null;
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public Connection initConnection() {
        try {
            Object ptConfiguration = matching(args[0]);
            if (null == ptConfiguration) {
                throw new PressureMeasureError("Hbase 影子Server未找到相关配置，请检查配置是否正确");
            }
            return ConnectionFactory.createConnection((Configuration) ptConfiguration, (ExecutorService) args[1], (User) args[2]);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Object matching(Object arg1) {
        if (!(arg1 instanceof Configuration)) {
            return null;
        }
        Configuration configuration = (Configuration) arg1;
        String quorum = configuration.get(HConstants.ZOOKEEPER_QUORUM);
        String port = configuration.get(HConstants.ZOOKEEPER_CLIENT_PORT);
        String znode = configuration.get(HConstants.ZOOKEEPER_ZNODE_PARENT);
        Map<String, ShadowHbaseConfig> hbaseConfigMap = GlobalConfig.getInstance().getShadowHbaseServerConfigs();

        ShadowHbaseConfig ptConfig = null;
        SS:
        for (Map.Entry<String, ShadowHbaseConfig> entry : hbaseConfigMap.entrySet()) {
            String[] split = entry.getKey().split("\\|");
            if (split.length == 3) {
                logger.info("business config quorums:{}, port:{}, znode:{} ---------- " +
                                "perfomanceTest config quorums:{}, port:{}, znode:{}",
                        quorum, port, znode, split[0], split[1], split[2]);
                if (!split[1].equals(port) || !split[2].equals(znode)) {
                    continue;
                }
                List<String> quorums = Arrays.asList(split[0].split(","));
                String[] bquorums = quorum.split(",");
                for (String bquorum : bquorums) {
                    if (!quorums.contains(bquorum)) {
                        continue SS;
                    }
                }
                ptConfig = entry.getValue();
                break;
            } else {
                logger.error("business config key is error:{}", entry.getKey());
            }
        }

        if (ptConfig != null) {
            Configuration ptConfiguration = HBaseConfiguration.create();
            ptConfiguration.addResource(configuration);

            ptConfiguration.set(HConstants.ZOOKEEPER_CLIENT_PORT, ptConfig.getPort());
            ptConfiguration.set(HConstants.ZOOKEEPER_QUORUM, ptConfig.getQuorum());
            ptConfiguration.set(HConstants.ZOOKEEPER_ZNODE_PARENT, ptConfig.getZnode());
            if (null != ptConfig.getToken()) {
                ptConfiguration.set(sf_token, ptConfig.getToken());
            }
            if (null != ptConfig.getUsername()) {
                ptConfiguration.set(sf_username, ptConfig.getUsername());
            }

            if (null != ptConfig.getParams()) {
                for (Map.Entry<String, String> entry : ptConfig.getParams().entrySet()) {
                    ptConfiguration.set(entry.getKey(), entry.getValue());
                }
            }
            return ptConfiguration;
        }
        return null;
    }

    @Override
    public Configuration getConfiguration() {
        if (Pradar.isClusterTest()) {
            if (GlobalConfig.getInstance().isShadowHbaseServer()) {
                return getPerformanceTestConnection().getConfiguration();
            }
        }
        return businessConnection.getConfiguration();
    }

    @Override
    public Table getTable(TableName tableName) throws IOException {
        if (Pradar.isClusterTest()) {
            if (GlobalConfig.getInstance().isShadowHbaseServer()) {
                return getPerformanceTestConnection().getTable(tableName);
            }
        }
        return businessConnection.getTable(tableName);
    }

    @Override
    public Table getTable(TableName tableName, ExecutorService pool) throws IOException {
        if (Pradar.isClusterTest()) {
            if (GlobalConfig.getInstance().isShadowHbaseServer()) {
                return getPerformanceTestConnection().getTable(tableName, pool);
            }
        }
        return businessConnection.getTable(tableName, pool);
    }

    @Override
    public BufferedMutator getBufferedMutator(TableName tableName) throws IOException {
        if (Pradar.isClusterTest()) {
            if (GlobalConfig.getInstance().isShadowHbaseServer()) {
                return getPerformanceTestConnection().getBufferedMutator(tableName);
            }
        }
        return businessConnection.getBufferedMutator(tableName);
    }

    @Override
    public BufferedMutator getBufferedMutator(BufferedMutatorParams bufferedMutatorParams) throws IOException {
        if (Pradar.isClusterTest()) {
            if (GlobalConfig.getInstance().isShadowHbaseServer()) {
                return getPerformanceTestConnection().getBufferedMutator(bufferedMutatorParams);
            }
        }
        return businessConnection.getBufferedMutator(bufferedMutatorParams);
    }

    @Override
    public RegionLocator getRegionLocator(TableName tableName) throws IOException {
        if (Pradar.isClusterTest()) {
            if (GlobalConfig.getInstance().isShadowHbaseServer()) {
                return getPerformanceTestConnection().getRegionLocator(tableName);
            }
        }
        return businessConnection.getRegionLocator(tableName);
    }

    @Override
    public Admin getAdmin() throws IOException {
        if (Pradar.isClusterTest()) {
            if (GlobalConfig.getInstance().isShadowHbaseServer()) {
                return getPerformanceTestConnection().getAdmin();
            }
        }
        return businessConnection.getAdmin();
    }

    @Override
    public void close() throws IOException {
        if (Pradar.isClusterTest()) {
            if (GlobalConfig.getInstance().isShadowHbaseServer()) {
                getPerformanceTestConnection().close();
            }
        }
        businessConnection.close();
    }

    @Override
    public boolean isClosed() {
        if (Pradar.isClusterTest()) {
            if (GlobalConfig.getInstance().isShadowHbaseServer()) {
                return getPerformanceTestConnection().isClosed();
            }
        }
        return businessConnection.isClosed();
    }

    @Override
    public void abort(String s, Throwable throwable) {
        if (Pradar.isClusterTest()) {
            if (GlobalConfig.getInstance().isShadowHbaseServer()) {
                getPerformanceTestConnection().abort(s, throwable);
            }
        }
        businessConnection.abort(s, throwable);
    }

    @Override
    public boolean isAborted() {
        if (Pradar.isClusterTest()) {
            if (GlobalConfig.getInstance().isShadowHbaseServer()) {
                return getPerformanceTestConnection().isAborted();
            }
        }
        return businessConnection.isAborted();
    }
}
