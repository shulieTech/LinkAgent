package com.pamirs.pradar.spi;


import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author jiangjibo
 * @date 2021/12/1 11:15 上午
 * @description:
 */
public class ShadowDataSourceSPIManager {

    private static Logger logger = LoggerFactory.getLogger(ShadowDataSourceSPIManager.class);

    private static Map<String, ShadowDataSourceServiceProvider> serviceProviders;

    static {
        serviceProviders = new HashMap<String, ShadowDataSourceServiceProvider>();
        for (ShadowDataSourceServiceProvider provider : ServiceLoader.load(ShadowDataSourceServiceProvider.class)) {
            if (!provider.getClass().isAnnotationPresent(ShadowDataSourceProvider.class)) {
                logger.warn("service provider {} not annotated by annotation {}", provider.getClass().getName(), ShadowDataSourceProvider.class.getName());
                continue;
            }
            serviceProviders.put(provider.getClass().getAnnotation(ShadowDataSourceProvider.class).value(), provider);
        }
    }

    public static boolean addServiceProvider(ShadowDataSourceServiceProvider provider) {
        if (!provider.getClass().isAnnotationPresent(ShadowDataSourceProvider.class)) {
            logger.warn("service provider {} not annotated by annotation {}", provider.getClass().getName(), ShadowDataSourceProvider.class.getName());
            return false;
        }
        String name = provider.getClass().getAnnotation(ShadowDataSourceProvider.class).value();
        if (!serviceProviders.containsKey(name)) {
            serviceProviders.put(name, provider);
        }
        return true;
    }

    public static boolean refreshAllShadowDatabaseConfigs() {
        boolean result = true;
        for (ShadowDatabaseConfig config : GlobalConfig.getInstance().getShadowDatasourceConfigs().values()) {
            result = result && refreshShadowDatabaseConfig(config);
        }
        return result;
    }

    public static boolean refreshShadowDatabaseConfig(ShadowDatabaseConfig config) {
        String providerName = config.getProperty(ShadowDataSourceServiceProvider.spi_key);
        if (providerName == null || !serviceProviders.containsKey(providerName)) {
            logger.warn("not ShadowDatabaseConfigServiceProvider plugin named with {}", providerName);
            return false;
        }
        boolean result = serviceProviders.get(providerName).processShadowDatabaseConfig(config);
        if (result) {
            // 成功的话移除标记
            config.getProperties().remove(ShadowDataSourceServiceProvider.spi_key);
        }
        return result;
    }


}
