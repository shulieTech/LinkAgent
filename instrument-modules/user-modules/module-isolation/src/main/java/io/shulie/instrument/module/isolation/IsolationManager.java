package io.shulie.instrument.module.isolation;

import com.pamirs.pradar.interceptor.Interceptors;
import com.shulie.instrument.simulator.api.listener.Listeners;
import com.shulie.instrument.simulator.api.listener.ext.EventWatchBuilder;
import com.shulie.instrument.simulator.api.listener.ext.EventWatcher;
import com.shulie.instrument.simulator.api.listener.ext.IBehaviorMatchBuilder;
import com.shulie.instrument.simulator.api.listener.ext.IClassMatchBuilder;
import com.shulie.instrument.simulator.api.resource.ModuleEventWatcher;
import com.shulie.instrument.simulator.api.scope.ExecutionPolicy;
import io.shulie.instrument.module.isolation.common.ResourceInit;
import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.enhance.EnhanceMethod;
import io.shulie.instrument.module.isolation.exception.IsolationRuntimeException;
import io.shulie.instrument.module.isolation.proxy.ShadowProxy;
import io.shulie.instrument.module.isolation.proxy.impl.RouteShadowMethodProxy;
import io.shulie.instrument.module.isolation.register.ShadowProxyConfig;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;
import io.shulie.instrument.module.isolation.route.interceptor.RouteInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Licey
 * @date 2022/8/1
 */
public class IsolationManager {
    private static final Logger logger = LoggerFactory.getLogger(IsolationManager.class);
    private static ModuleEventWatcher moduleEventWatcher;

    private static final Map<String, List<EventWatcher>> moduleWatcherMap = new ConcurrentHashMap<String, List<EventWatcher>>();

    public static void init(ModuleEventWatcher moduleEventWatcher){
        IsolationManager.moduleEventWatcher = moduleEventWatcher;
    }


    public static void register(ShadowProxyConfig proxyConfig) {
        String moduleName = proxyConfig.getModuleName();

        List<EventWatcher> watcherList = moduleWatcherMap.get(moduleName);
        if (watcherList == null) {
            watcherList = new ArrayList<EventWatcher>();
            moduleWatcherMap.put(moduleName, watcherList);
        }
        //todo@langyi 只实现了对指定方法的增强和实现， 对类中其他方法的增强和检查，暂未实现。  这里还没获取到业务类加载器，无法提前检查/增强非指定方法
        for (EnhanceClass enhanceClass : proxyConfig.getEnhanceClassList()) {
            ResourceInit<ShadowResourceProxyFactory> factoryResourceInit = enhanceClass.getFactoryResourceInit();
            if (factoryResourceInit == null) {
                throw new IsolationRuntimeException("class " + enhanceClass.getClassName() + " proxyFactoryInit is null!");
            }
            watcherList.add(enhanceClassMethod(proxyConfig.getModuleName(), enhanceClass));
        }
    }

    public static EventWatcher enhanceClassMethod(String module, EnhanceClass enhanceClass) {
        logger.info("[isolation]pre enhance class:{}", enhanceClass.getClassName());

        IClassMatchBuilder buildingForClass = new EventWatchBuilder(moduleEventWatcher).onClass(enhanceClass.getClassName());

        for (EnhanceMethod enhanceMethod : enhanceClass.getMethodList()) {
            if (enhanceMethod.getMethodProxyInit() == null) {
                throw new IsolationRuntimeException("class " + enhanceClass.getClassName() + ",method:" + enhanceMethod.getMethod() + " methodProxyInit is null!");
            }
            IBehaviorMatchBuilder buildingForBehavior = buildingForClass.onAnyBehavior(enhanceMethod.getMethod());
            if (enhanceMethod.getArgTypes() != null && enhanceMethod.getArgTypes().length > 0) {
                buildingForBehavior.withParameterTypes(enhanceMethod.getArgTypes());
            }
            buildingForBehavior.onListener(Listeners.of(RouteInterceptor.class, keyOfScope(enhanceClass,enhanceMethod), ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK, new Object[]{new ShadowProxy(module, enhanceClass, enhanceMethod)}));
        }
        return buildingForClass.onWatch();
    }

    private static String keyOfScope(EnhanceClass enhanceClass, EnhanceMethod enhanceMethod) {
        StringBuilder args = new StringBuilder();
        //去掉参数， 这里不需要参数，如果是会重复调用的，本来就应该只拦截一次就够了
//        args.append("(");
//        if (enhanceMethod.getArgTypes() != null) {
//            for (String argType : enhanceMethod.getArgTypes()) {
//                if (args.length() != 1) {
//                    args.append(",");
//                }
//                args.append(argType);
//            }
//        }
//        args.append(")");
        return enhanceClass.getClassName() + "#" + enhanceMethod.getMethod() + args.toString() + "-routeInterceptor";
    }

}
