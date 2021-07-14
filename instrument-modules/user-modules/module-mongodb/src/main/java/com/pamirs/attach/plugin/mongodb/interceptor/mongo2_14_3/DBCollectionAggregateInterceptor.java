package com.pamirs.attach.plugin.mongodb.interceptor.mongo2_14_3;

import java.util.List;

import com.mongodb.AggregationOptions;
import com.mongodb.DBCollection;
import com.mongodb.ReadPreference;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/20 4:18 下午
 */
public class DBCollectionAggregateInterceptor extends AbstractDBCollectionInterceptor {

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
    protected Object cutoffShadow(DBCollection ptDbCollection, Advice advice) {
        Class<?>[] classes = advice.getBehavior().getParameterTypes();
        Object[] args = advice.getParameterArray();
        if (classes[1] == AggregationOptions.class) {
            return ptDbCollection.aggregate((List)args[0], (AggregationOptions)args[1]);
        } else if (classes[1] == ReadPreference.class) {
            return ptDbCollection.aggregate((List)args[0], (ReadPreference)args[1]);
        }
        throw new PressureMeasureError("missing method interceptor!");
    }

}
