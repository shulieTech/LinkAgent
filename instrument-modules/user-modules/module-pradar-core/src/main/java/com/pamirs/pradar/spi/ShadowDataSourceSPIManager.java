package com.pamirs.pradar.spi;


import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
        if (serviceProviders.containsKey(name)) {
            return true;
        }
        logger.info("add shadow datasource service provider :{}", provider.getClass().getName());
        serviceProviders.put(name, provider);
        return true;
    }

    public static Map<String, ShadowDatabaseConfig> refreshAllShadowDatabaseConfigs(Map<String, ShadowDatabaseConfig> datasourceConfigs) {

        Map<String, ShadowDatabaseConfig> newConfig = new HashMap<String, ShadowDatabaseConfig>();

        for (Map.Entry<String, ShadowDatabaseConfig> entry : datasourceConfigs.entrySet()) {
            ShadowDatabaseConfig config = entry.getValue();
            if (!config.getShadowUsername().startsWith("$") && !config.getShadowPassword().startsWith("$")) {
                newConfig.put(entry.getKey(), entry.getValue());
                continue;
            }
            logger.info("start process shadow datasource config :{}", entry.getKey());
            boolean result = refreshShadowDatabaseConfig(config);
            if (result) {
                String key = DbUrlUtils.getKey(config.getUrl(), config.getUsername());
                logger.info("success process shadow datasource config, url:{}, shadow userName{}, shadow password length :{}",
                        config.getShadowUrl(), config.getShadowUsername(), config.getShadowPassword().length());
                newConfig.put(key, config);
            }else{
                logger.error("failed process shadow datasource config, shadow userName:{}", config.getShadowUsername());
            }
        }

        return newConfig;

    }

    public static boolean refreshShadowDatabaseConfig(ShadowDatabaseConfig config) {
        String userName = config.getShadowUsername();
        String pwd = config.getShadowPassword();
        Map.Entry<String, String> userValue = extractConfigValue(userName);

        String providerName = userValue.getKey();
        config.setShadowUsername(userValue.getValue());

        Map.Entry<String, String> pwdValue = extractConfigValue(pwd);

        if (providerName != null && !providerName.equals(pwdValue.getKey())) {
            logger.warn("shadow data source config processed by spi not with same plugin {}, {}", providerName, pwdValue.getKey());
            return false;
        }
        config.setShadowPassword(pwdValue.getValue());

        if (providerName == null || !serviceProviders.containsKey(providerName)) {
            logger.warn("not ShadowDatabaseConfigServiceProvider plugin named with {}", providerName);
            return false;
        }

        logger.info("process shadow datasource with spi, username:{}, password:{}, providerName:{}",
                config.getShadowUsername(), config.getShadowPassword(), providerName);

        boolean result = serviceProviders.get(providerName).processShadowDatabaseConfig(config);
        if (result) {
            // 成功的话添加标记
            Map<String, String> properties = config.getProperties();
            if (properties == null) {
                properties = new HashMap<String, String>();
                config.setProperties(properties);
            }
            properties.put(ShadowDataSourceServiceProvider.spi_key, providerName);
        } else {
            logger.error("failed process shadow datasource by service provider {}, shadow url:{}, shadow userName:{}, shadow password:{}",
                    providerName, config.getShadowUrl(), config.getShadowUsername(), config.getShadowPassword());
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
