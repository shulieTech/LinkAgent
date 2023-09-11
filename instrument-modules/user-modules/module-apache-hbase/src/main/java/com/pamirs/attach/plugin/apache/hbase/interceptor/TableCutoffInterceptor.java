/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.apache.hbase.interceptor;

import com.pamirs.attach.plugin.apache.hbase.utils.ShadowConnectionHolder;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @Description 解决Table对象复用问题
 * @Author ocean_wll
 * @Date 2022/4/28 6:28 下午
 */
public class TableCutoffInterceptor extends CutoffInterceptorAdaptor {

    final Logger logger = LoggerFactory.getLogger(TableCutoffInterceptor.class);

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        HTable hTable = (HTable) advice.getTarget();
        String tableName = hTable.getName().getNameAsString();
        if ("hbase:meta".equals(tableName)) {
            return CutOffResult.PASSED;
        }
        if (!Pradar.isClusterTest()) {
            return CutOffResult.PASSED;
        }
        // 是否用影子表
        if (GlobalConfig.getInstance().isShadowTableReplace() && Pradar.isClusterTestPrefix(tableName)) {
            return CutOffResult.PASSED;
        }

        if (!GlobalConfig.getInstance().isShadowTableReplace() && !Pradar.isClusterTestPrefix(tableName)) {
            return CutOffResult.PASSED;
        }
        ClusterConnection clusterConnection = (ClusterConnection) hTable.getConnection();

        ClusterConnection ptClusterConnection;
        if (GlobalConfig.getInstance().isShadowHbaseServer() && !ShadowConnectionHolder.isPtConnection(clusterConnection)) {
            ptClusterConnection = ShadowConnectionHolder.computeIfAbsent(clusterConnection,
                    new ShadowConnectionHolder.Supplier() {
                        @Override
                        public ClusterConnection get(ClusterConnection busClusterConnection) {
                            Configuration busConfiguration = ShadowConnectionHolder.getConfiguration(busClusterConnection);
                            Configuration ptConfiguration = ShadowConnectionHolder.matching(busConfiguration);
                            if (ptConfiguration != null) {
                                try {
                                    Connection prefConnection = ConnectionFactory.createConnection(ptConfiguration,
                                            (ExecutorService) ReflectionUtils.get(busClusterConnection, "batchPool"),
                                            (User) ReflectionUtils.get(busClusterConnection, "user"));
                                    return (ClusterConnection) prefConnection;
                                } catch (IOException e) {
                                    throw new RuntimeException("[hbase-htable] create shadow connection fail!", e);
                                }
                            }
                            return null;
                        }
                    });
        } else {
            ptClusterConnection = clusterConnection;
        }

        if (ptClusterConnection != null) {
            Table table = ptClusterConnection.getTable(TableName.valueOf(hTable.getName().getNameAsString()));
            if (table instanceof HTable) {
                hTable = (HTable) table;
            }
            return CutOffResult.cutoff(process(hTable, advice.getBehaviorName(), advice.getParameterArray()));
        } else {
            Configuration busConfiguration = ShadowConnectionHolder.getConfiguration(clusterConnection);

            String quorum = busConfiguration.get(HConstants.ZOOKEEPER_QUORUM);
            String port = busConfiguration.get(HConstants.ZOOKEEPER_CLIENT_PORT);
            String znode = busConfiguration.get(HConstants.ZOOKEEPER_ZNODE_PARENT);

            throw new PressureMeasureError(
                    "[hbase-htable]hbase未配置影子库, TableCutoffInterceptor business config quorums:  " + quorum
                            + " +, port: " + port + ", znode:" + znode + " ---------- " + advice.getBehaviorName());
        }
    }


    private Object process(HTable hTable, String behaviorName, Object[] args) throws Exception {
        if ("get".equals(behaviorName)) {
            if (args.length == 1 && args[0] instanceof org.apache.hadoop.hbase.client.Get) {
                return hTable.get(((Get) args[0]));
            }
        } else if ("append".equals(behaviorName)) {
            if (args[0] instanceof Append) {
                return hTable.append((Append) args[0]);
            }
        } else if ("increment".equals(behaviorName)) {
            if (args[0] instanceof Increment) {
                return hTable.increment((Increment) args[0]);
            }
        } else if ("exists".equals(behaviorName)) {
            if (args[0] instanceof Get) {
                return hTable.exists((Get) args[0]);
            }
        } else if ("existsAll".equals(behaviorName)) {
            if (args[0] instanceof List) {
                return hTable.existsAll((List<Get>) args[0]);
            }
        } else if ("getScanner".equals(behaviorName)) {
            if (args.length == 1) {
                if (args[0] instanceof Scan) {
                    return hTable.getScanner((Scan) args[0]);
                } else if (args[0] instanceof byte[]) {
                    return hTable.getScanner((byte[]) args[0]);
                }
            }
            if (args.length == 2) {
                if (args[0] instanceof byte[] && args[1] instanceof byte[]) {
                    return hTable.getScanner((byte[]) args[0], (byte[]) args[1]);
                }
            }
        } else if ("put".equals(behaviorName)) {
            if (args[0] instanceof Put) {
                hTable.put((Put) args[0]);
                return null;
            } else if (args[0] instanceof List) {
                hTable.put((List<Put>) args[0]);
                return null;
            }
        } else if ("checkAndPut".equals(behaviorName)) {
            if (args.length == 5) {
                return hTable.checkAndPut((byte[]) args[0], (byte[]) args[1], (byte[]) args[2], (byte[]) args[3], (Put) args[4]);
            } else if (args.length == 6) {
                return hTable.checkAndPut((byte[]) args[0], (byte[]) args[1], (byte[]) args[2], (CompareFilter.CompareOp) args[3], (byte[]) args[4], (Put) args[5]);
            }
        } else if ("delete".equals(behaviorName)) {
            if (args[0] instanceof Delete) {
                hTable.delete((Delete) args[0]);
                return null;
            } else if (args[0] instanceof List) {
                hTable.delete((List<Delete>) args[0]);
                return null;
            }
        } else if ("checkAndDelete".equals(behaviorName)) {
            if (args.length == 5) {
                return hTable.checkAndDelete((byte[]) args[0], (byte[]) args[1], (byte[]) args[2], (byte[]) args[3], (Delete) args[4]);
            } else if (args.length == 6) {
                return hTable.checkAndDelete((byte[]) args[0], (byte[]) args[1], (byte[]) args[2], (CompareFilter.CompareOp) args[3], (byte[]) args[4], (Delete) args[5]);
            }
        } else if ("mutateRow".equals(behaviorName)) {
            if (args[0] instanceof RowMutations) {
                hTable.mutateRow((RowMutations) args[0]);
                return null;
            }

        } else if ("checkAndMutate".equals(behaviorName)) {
            if (args.length == 6) {
                return hTable.checkAndMutate((byte[]) args[0], (byte[]) args[1], (byte[]) args[2], (CompareFilter.CompareOp) args[3], (byte[]) args[4], (RowMutations) args[5]);
            }
        }
        return throwError(behaviorName, args);
    }


    private RuntimeException throwError(String behaviorName, Object[] args) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object arg : args) {
            if (!first) {
                sb.append(",");
            }
            sb.append(arg.getClass().getName());
            first = false;
        }
        return new RuntimeException(
                "[hbase-htable] shadow hTable method invoke process error! behaviorName : " + behaviorName + " args : " + sb);
    }
}
