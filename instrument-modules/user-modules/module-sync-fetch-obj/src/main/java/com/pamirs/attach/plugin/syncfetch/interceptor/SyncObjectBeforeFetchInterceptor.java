package com.pamirs.attach.plugin.syncfetch.interceptor;

import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.listener.ext.AdviceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncObjectBeforeFetchInterceptor extends AdviceListener {

    private static final Logger logger = LoggerFactory.getLogger(SyncObjectBeforeFetchInterceptor.class);

    @Override
    public void before(Advice advice) throws Throwable {
        String key = String.format("%s#%s", advice.getTarget().getClass().getName(), advice.getBehaviorName());
        logger.info("success save sync object before invoke from : {}", key);
        SyncObjectService.saveSyncObject(key, new SyncObject()
                .addData(new SyncObjectData(advice.getTarget(), advice.getBehaviorName(), advice.getParameterArray(), advice.getBehavior().getParameterTypes(), advice.getReturnObj())));
    }


}
