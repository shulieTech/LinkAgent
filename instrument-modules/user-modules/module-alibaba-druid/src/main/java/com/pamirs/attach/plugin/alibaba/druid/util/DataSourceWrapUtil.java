/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.alibaba.druid.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.pamirs.attach.plugin.alibaba.druid.obj.DbDruidMediatorDataSource;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.DbMediatorDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create by xuyh at 2020/3/25 16:48.
 */
public class DataSourceWrapUtil {
    private final static Logger LOGGER = LoggerFactory.getLogger(DataSourceWrapUtil.class.getName());
    private static Set<Object> pressureDatasourceSet = new HashSet<Object>();

    public static final ConcurrentHashMap<DataSourceMeta, DbDruidMediatorDataSource> pressureDataSources = new ConcurrentHashMap<DataSourceMeta, DbDruidMediatorDataSource>();

    public static void destroy() {
        Iterator<Map.Entry<DataSourceMeta, DbDruidMediatorDataSource>> it = pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, DbDruidMediatorDataSource> entry = it.next();
            it.remove();
            entry.getValue().close();
        }
        pressureDataSources.clear();
        pressureDatasourceSet.clear();
    }

    public static DbDruidMediatorDataSource doWrap(DataSourceMeta<DruidDataSource> dataSourceMeta) {
        DbDruidMediatorDataSource cacheValue = pressureDataSources.get(dataSourceMeta);
        if (cacheValue != null) {
            return cacheValue;
        }
        DruidDataSource target = dataSourceMeta.getDataSource();
        if (isPerformanceDataSource(target)) {
            LOGGER.warn("[druid] current datasource is performance datasource. ignore it. url={}, username={}", target.getUrl(), target.getUsername());
            return null;
        }
        boolean infoEnabled = LOGGER.isInfoEnabled();
        if (!DruidDatasourceUtils.configured(target)) {//没有配置对应的影子表或影子库
            LOGGER.error("[druid] No configuration found for datasource, url:{} username:{}", target.getUrl(), target.getUsername());
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0002")
                    .setMessage("没有配置对应的影子表或影子库！")
                    .setDetail("业务库配置:::url: " + target.getUrl() + " ;username: " + target.getUsername())
                    .report();
            /**
             * 如果未配置,则返回包装的数据源,类似于影子表
             */
            DbDruidMediatorDataSource dbMediatorDataSource = new DbDruidMediatorDataSource();
            dbMediatorDataSource.setDataSourceBusiness(target);
            DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
            if (old != null) {
                if (infoEnabled) {
                    LOGGER.info("[druid] destroyed shadow table datasource success. url:{} ,username:{}", target.getUrl(), target.getUsername());
                }
                old.close();
            }
            return dbMediatorDataSource;
        }
        if (DruidDatasourceUtils.shadowTable(target)) {//影子表
            DbDruidMediatorDataSource dbMediatorDataSource = new DbDruidMediatorDataSource();
            dbMediatorDataSource.setDataSourceBusiness(target);
            DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
            if (old != null) {
                if (infoEnabled) {
                    LOGGER.info("[druid] destroyed shadow table datasource success. url:{} ,username:{}", target.getUrl(), target.getUsername());
                }
                old.close();
            }
            return dbMediatorDataSource;
        } else {//影子库
            // 初始化影子数据源配置
            DbDruidMediatorDataSource dbMediatorDataSource = new DbDruidMediatorDataSource();
            dbMediatorDataSource.setDataSourceBusiness(target);
            if (infoEnabled) {
                LOGGER.info("[druid] use db shadow config:{}", GlobalConfig.getInstance().getShadowDatasourceConfigs());
            }
            DruidDataSource ptDataSource = DruidDatasourceUtils.generateDatasourceFromConfiguration(target, GlobalConfig.getInstance().getShadowDatasourceConfigs());
            if (ptDataSource == null) {
                LOGGER.error("[druid] create shadow datasource error. maybe datasource config is not correct, url: {} username:{} configurations:{}", target.getUrl(), target.getUsername(), GlobalConfig.getInstance().getShadowDatasourceConfigs());
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0003")
                        .setMessage("影子库配置异常，无法由配置正确生成影子库！")
                        .setDetail("url: " + target.getUrl() + " username: " + target.getUsername())
                        .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                        .report();
                return null;
            }
            dbMediatorDataSource.setDataSourcePerformanceTest(ptDataSource);
            DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
            if (old != null) {
                if (infoEnabled) {
                    LOGGER.info("[druid] destroyed shadow table datasource success. url:{} ,username:{}", target.getUrl(), target.getUsername());
                }
                old.close();
            }
            if (infoEnabled) {
                LOGGER.info("[druid] create shadow datasource success. target:{} url:{} ,username:{} shadow-url:{},shadow-username:{}", target.hashCode(), target.getUrl(), target.getUsername(), ptDataSource.getUrl(), ptDataSource.getUsername());
            }
            pressureDatasourceSet.add(ptDataSource);
            return dbMediatorDataSource;
        }
    }

    /**
     * 是否是影子数据源
     *
     * @param target 目标数据源
     * @return
     */
    private static boolean isPerformanceDataSource(DruidDataSource target) {
        for (Map.Entry<DataSourceMeta, DbDruidMediatorDataSource> entry : pressureDataSources.entrySet()) {
            DbDruidMediatorDataSource mediatorDataSource = entry.getValue();
            if (mediatorDataSource.getDataSourcePerformanceTest() == null) {
                continue;
            }
            if (StringUtils.equals(mediatorDataSource.getDataSourcePerformanceTest().getUrl(), target.getUrl())
                    && StringUtils.equals(mediatorDataSource.getDataSourcePerformanceTest().getUsername(), target.getUsername())
                && pressureDatasourceSet.contains(target)) {
                return true;
            }
        }
        return false;
    }
}
