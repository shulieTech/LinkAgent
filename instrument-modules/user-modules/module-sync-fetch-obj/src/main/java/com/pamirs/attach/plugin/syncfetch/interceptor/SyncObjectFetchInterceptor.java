package com.pamirs.attach.plugin.syncfetch.interceptor;

import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.listener.ext.AdviceListener;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * @author Licey
 * @date 2022/5/16
 */
public class SyncObjectFetchInterceptor extends AdviceListener {
    private static final Logger logger = LoggerFactory.getLogger(SyncObjectFetchInterceptor.class);

    public static final List<String> keys = new Vector<String>();

    @Override
    public void after(Advice advice) throws Throwable {
        initKeys();
        String key = getKey(advice.getTarget().getClass(), advice.getBehaviorName());
        logger.info("success save sync object from : {}", key);
        SyncObjectService.saveSyncObject(key, new SyncObject()
                .addData(new SyncObjectData(advice.getTarget(),advice.getBehaviorName(), advice.getParameterArray(), advice.getBehavior().getParameterTypes(), advice.getReturnObj())));
    }

    private synchronized void initKeys() {
        if (keys.size() > 0) {
            return;
        }
        String values = System.getProperty("simulator.inner.module.syncfetch");
        keys.addAll(Arrays.asList(values.split(",")));
    }

    private String getKey(Class clazz, String method) {
        if (clazz == null || StringUtil.isEmpty(method)) {
            return "";
        }
        String key = clazz.getName() + "#" + method;
        if (keys.contains(key)) {
            return key;
        }
        return getKey(clazz.getSuperclass(), method);
    }
}
