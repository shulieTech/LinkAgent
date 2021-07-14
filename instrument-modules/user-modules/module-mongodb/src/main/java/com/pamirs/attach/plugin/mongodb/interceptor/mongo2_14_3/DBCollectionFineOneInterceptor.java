package com.pamirs.attach.plugin.mongodb.interceptor.mongo2_14_3;

import java.lang.reflect.Method;

import com.mongodb.DBCollection;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author angju
 * @date 2021/5/10 20:56
 */
public class DBCollectionFineOneInterceptor extends AbstractDBCollectionInterceptor {

    private Method findOneMethod = null;

    @Override
    protected boolean check(Advice advice) {
        return true;
    }

    @Override
    protected Object cutoffShadow(DBCollection ptDbCollection, Advice advice) throws Throwable {
        return getFindOneMethod(ptDbCollection)
            .invoke(ptDbCollection, advice.getParameterArray()[0], advice.getParameterArray()[1],
                advice.getParameterArray()[2], advice.getParameterArray()[3], advice.getParameterArray()[4],
                advice.getParameterArray()[5]);
    }

    @Override
    protected boolean isRead() {
        return true;
    }

    private Method getFindOneMethod(DBCollection dbCollection) {
        if (findOneMethod == null) {
            Method[] methods = dbCollection.getClass().getSuperclass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("findOne") && method.getParameterTypes().length == 6) {
                    findOneMethod = method;
                    findOneMethod.setAccessible(true);
                    return findOneMethod;
                }
            }
        }
        return findOneMethod;
    }
}
