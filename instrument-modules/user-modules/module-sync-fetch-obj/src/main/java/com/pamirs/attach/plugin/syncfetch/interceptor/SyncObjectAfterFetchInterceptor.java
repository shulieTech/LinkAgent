package com.pamirs.attach.plugin.syncfetch.interceptor;

import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.listener.ext.AdviceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Vector;

/**
 * @author Licey
 * @date 2022/5/16
 */
public class SyncObjectAfterFetchInterceptor extends AdviceListener {
    private static final Logger logger = LoggerFactory.getLogger(SyncObjectAfterFetchInterceptor.class);

    private boolean strongReference;

    public SyncObjectAfterFetchInterceptor(boolean strongReference) {
        this.strongReference = strongReference;
    }

    @Override
    public void after(Advice advice) throws Throwable {
        String key = SyncObjectFetchUtils.getKey(advice.getTargetClass(), advice.getBehaviorName());
        logger.info("success save sync object after invoked from : {}, reference:{}", key, strongReference ? "strong" : "weak");
        SyncObjectService.saveSyncObject(key, new SyncObject()
                .addData(new SyncObjectData(advice.getTarget(), advice.getBehaviorName(), advice.getParameterArray(), advice.getBehavior().getParameterTypes(), advice.getReturnObj(), strongReference)));
    }

}
