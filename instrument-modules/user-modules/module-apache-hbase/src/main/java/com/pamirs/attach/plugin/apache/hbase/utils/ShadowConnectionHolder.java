package com.pamirs.attach.plugin.apache.hbase.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.hbase.client.ClusterConnection;
import org.apache.hadoop.hbase.client.Connection;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/04/14 5:01 PM
 */
public class ShadowConnectionHolder {

    private static final Map<ClusterConnection, ClusterConnection> CACHE
        = new HashMap<ClusterConnection, ClusterConnection>();
    private static final Set<Connection> allPtConnection = new HashSet<Connection>();

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

    public static boolean isPtConnection(Connection connection) {
        return allPtConnection.contains(connection);
    }

    public static void clear() {
        CACHE.clear();
    }
}
