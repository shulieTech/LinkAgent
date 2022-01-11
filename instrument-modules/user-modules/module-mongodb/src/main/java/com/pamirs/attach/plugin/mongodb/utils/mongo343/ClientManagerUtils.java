package com.pamirs.attach.plugin.mongodb.utils.mongo343;

import com.mongodb.Mongo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author angju
 * @date 2021/9/22 20:31
 */
public class ClientManagerUtils {

    private static volatile ClientManagerUtils clientManagerUtils = null;

    public ClientManagerUtils(){
        System.out.println("1");
    }

    public static Map<Mongo, Mongo> getBusClient2ptClientMapping(){
        if (clientManagerUtils == null){
            synchronized (ClientManagerUtils.class){
                if (clientManagerUtils != null){
                   return clientManagerUtils.busClient2ptClientMapping;
                }
                clientManagerUtils = new ClientManagerUtils();
                return clientManagerUtils.busClient2ptClientMapping;
            }
        }
        return clientManagerUtils.busClient2ptClientMapping;
    }
    private final Map<Mongo, Mongo> busClient2ptClientMapping
            = new ConcurrentHashMap<Mongo, Mongo>(8, 1);
}
