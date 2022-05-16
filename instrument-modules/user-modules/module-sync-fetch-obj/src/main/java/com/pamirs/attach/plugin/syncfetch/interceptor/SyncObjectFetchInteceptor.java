package com.pamirs.attach.plugin.syncfetch.interceptor;

import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.listener.ext.AdviceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Licey
 * @date 2022/5/16
 */
public class SyncObjectFetchInteceptor extends AdviceListener {
    private static final Logger logger = LoggerFactory.getLogger(SyncObjectFetchInteceptor.class);

    @Override
    public void after(Advice advice) throws Throwable {
        logger.info("success save sync object from : {}#{}", advice.getTarget().getClass().getName(), advice.getBehaviorName());
        String key = advice.getTarget().getClass().getName() + "#" + advice.getBehaviorName();
        SyncObjectService.saveSyncObject(key, new SyncObject()
                .addData(new SyncObjectData(advice.getTarget(), advice.getParameterArray(), advice.getReturnObj())));
    }
}
