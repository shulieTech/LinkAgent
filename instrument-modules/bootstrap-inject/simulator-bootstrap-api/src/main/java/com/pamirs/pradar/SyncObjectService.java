package com.pamirs.pradar;

import com.pamirs.pradar.bean.SyncObject;

import java.util.Hashtable;
import java.util.Map;

/**
 * @author Licey
 * @date 2022/5/16
 */
public class SyncObjectService {
    private final static Map<String, SyncObject> syncObjectMap = new Hashtable<String, SyncObject>();

    public synchronized static void saveSyncObject(String key, SyncObject data) {
        if (syncObjectMap.containsKey(key)) {
            syncObjectMap.get(key).addData(data.getDatas());
        } else {
            syncObjectMap.put(key, data);
        }
    }

    public static SyncObject getSyncObject(String key) {
        return syncObjectMap.get(key);
    }

    public static SyncObject removeSyncObject(String key){
        return syncObjectMap.remove(key);
    }
}
