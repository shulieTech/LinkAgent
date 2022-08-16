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
package com.pamirs.attach.plugin.aerospike.interceptor;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.BatchRead;
import com.aerospike.client.Key;
import com.aerospike.client.async.AsyncClient;
import com.aerospike.client.async.AsyncCluster;
import com.aerospike.client.cluster.Cluster;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.query.Statement;
import com.pamirs.attach.plugin.aerospike.AerospikeConstants;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * @author xiaobin.zfb
 * @since 2020/8/14 10:06 下午
 */
public class AreospikeOperateInterceptor extends TraceInterceptorAdaptor {
    @Override
    public String getPluginName() {
        return AerospikeConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return AerospikeConstants.PLUGIN_TYPE;
    }

    private String getMethod(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Key) {
                Key key = (Key) arg;
                return key.setName;
            } else if (arg instanceof Key[]) {
                Key[] keys = (Key[]) arg;
                if (keys == null || keys.length == 0) {
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                for (Object obj : keys) {
                    Key key = (Key) obj;
                    String setName = key.setName;
                    if (StringUtils.isNotBlank(setName)) {
                        builder.append(setName).append(',');
                    }
                }
                if (builder.length() > 0) {
                    builder.deleteCharAt(builder.length() - 1);
                }
                return builder.toString();
            } else if (arg instanceof List) {
                List list = (List) arg;
                if (list == null || list.isEmpty()) {
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                for (Object obj : list) {
                    if (!(obj instanceof BatchRead)) {
                        continue;
                    }
                    BatchRead batchRead = (BatchRead) obj;
                    Key key = batchRead.key;
                    if (key == null) {
                        continue;
                    }

                    String setName = key.setName;
                    if (StringUtils.isNotBlank(setName)) {
                        builder.append(setName).append(',');
                    }
                }
                if (builder.length() > 0) {
                    builder.deleteCharAt(builder.length() - 1);
                }
                return builder.toString();
            } else if (arg instanceof Statement) {
                Statement statement = (Statement) arg;
                return statement.getSetName();
            }
        }
        return "";
    }

    private String getService(String method, Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Key) {
                Key key = (Key) arg;
                return StringUtils.isBlank(key.namespace) ? method : key.namespace + ':' + method;
            } else if (arg instanceof Key[]) {
                Key[] keys = (Key[]) arg;
                if (keys == null || keys.length == 0) {
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                for (Object obj : keys) {
                    Key key = (Key) obj;
                    String namespace = key.namespace;
                    if (StringUtils.isNotBlank(namespace)) {
                        builder.append(namespace).append(',');
                    }
                }
                if (builder.length() > 0) {
                    builder.deleteCharAt(builder.length() - 1);
                }
                if (builder.length() > 0) {
                    return builder.toString() + ':' + method;
                }
                return method;
            } else if (arg instanceof List) {
                List list = (List) arg;
                if (list == null || list.isEmpty()) {
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                for (Object obj : list) {
                    if (!(obj instanceof BatchRead)) {
                        continue;
                    }
                    BatchRead batchRead = (BatchRead) obj;
                    Key key = batchRead.key;
                    if (key == null) {
                        continue;
                    }

                    String namespace = key.namespace;
                    if (StringUtils.isNotBlank(namespace)) {
                        builder.append(namespace).append(',');
                    }
                }
                if (builder.length() > 0) {
                    builder.deleteCharAt(builder.length() - 1);
                }
                if (builder.length() > 0) {
                    return builder.toString() + ':' + method;
                }
                return method;
            } else if (arg instanceof Statement) {
                Statement statement = (Statement) arg;
                String namespace = statement.getNamespace();
                return StringUtils.isBlank(namespace) ? method : namespace + ':' + method;
            }
        }
        return method;
    }


    @Override
    public void beforeFirst(Advice advice) {
        ClusterTestUtils.validateClusterTest();
        if (!Pradar.isClusterTest()) {
            return;
        }
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0) {
            return;
        }

        for (Object arg : args) {
            if (arg instanceof Key) {
                Key key = (Key) arg;
                /**
                 * 使用setName来进行隔离
                 */
                String setName = key.setName;
                if (!Pradar.isClusterTestPrefix(setName)) {
                    setName = Pradar.addClusterTestPrefix(setName);
                    ReflectionUtils.set(advice.getTarget(),"setName", setName );
                }
            } else if (arg instanceof Key[]) {
                Key[] keys = (Key[]) arg;
                if (keys == null || keys.length == 0) {
                    continue;
                }
                for (Object obj : keys) {
                    Key key = (Key) obj;
                    /**
                     * 使用setName来进行隔离
                     */
                    String setName = key.setName;
                    if (!Pradar.isClusterTestPrefix(setName)) {
                        setName = Pradar.addClusterTestPrefix(setName);
                        ReflectionUtils.set(advice.getTarget(), "setName", setName);
                    }
                }
            } else if (arg instanceof List) {
                List list = (List) arg;
                if (list == null || list.isEmpty()) {
                    continue;
                }
                for (Object obj : list) {
                    if (!(obj instanceof BatchRead)) {
                        continue;
                    }
                    BatchRead batchRead = (BatchRead) obj;
                    Key key = batchRead.key;
                    if (key == null) {
                        continue;
                    }

                    /**
                     * 使用setName来进行隔离
                     */
                    String setName = key.setName;
                    if (!Pradar.isClusterTestPrefix(setName)) {
                        setName = Pradar.addClusterTestPrefix(setName);
                        ReflectionUtils.set(advice.getTarget(), "setName", setName);
                    }
                }
            } else if (arg instanceof Statement) {
                Statement statement = (Statement) arg;
                /**
                 * 使用setName来进行隔离
                 */
                String setName = statement.getSetName();
                if (!Pradar.isClusterTestPrefix(setName)) {
                    setName = Pradar.addClusterTestPrefix(setName);
                    statement.setSetName(setName);
                }
            }
        }
    }

    private String getRemoteIp(Object target) {
        if (target instanceof AerospikeClient) {
            Cluster cluster = ReflectionUtils.get(target, "cluster");
            Node[] nodes = cluster.getNodes();
            if (nodes == null || nodes.length == 0) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (Node node : nodes) {
                builder.append(node.getHost().name).append(':').append(node.getHost().port).append(',');
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        }

        if (target instanceof AsyncClient) {
            AsyncCluster cluster = ReflectionUtils.get(target, "cluster");
            Node[] nodes = cluster.getNodes();
            if (nodes == null || nodes.length == 0) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (Node node : nodes) {
                builder.append(node.getHost().name).append(':').append(node.getHost().port).append(',');
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        }
        return null;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setService(getService(advice.getBehaviorName(), advice.getParameterArray()));
        spanRecord.setMethod(getMethod(advice.getParameterArray()));
        spanRecord.setRemoteIp(getRemoteIp(advice.getTarget()));
        spanRecord.setRequest(advice.getParameterArray());
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(advice.getReturnObj());
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(advice.getThrowable());
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }
}
