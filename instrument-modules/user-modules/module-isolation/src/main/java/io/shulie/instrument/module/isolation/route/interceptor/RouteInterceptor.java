package io.shulie.instrument.module.isolation.route.interceptor;

import com.alibaba.fastjson.JSON;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import io.shulie.instrument.module.isolation.proxy.ShadowProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Licey
 * @date 2022/7/26
 */
public class RouteInterceptor extends CutoffInterceptorAdaptor {
    private static final Logger logger = LoggerFactory.getLogger(RouteInterceptor.class);

    private Map<String, Method> methodMap = new ConcurrentHashMap<String, Method>();

    private ShadowProxy shadowProxy;

    public RouteInterceptor(ShadowProxy shadowProxy) {
        this.shadowProxy = shadowProxy;
    }

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        //todo@langyi 如果进入的是影子对象，需要检查
        if (Pradar.isClusterTest() && shadowProxy.needRoute(advice.getTarget())) {
            try {
                Object[] parameterArray = advice.getParameterArray();
                Object res = shadowProxy.executeMethod(
                        advice.getTarget()
                        , advice.getBehaviorName()
                        , advice.getBehaviorNameDesc()
                        , parameterArray == null ? null : Arrays.copyOf(parameterArray, parameterArray.length));
                return CutOffResult.cutoff(res);
            } catch (Throwable t) {
                logger.error("execute shadow proxy fail: {}", JSON.toJSONString(this.shadowProxy), t);
                throw new PressureMeasureError("execute shadow proxy fail", t);
            }
        }
        return CutOffResult.passed();
    }
}
