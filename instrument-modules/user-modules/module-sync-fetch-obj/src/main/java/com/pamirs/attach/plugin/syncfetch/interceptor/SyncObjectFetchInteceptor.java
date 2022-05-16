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
        String key = advice.getTarget().getClass().getName() + "#" + advice.getBehaviorName();
        if (isInLinkAgentInvoke()) {
            logger.info("in linkAgent invoke ,will not save sync object: {}", key);
            return;
        }
        logger.info("success save sync object from : {}#{}", advice.getTarget().getClass().getName(), advice.getBehaviorName());
        SyncObjectService.saveSyncObject(key, new SyncObject()
                .addData(new SyncObjectData(advice.getTarget(), advice.getParameterArray(), advice.getReturnObj())));
    }

    private boolean isInLinkAgentInvoke() {
//        StackTraceElement[] stackTrace = new Exception().getStackTrace();
//        int num = 0;
//        for (StackTraceElement stackTraceElement : stackTrace) {
//            if (stackTraceElement.getClassName().equals("com.shulie.instrument.simulator.message.Messager")) {
//                num++;
//            }
//        }
//        return num != 1;
        return false;
    }
}
