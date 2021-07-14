package com.pamirs.attach.plugin.mongodb.interceptor.mongo2_14_3;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author angju
 * @date 2021/5/10 20:56
 */
public class DBCollectionCountInterceptor extends AbstractDBCollectionInterceptor {

    @Override
    protected boolean check(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null) {
            return false;
        }
        return args.length == 2;
    }

    @Override
    protected boolean isRead() {
        return true;
    }

    @Override
    protected Object cutoffShadow(DBCollection ptDbCollection, Advice advice) throws Throwable {
        Object[] args = advice.getParameterArray();
        return ptDbCollection.count((DBObject)args[0], (ReadPreference)args[1]);
    }

}
