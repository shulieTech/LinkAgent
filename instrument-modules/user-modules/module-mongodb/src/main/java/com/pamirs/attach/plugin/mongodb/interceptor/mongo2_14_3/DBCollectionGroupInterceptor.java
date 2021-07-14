package com.pamirs.attach.plugin.mongodb.interceptor.mongo2_14_3;

import java.lang.reflect.Method;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.GroupCommand;
import com.mongodb.ReadPreference;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/20 4:18 下午
 */
public class DBCollectionGroupInterceptor extends AbstractDBCollectionInterceptor {

    private Method method = null;

    @Override
    protected boolean check(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null) {
            return false;
        }
        return args.length == 1 || args.length == 2 || args.length == 5 || args.length == 6;
    }

    @Override
    protected boolean isRead() {
        return true;
    }

    @Override
    protected Object cutoffShadow(DBCollection ptDbCollection, Advice advice) throws Exception {
        Object[] args = advice.getParameterArray();
        if (args.length == 1) {
            return getMethod(ptDbCollection).invoke(ptDbCollection, args[0]);
        }
        if (args.length == 2) {
            return ptDbCollection.group((GroupCommand)args[0], (ReadPreference)args[1]);
        }
        if (args.length == 5) {
            return ptDbCollection.group((DBObject)args[0], (DBObject)args[1], (DBObject)args[2], (String)args[3],
                (String)args[4]);
        }
        if (args.length == 6) {
            return ptDbCollection.group((DBObject)args[0], (DBObject)args[1], (DBObject)args[2], (String)args[3],
                (String)args[4], (ReadPreference)args[5]);
        }
        throw new RuntimeException("this should never happened!");
    }

    private Method getMethod(DBCollection dbCollection) {
        if (method == null) {
            Method[] methods = dbCollection.getClass().getSuperclass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("group") && method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0] == DBObject.class) {
                    this.method = method;
                    this.method.setAccessible(true);
                }
            }
        }
        if (method == null) {
            throw new PressureMeasureError("未支持的版本！");
        }
        return method;
    }
}
