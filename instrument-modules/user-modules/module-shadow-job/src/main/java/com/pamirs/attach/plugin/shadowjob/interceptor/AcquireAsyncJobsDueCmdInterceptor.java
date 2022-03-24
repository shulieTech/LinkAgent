package com.pamirs.attach.plugin.shadowjob.interceptor;

import com.pamirs.attach.plugin.shadowjob.obj.PTAcquireAsyncJobsDueRunnable;
import com.pamirs.attach.plugin.shadowjob.util.AcquireAsyncJobsDueRunnableUtils;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.activiti.engine.impl.asyncexecutor.AcquireAsyncJobsDueRunnable;
import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;

import java.lang.reflect.Field;

/**
 * @author angju
 * @date 2021/10/14 10:14
 */
public class AcquireAsyncJobsDueCmdInterceptor extends CutoffInterceptorAdaptor {

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        AsyncExecutor asyncExecutor = (AsyncExecutor) advice.getParameterArray()[0];
        if (AcquireAsyncJobsDueRunnableUtils.inited(asyncExecutor)){
            return CutOffResult.passed();
        }

        PTAcquireAsyncJobsDueRunnable pt = new PTAcquireAsyncJobsDueRunnable(asyncExecutor);
//        AcquireAsyncJobsDueRunnableUtils.setValue(advice.getTarget(), pt);
        new Thread(pt).start();//启动影子
        return CutOffResult.passed();
    }


    private AsyncExecutor getAsyncExecutor(AcquireAsyncJobsDueRunnable bus){
        Field asyncExecutorField = null;
        boolean asyncExecutorAccess = false;
        boolean e = false;
        AsyncExecutor asyncExecutor = null;
        try {
            asyncExecutorField = bus.getClass().getDeclaredField("asyncExecutor");
            asyncExecutorAccess = asyncExecutorField.isAccessible();
            asyncExecutorField.setAccessible(true);
            asyncExecutor = (AsyncExecutor) asyncExecutorField.get(bus);
            return asyncExecutor;
        } catch (NoSuchFieldException noSuchFieldException) {
            LOGGER.error("getAsyncExecutor error, msg is noSuchFieldException");
        } catch (IllegalAccessException illegalAccessException) {
            LOGGER.error("getAsyncExecutor error, msg is illegalAccessException");
        } finally {
            if (asyncExecutorField != null){
                asyncExecutorField.setAccessible(asyncExecutorAccess);
            }
            return asyncExecutor;
        }
    }
}
