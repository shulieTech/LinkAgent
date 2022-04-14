package com.pamirs.attach.plugin.apache.hbase.interceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.pamirs.attach.plugin.apache.hbase.interceptor.shadowserver.HbaseMediatorConnection;
import com.pamirs.attach.plugin.apache.hbase.utils.ShadowConnectionHolder;
import com.pamirs.attach.plugin.apache.hbase.utils.ShadowConnectionHolder.Supplier;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowHbaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ClusterConnection;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.security.User;
import org.slf4j.Logger;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/04/14 4:37 PM
 */

@ListenerBehavior(isFilterClusterTest = true)
public class HConnectionShadowReplaceInterceptor extends CutoffInterceptorAdaptor {

    private final Logger logger = org.slf4j.LoggerFactory.getLogger(HConnectionShadowReplaceInterceptor.class);

    public static final String sf_token = "hbase.sf.token";

    public static final String sf_username = "hbase.sf.username";

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if (advice.getTarget() instanceof HbaseMediatorConnection) {
            return CutOffResult.passed();
        }
        Object target = advice.getTarget();
        if (!(target instanceof ClusterConnection)) {
            return CutOffResult.passed();
        }
        if (!Pradar.isClusterTest()) {
            return CutOffResult.passed();
        }
        ClusterConnection ptClusterConnection = ShadowConnectionHolder.computeIfAbsent((ClusterConnection)target,
            new Supplier() {
                @Override
                public ClusterConnection get(ClusterConnection busClusterConnection) {
                    Configuration busConfiguration = busClusterConnection.getConfiguration();
                    Configuration ptConfiguration = matching(busConfiguration);
                    if (ptConfiguration != null) {
                        try {
                            Connection prefConnection = ConnectionFactory.createConnection(ptConfiguration,
                                (ExecutorService)Reflect.on(busClusterConnection).get("batchPool"),
                                (User)Reflect.on(busClusterConnection).get("user"));
                            return (ClusterConnection)prefConnection;
                        } catch (IOException e) {
                            throw new RuntimeException("[hbase] create shadow connection fail!", e);
                        }
                    }
                    return null;
                }
            });
        if (ptClusterConnection != null) {
            return CutOffResult.cutoff(process(ptClusterConnection, advice.getBehaviorName(), advice.getParameterArray()));
        }

        return CutOffResult.passed();
    }

    private Object process(ClusterConnection ptClusterConnection, String behaviorName, Object[] args) throws Exception {
        if ("isMasterRunning".equals(behaviorName)) {
            return ptClusterConnection.isMasterRunning();
        } else if ("isTableAvailable".equals(behaviorName)) {
            if (args.length == 2) {
                if (args[0] instanceof byte[] && args[1] instanceof byte[][]) {
                    return ptClusterConnection.isTableAvailable((byte[])args[0], (byte[][])args[1]);
                } else if (args[0] instanceof TableName && args[1] instanceof byte[][]) {
                    return ptClusterConnection.isTableAvailable((TableName)args[0], (byte[][])args[1]);
                }
            } else if (args.length == 1) {
                if (args[0] instanceof byte[]) {
                    return ptClusterConnection.isTableAvailable((byte[])args[0]);
                } else if (args[0] instanceof TableName) {
                    return ptClusterConnection.isTableAvailable((TableName)args[0]);
                }
            }
        } else if ("locateRegion".equals(behaviorName)) {
            if (args.length == 2) {
                if (args[0] instanceof byte[] && args[1] instanceof byte[]) {
                    return ptClusterConnection.locateRegion((byte[])args[0], (byte[])args[1]);
                } else if (args[0] instanceof TableName && args[1] instanceof byte[]) {
                    return ptClusterConnection.locateRegion((TableName)args[0], (byte[])args[1]);
                }
            } else if (args.length == 1) {
                if (args[0] instanceof byte[]) {
                    return ptClusterConnection.locateRegion((byte[])args[0]);
                }
            } else if (args.length == 4) {
                ptClusterConnection.locateRegion((TableName)args[0], (byte[])args[1], (Boolean)args[2], (Boolean)args[3]);
            } else if (args.length == 5) {
                ptClusterConnection.locateRegion((TableName)args[0], (byte[])args[1], (Boolean)args[2], (Boolean)args[3],
                    (Integer)args[4]);
            }
        } else if ("clearRegionCache".equals(behaviorName)) {
            if (args.length == 1) {
                if (args[0] instanceof byte[]) {
                    ptClusterConnection.clearRegionCache((byte[])args[0]);
                } else if (args[0] instanceof TableName) {
                    ptClusterConnection.clearRegionCache((TableName)args[0]);
                }
            }
        } else if ("cacheLocation".equals(behaviorName)) {
            return ptClusterConnection.cacheLocation(args[0], args[1]);
        } else if ("deleteCachedRegionLocation".equals(behaviorName)) {
            return ptClusterConnection.deleteCachedRegionLocation();
        } else if ("relocateRegion".equals(behaviorName)) {
            return ptClusterConnection.relocateRegion();
        } else if ("updateCachedLocations".equals(behaviorName)) {
            return ptClusterConnection.updateCachedLocations();
        } else if ("locateRegions".equals(behaviorName)) {
            return ptClusterConnection.locateRegions();
        } else if ("getMaster".equals(behaviorName)) {
            return ptClusterConnection.getMaster();
        } else if ("getAdmin".equals(behaviorName)) {
            return ptClusterConnection.getAdmin();
        } else if ("getClient".equals(behaviorName)) {
            return ptClusterConnection.getClient();
        } else if ("getRegionLocation".equals(behaviorName)) {
            return ptClusterConnection.getRegionLocation();
        } else if ("clearCaches".equals(behaviorName)) {
            return ptClusterConnection.clearCaches();
        } else if ("getKeepAliveMasterService".equals(behaviorName)) {
            return ptClusterConnection.getKeepAliveMasterService();
        } else if ("isDeadServer".equals(behaviorName)) {
            return ptClusterConnection.isDeadServer();
        } else if ("getNonceGenerator".equals(behaviorName)) {
            return ptClusterConnection.getNonceGenerator();
        } else if ("getAsyncProcess".equals(behaviorName)) {
            return ptClusterConnection.getAsyncProcess();
        } else if ("getNewRpcRetryingCallerFactory".equals(behaviorName)) {
            return ptClusterConnection.getNewRpcRetryingCallerFactory();
        } else if ("getRpcRetryingCallerFactory".equals(behaviorName)) {
            return ptClusterConnection.getRpcRetryingCallerFactory();
        } else if ("getRpcControllerFactory".equals(behaviorName)) {
            return ptClusterConnection.getRpcControllerFactory();
        } else if ("getConnectionConfiguration".equals(behaviorName)) {
            return ptClusterConnection.getConnectionConfiguration();
        } else if ("isManaged".equals(behaviorName)) {
            return ptClusterConnection.isManaged();
        } else if ("getStatisticsTracker".equals(behaviorName)) {
            return ptClusterConnection.getStatisticsTracker();
        } else if ("getBackoffPolicy".equals(behaviorName)) {
            return ptClusterConnection.getBackoffPolicy();
        } else if ("getConnectionMetrics".equals(behaviorName)) {
            return ptClusterConnection.getConnectionMetrics();
        } else if ("hasCellBlockSupport".equals(behaviorName)) {
            return ptClusterConnection.hasCellBlockSupport();
        } else if ("getConfiguration".equals(behaviorName)) {
            return ptClusterConnection.getConfiguration();
        } else if ("getTable".equals(behaviorName)) {
            return ptClusterConnection.getTable();
        } else if ("getRegionLocator".equals(behaviorName)) {
            return ptClusterConnection.getRegionLocator();
        } else if ("isTableEnabled".equals(behaviorName)) {
            return ptClusterConnection.isTableEnabled();
        } else if ("isTableDisabled".equals(behaviorName)) {
            return ptClusterConnection.isTableDisabled();
        } else if ("listTables".equals(behaviorName)) {
            return ptClusterConnection.listTables();
        } else if ("getTableNames".equals(behaviorName)) {
            return ptClusterConnection.getTableNames();
        } else if ("listTableNames".equals(behaviorName)) {
            return ptClusterConnection.listTableNames();
        } else if ("getHTableDescriptor".equals(behaviorName)) {
            return ptClusterConnection.getHTableDescriptor();
        } else if ("processBatch".equals(behaviorName)) {
            return ptClusterConnection.processBatch();
        } else if ("processBatchCallback".equals(behaviorName)) {
            return ptClusterConnection.processBatchCallback();
        } else if ("setRegionCachePrefetch".equals(behaviorName)) {
            return ptClusterConnection.setRegionCachePrefetch();
        } else if ("getRegionCachePrefetch".equals(behaviorName)) {
            return ptClusterConnection.getRegionCachePrefetch();
        } else if ("getCurrentNrHRS".equals(behaviorName)) {
            return ptClusterConnection.getCurrentNrHRS();
        } else if ("getHTableDescriptorsByTableName".equals(behaviorName)) {
            return ptClusterConnection.getHTableDescriptorsByTableName();
        } else if ("getHTableDescriptors".equals(behaviorName)) {
            return ptClusterConnection.getHTableDescriptors();
        } else if ("isClosed".equals(behaviorName)) {
            return ptClusterConnection.isClosed();
        } else if ("getBufferedMutator".equals(behaviorName)) {
            return ptClusterConnection.getBufferedMutator();
        } else if ("close".equals(behaviorName)) {
            ptClusterConnection.close();
            return null;
        }
        throw throwError(behaviorName, args);
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
            "[hbase] shadow connection method invoke process error! behaviorName : " + behaviorName + " args : " + sb);
    }

    private Configuration matching(Configuration configuration) {
        String quorum = configuration.get(HConstants.ZOOKEEPER_QUORUM);
        String port = configuration.get(HConstants.ZOOKEEPER_CLIENT_PORT);
        String znode = configuration.get(HConstants.ZOOKEEPER_ZNODE_PARENT);
        Map<String, ShadowHbaseConfig> hbaseConfigMap = GlobalConfig.getInstance().getShadowHbaseServerConfigs();

        ShadowHbaseConfig ptConfig = null;
        SS:
        for (Map.Entry<String, ShadowHbaseConfig> entry : hbaseConfigMap.entrySet()) {
            String[] split = entry.getKey().split("\\|");
            ShadowHbaseConfig shadowHbaseConfig = entry.getValue();
            if (split.length == 3) {
                if (logger.isInfoEnabled()) {
                    logger.info(
                        "HConnectionShadowReplaceInterceptor business config quorums:{}, port:{}, znode:{} ---------- "
                            + "perfomanceTest config quorums:{}, port:{}, znode:{}", quorum, port, znode,
                        shadowHbaseConfig.getQuorum(), shadowHbaseConfig.getPort(), shadowHbaseConfig.getZnode());
                }
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

        if (ptConfig == null) {
            logger.warn("hbase shadow base config not find , business config key is {}", quorum + "|" + port + "|" + znode);
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
}
