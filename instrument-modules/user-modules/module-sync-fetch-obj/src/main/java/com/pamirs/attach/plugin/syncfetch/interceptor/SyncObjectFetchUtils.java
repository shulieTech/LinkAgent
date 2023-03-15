package com.pamirs.attach.plugin.syncfetch.interceptor;

import com.pamirs.attach.plugin.syncfetch.SyncPlugin;
import com.shulie.instrument.simulator.api.util.StringUtil;

import java.util.List;
import java.util.Vector;

public class SyncObjectFetchUtils {

    public static final List<String> keys = new Vector<String>();

    static {
        String values = System.getProperty("simulator.inner.module.syncfetch");
        for (String key : values.split(",")) {
            if (key.endsWith(SyncPlugin.STRONG_REFERENCE_FLAG)) {
                key = key.substring(0, key.length() - SyncPlugin.STRONG_REFERENCE_FLAG.length());
            }
            keys.add(key);
        }
    }

    public static String getKey(Class clazz, String method) {
        if (clazz == null) {
            return "";
        }
        String key = clazz.getName();
        //<init>是切构造函数时的方法名称
        if ((StringUtil.isEmpty(method) || "<init>".equals(method)) && keys.contains(key)) {
            return key;
        }
        key = key + "#" + method;
        if (keys.contains(key)) {
            return key;
        }
        return getKey(clazz.getSuperclass(), method);
    }

}
