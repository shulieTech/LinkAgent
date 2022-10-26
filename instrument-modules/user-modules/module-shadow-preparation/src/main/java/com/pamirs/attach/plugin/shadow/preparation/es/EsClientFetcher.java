package com.pamirs.attach.plugin.shadow.preparation.es;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.internal.config.ShadowEsServerConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;

import java.util.*;
import java.util.stream.Collectors;

public class EsClientFetcher {

    private static String es_client_sync_key = "org.elasticsearch.client.RestClient";

    private static WeakHashMap<Object, String> bizClients = new WeakHashMap<>();
    private static WeakHashMap<Object, String> shadowClients = new WeakHashMap<>();

    public static synchronized void refreshClients() {
        fetchClientsForEs(SyncObjectService.removeSyncObject(es_client_sync_key));
    }

    private static void fetchClientsForEs(SyncObject syncObject) {
        if (syncObject == null) {
            return;
        }
        Set<String> shadowHosts = fetchShadowServerHosts();

        for (SyncObjectData data : syncObject.getDatas()) {
            Object[] args = data.getArgs();
            Optional<Object> optional = Arrays.stream(args).filter(o -> o instanceof List && ((List) o).get(0).getClass().getName().equals("org.elasticsearch.client.Node")).findAny();
            List nodes = (List) optional.get();
            List<String> hosts = new ArrayList<>();
            for (Object node : nodes) {
                Object host = ReflectionUtils.get(node, "host");
                hosts.add(ReflectionUtils.invoke(host, "toHostString"));
            }
            Collections.sort(hosts);
            String hostString = hosts.stream().collect(Collectors.joining(","));
            if (shadowHosts.contains(hostString)) {
                shadowClients.put(data.getTarget(), hostString);
            } else {
                bizClients.put(data.getTarget(), hostString);
            }
        }
    }

    private static Set<String> fetchShadowServerHosts() {
        boolean shadowEsServer = GlobalConfig.getInstance().isShadowEsServer();
        if (!shadowEsServer) {
            return Collections.emptySet();
        }
        Set<String> nodeStrings = new HashSet<>();
        Collection<ShadowEsServerConfig> values = GlobalConfig.getInstance().getShadowEsServerConfigs().values();
        for (ShadowEsServerConfig config : values) {
            List<String> nodes = config.getPerformanceTestNodes();
            Collections.sort(nodes);
            nodeStrings.add(nodes.stream().collect(Collectors.joining(",")));
        }
        return nodeStrings;
    }

    public static Object getShadowClient(String nodes) {
        return getClient(nodes, shadowClients);
    }

    public static Object getBizClient(String nodes) {
        return getClient(nodes, bizClients);
    }

    private static Object getClient(String nodes, WeakHashMap<Object, String> clients) {
        String[] split = nodes.split(",");
        String collect = Arrays.stream(split).sorted().collect(Collectors.joining(","));
        for (Map.Entry<Object, String> entry : clients.entrySet()) {
            if (entry.getValue().equals(collect)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static Set<String> getShadowKeys(){
        return new HashSet<String>(shadowClients.values());
    }

    public static ClassLoader getBizClassLoader(){
        return bizClients.keySet().iterator().next().getClass().getClassLoader();
    }

    public static int getShadowClientNum() {
        return shadowClients.size();
    }

    public static void removeShadowClients(Collection<String> keys) {
        Iterator<Map.Entry<Object, String>> iterator = shadowClients.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Object, String> next = iterator.next();
            if (keys.contains(next.getValue())) {
                iterator.remove();
            }
        }
    }

}
