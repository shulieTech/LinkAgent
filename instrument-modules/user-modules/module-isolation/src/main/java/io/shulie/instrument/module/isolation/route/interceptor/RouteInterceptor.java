package io.shulie.instrument.module.isolation.route.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.listener.ext.AdviceListener;
import io.shulie.instrument.module.isolation.proxy.ShadowProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Licey
 * @date 2022/7/26
 */
public class RouteInterceptor extends AdviceListener {
    private static final Logger logger = LoggerFactory.getLogger(RouteInterceptor.class);

    private ShadowProxy shadowProxy;

    public RouteInterceptor(ShadowProxy shadowProxy) {
        this.shadowProxy = shadowProxy;
    }

    @Override
    public void before(Advice advice) throws Throwable {
        if (Pradar.isClusterTest()) {
            try {
                Object res = shadowProxy.executeMethod(advice.getTarget(), advice.getBehaviorName(), advice.getParameterArray());
                advice.returnImmediately(res);
            } catch (Throwable e) {
                throw new PressureMeasureError("execute shadow proxy fail", e);
            }
        }
    }

    @Override
    public void after(Advice advice) throws Throwable {
    }

    @Override
    public void afterReturning(Advice advice) throws Throwable {
    }

    @Override
    public void afterThrowing(Advice advice) throws Throwable {
    }

}
