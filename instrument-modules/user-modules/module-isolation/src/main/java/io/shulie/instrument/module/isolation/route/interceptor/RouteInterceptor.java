package io.shulie.instrument.module.isolation.route.interceptor;

import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.listener.ext.AdviceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Licey
 * @date 2022/7/26
 */
public class RouteInterceptor extends AdviceListener {
    private static final Logger logger = LoggerFactory.getLogger(RouteInterceptor.class);


    @Override
    public void before(Advice advice) throws Throwable {
        //todo@langyi
        super.before(advice);
    }

    @Override
    public void after(Advice advice) throws Throwable {
        //todo@langyi
        super.after(advice);
    }

    @Override
    public void afterReturning(Advice advice) throws Throwable {
        //todo@langyi
        super.afterReturning(advice);
    }

    @Override
    public void afterThrowing(Advice advice) throws Throwable {
        //todo@langyi
        super.afterThrowing(advice);
    }

}
