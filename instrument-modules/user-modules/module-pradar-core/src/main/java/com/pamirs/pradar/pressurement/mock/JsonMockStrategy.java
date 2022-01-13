package com.pamirs.pradar.pressurement.mock;

import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.ExecutionCall;
import com.shulie.instrument.simulator.api.ProcessControlException;


/**
 * @Auther: vernon
 * @Date: 2022/1/13 14:20
 * @Description:
 */
public abstract class JsonMockStrategy implements ExecutionStrategy {

    public abstract String name();

    Router router = new JsonMockRouter();

    @Override
    public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {

        JsonMockStrategy strategy = (JsonMockStrategy) router.route(name());
        if (strategy == null) {
            throw new PressureMeasureError("not support mock type.");
        }
        strategy.processBlock(returnType, classLoader, params);
        return null;
    }

    @Override
    public Object processNonBlock(Class returnType, ClassLoader classLoader, Object params, ExecutionCall call) {
        throw new PressureMeasureError("not support mock type.");
    }

    @Override
    public Object processBlock(Class returnType, ClassLoader classLoader, Object params, ExecutionCall call) {
        throw new PressureMeasureError("not support mock type.");
    }

}
