package com.pamirs.attach.plugin.apache.hbase.utils;

import com.pamirs.attach.plugin.apache.hbase.interceptor.HConnectionShadowReplaceInterceptor;
import com.pamirs.pradar.internal.config.ShadowHbaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.ClusterConnection;
import org.apache.hadoop.hbase.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/04/14 5:01 PM
 */
public class ShadowConnectionHolder {

    private static final Logger logger = LoggerFactory.getLogger(ShadowConnectionHolder.class);

    private static final Map<ClusterConnection, ClusterConnection> CACHE
            = new HashMap<ClusterConnection, ClusterConnection>();
    private static final Set<Connection> allPtConnection = new HashSet<Connection>();

    public static final String sf_token = "hbase.sf.token";

    public static final String sf_username = "hbase.sf.username";

    private static Field confField;

    public static ClusterConnection computeIfAbsent(ClusterConnection busClusterConnection, Supplier supplier) {
        ClusterConnection ptClusterConnection = CACHE.get(busClusterConnection);
        if (ptClusterConnection == null) {
            synchronized (ShadowConnectionHolder.class) {
                ptClusterConnection = CACHE.get(busClusterConnection);
                if (ptClusterConnection == null) {
                    ptClusterConnection = supplier.get(busClusterConnection);
                    if (ptClusterConnection != null) {
                        CACHE.put(busClusterConnection, ptClusterConnection);
                        allPtConnection.add(ptClusterConnection);
                    }
                }
            }
        }
        return ptClusterConnection;
    }

    public static void setShadowConnection(Connection prefConnection) {
        allPtConnection.add(prefConnection);
    }

    public interface Supplier {

        ClusterConnection get(ClusterConnection busClusterConnection);

    }

    /**
     * 因为在 com.pamirs.attach.plugin.apache.hbase.interceptor.TableCutoffInterceptor 中 create ptConnection时还是会调用 getConfiguration方法
     * 所以这个要多加一个判断是否是影子connection
     *
     * @param connection
     * @return
     */
    public static boolean isPtConnection(ClusterConnection connection) {
        boolean isPtCon = allPtConnection.contains(connection);
        if (!isPtCon) {
            isPtCon = isClusterConnection(connection);
        }
        return isPtCon;
    }

    public static void clear() {
        CACHE.clear();
    }

    public static Configuration matching(Configuration configuration) {
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

    private static boolean isClusterConnection(ClusterConnection connection) {
        Configuration configuration = getConfiguration(connection);
        String quorum = configuration.get(HConstants.ZOOKEEPER_QUORUM);
        String port = configuration.get(HConstants.ZOOKEEPER_CLIENT_PORT);
        String znode = configuration.get(HConstants.ZOOKEEPER_ZNODE_PARENT);

        Map<String, ShadowHbaseConfig> hbaseConfigMap = GlobalConfig.getInstance().getShadowHbaseServerConfigs();
        for (Map.Entry<String, ShadowHbaseConfig> entry : hbaseConfigMap.entrySet()) {
            ShadowHbaseConfig shadowHbaseConfig = entry.getValue();
            if (quorum.equals(shadowHbaseConfig.getQuorum()) && port.equals(shadowHbaseConfig.getPort()) && znode.equals(shadowHbaseConfig.getZnode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取connection的configuration，因为 (clusterConnection).getConfiguration() 已经被增强过了，所以通过反射去获取configuration对象
     *
     * @param clusterConnection
     * @return
     * @see HConnectionShadowReplaceInterceptor
     */
    public static Configuration getConfiguration(ClusterConnection clusterConnection) {
        Configuration conf = null;
        try {
            initConf(clusterConnection);
            conf = Reflect.on(clusterConnection).get(confField);
        } catch (Throwable t) {
            // ignore
        }
        if (conf == null) {
            conf = clusterConnection.getConfiguration();
        }
        return conf;
    }

    private static void initConf(Object target) {
        if (confField != null) {
            return;
        }
        try {
            confField = target.getClass().getDeclaredField("conf");
            confField.setAccessible(true);
        } catch (Throwable e) {
            logger.error("TableCutoffInterceptor get conf field error", e);
        }
    }
}
