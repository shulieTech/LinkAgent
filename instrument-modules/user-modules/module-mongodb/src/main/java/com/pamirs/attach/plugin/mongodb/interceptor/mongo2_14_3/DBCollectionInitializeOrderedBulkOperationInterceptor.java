package com.pamirs.attach.plugin.mongodb.interceptor.mongo2_14_3;

import com.mongodb.DBCollection;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/20 4:18 下午
 */
public class DBCollectionInitializeOrderedBulkOperationInterceptor extends AbstractDBCollectionInterceptor {

    @Override
    protected boolean check(Advice advice) {
        return advice.getBehavior().getName().equals("initializeOrderedBulkOperation");
    }

    @Override
    protected boolean isRead() {
        return false;
    }

    @Override
    protected Object cutoffShadow(DBCollection ptDbCollection, Advice advice) {
        return ptDbCollection.initializeOrderedBulkOperation();
    }

}
