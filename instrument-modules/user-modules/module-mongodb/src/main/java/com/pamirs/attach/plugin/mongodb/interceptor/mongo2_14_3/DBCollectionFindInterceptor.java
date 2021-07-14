package com.pamirs.attach.plugin.mongodb.interceptor.mongo2_14_3;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/20 4:18 下午
 */
public class DBCollectionFindInterceptor extends AbstractDBCollectionInterceptor {

    @Override
    protected boolean check(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null) {
            return false;
        }
        return args.length == 0 || args.length == 1 || args.length == 2;
    }

    @Override
    protected boolean isRead() {
        return true;
    }

    @Override
    protected Object cutoffShadow(DBCollection ptDbCollection, Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args.length == 0) {
            return ptDbCollection.find();
        }
        if (args.length == 1) {
            return ptDbCollection.find((DBObject)args[0]);
        }
        if (args.length == 2) {
            return ptDbCollection.find((DBObject)args[0], (DBObject)args[1]);
        }
        throw new RuntimeException("this should never happened!");
    }

}
