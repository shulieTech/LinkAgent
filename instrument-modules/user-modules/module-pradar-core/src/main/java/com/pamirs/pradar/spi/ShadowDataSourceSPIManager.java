package com.pamirs.pradar.spi;


import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
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

    private static final String splitter = "##";

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
            if (config.getProperties() != null && config.getProperties().containsKey(ShadowDataSourceServiceProvider.spi_key)) {
                continue;
            }
            result = result && refreshShadowDatabaseConfig(config);
        }
        return result;
    }

    public static boolean refreshShadowDatabaseConfig(ShadowDatabaseConfig config) {
        String userName = config.getShadowUsername();
        String pwd = config.getShadowPassword();
        String providerName = null;
        if (userName.startsWith("${")) {
            Map.Entry<String, String> userValue = extractConfigValue(userName);
            if (userValue == null) {
                return false;
            }
            providerName = userValue.getKey();
            config.setShadowUsername(userValue.getValue());

        }
        if (pwd.startsWith("${")) {
            Map.Entry<String, String> pwdValue = extractConfigValue(pwd);
            if (pwdValue == null) {
                return false;
            }
            if (providerName != null && !providerName.equals(pwdValue.getKey())) {
                logger.warn("shadow data source config processed by spi not with same plugin {}, {}", providerName, pwdValue.getKey());
                return false;
            }
            config.setShadowPassword(pwdValue.getValue());
        }

        if (providerName == null || !serviceProviders.containsKey(providerName)) {
            logger.warn("not ShadowDatabaseConfigServiceProvider plugin named with {}", providerName);
            return false;
        }
        boolean result = serviceProviders.get(providerName).processShadowDatabaseConfig(config);
        if (result) {
            // 成功的话添加标记
            Map<String, String> properties = config.getProperties();
            if (properties == null) {
                properties = new HashMap<String, String>();
                config.setProperties(properties);
            }
            properties.put(ShadowDataSourceServiceProvider.spi_key, providerName);
        }
        return result;
    }

    private static Map.Entry<String, String> extractConfigValue(String config) {
        String innerConfig = config.substring(2, config.length() - 1);
        if (!innerConfig.contains(splitter)) {
            logger.warn("shadow data source config value {} is not formatted", config);
            return null;
        }
        String[] split = innerConfig.split(splitter, 2);
        return new AbstractMap.SimpleEntry<String, String>(split[0], "${" + split[1] + "}");
    }


}
