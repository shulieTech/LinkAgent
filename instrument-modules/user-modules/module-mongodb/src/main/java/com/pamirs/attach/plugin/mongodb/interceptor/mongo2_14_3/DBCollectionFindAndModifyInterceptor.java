package com.pamirs.attach.plugin.mongodb.interceptor.mongo2_14_3;

import java.util.concurrent.TimeUnit;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/20 4:18 下午
 */
public class DBCollectionFindAndModifyInterceptor extends AbstractDBCollectionInterceptor {

    @Override
    protected boolean check(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null) {
            return false;
        }
        return args.length == 8 || args.length == 10 || args.length == 11;
    }

    @Override
    protected boolean isRead() {
        return true;
    }

    @Override
    protected Object cutoffShadow(DBCollection ptDbCollection, Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args.length == 8) {
            return ptDbCollection.findAndModify((DBObject)args[0], (DBObject)args[1], (DBObject)args[2],
                (Boolean)args[3], (DBObject)args[4], (Boolean)args[5], (Boolean)args[6], (WriteConcern)args[7]);
        }
        if (args.length == 10) {
            return ptDbCollection.findAndModify((DBObject)args[0], (DBObject)args[1], (DBObject)args[2],
                (Boolean)args[3], (DBObject)args[4], (Boolean)args[5], (Boolean)args[6], (Long)args[7],
                (TimeUnit)args[8], (WriteConcern)args[9]);
        }
        if (args.length == 11) {
            return ptDbCollection.findAndModify((DBObject)args[0], (DBObject)args[1], (DBObject)args[2],
                (Boolean)args[3], (DBObject)args[4], (Boolean)args[5], (Boolean)args[6], (Boolean)args[7],
                (Long)args[8], (TimeUnit)args[9], (WriteConcern)args[10]);
        }
        throw new RuntimeException("this should never happened!");
    }
}
