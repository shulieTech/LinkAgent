/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.mongodb.interceptor.mongo2_14_3;

import java.util.List;

import com.mongodb.DBCollection;
import com.mongodb.DBEncoder;
import com.mongodb.DBObject;
import com.mongodb.InsertOptions;
import com.mongodb.WriteConcern;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/20 4:18 下午
 */
public class DBCollectionUpdateInterceptor extends AbstractDBCollectionInterceptor {

    @Override
    protected boolean check(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null) {
            return false;
        }
        return args.length == 6 || args.length == 7;
    }

    @Override
    protected boolean isRead() {
        return false;
    }

    @Override
    protected Object cutoffShadow(DBCollection ptDbCollection, Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args.length == 6) {
            return ptDbCollection.update((DBObject)args[0], (DBObject)args[1], (Boolean)args[2], (Boolean)args[3],
                (WriteConcern)args[4], (DBEncoder)args[5]);
        }
        if (args.length == 7) {
            return ptDbCollection.update((DBObject)args[0], (DBObject)args[1], (Boolean)args[2], (Boolean)args[3],
                (WriteConcern)args[4], (Boolean)args[5], (DBEncoder)args[6]);
        }
        throw new RuntimeException("this should never happened!");
    }

}
