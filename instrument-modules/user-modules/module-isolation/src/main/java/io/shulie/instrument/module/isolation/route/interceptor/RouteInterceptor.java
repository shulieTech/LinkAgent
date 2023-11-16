package io.shulie.instrument.module.isolation.route.interceptor;

import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.gson.GsonFactory;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import io.shulie.instrument.module.isolation.common.ShadowResourceLifecycleModule;
import io.shulie.instrument.module.isolation.common.ShadowTargetCache;
import io.shulie.instrument.module.isolation.proxy.ShadowProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @author Licey
 * @date 2022/7/26
 */
public class RouteInterceptor extends CutoffInterceptorAdaptor {
    private static final Logger logger = LoggerFactory.getLogger(RouteInterceptor.class);

    private final ShadowProxy shadowProxy;

    public RouteInterceptor(ShadowProxy shadowProxy) {
        this.shadowProxy = shadowProxy;
    }

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
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
                logger.error("execute shadow proxy fail: {}", GsonFactory.getGson().toJson(this.shadowProxy), t);
                throw new PressureMeasureError("execute shadow proxy fail", t);
            }
        }
        //处理资源关闭方法，关闭可能是任务触发，无压测标
        if(isCloseAction(advice) && shadowProxy.needRoute(advice.getTarget())){
            //业务和代理均需执行
            Reflect.on(advice.getTarget()).call(advice.getBehaviorName(),advice.getParameterArray());
            ShadowResourceLifecycleModule lifecycleModule = ShadowTargetCache.remove(advice.getTarget());
            if(lifecycleModule  == null){
                return CutOffResult.passed();
            }
            lifecycleModule.getShadowResourceLifecycle().destroy(60L);
        }
        return CutOffResult.passed();
    }


    /**
     * 方法是否为关闭资源方法
     * @param advice
     * @return
     */
    private boolean isCloseAction(Advice advice){
        String behaviorName = advice.getBehaviorName();
        if("close".equals(behaviorName)){
            return true;
        }
        return false;
    }




}
