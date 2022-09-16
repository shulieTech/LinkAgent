package com.pamirs.attach.plugin.alibaba.druid.listener;

import com.alibaba.druid.pool.DruidDataSource;
import com.pamirs.attach.plugin.alibaba.druid.obj.DbDruidMediatorDataSource;
import com.pamirs.attach.plugin.alibaba.druid.util.DataSourceWrapUtil;
import com.pamirs.attach.plugin.alibaba.druid.util.DruidDatasourceUtils;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceActiveEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Iterator;
import java.util.Map;

public class DruidShadowActiveEventListener implements PradarEventListener {

    private static Logger LOGGER = LoggerFactory.getLogger(DruidShadowDisableEventListener.class.getName());

    @Override
    public EventResult onEvent(IEvent event) {
        if (!(event instanceof ShadowDataSourceActiveEvent)) {
            return EventResult.IGNORE;
        }
        Map.Entry<ShadowDatabaseConfig, DataSource> entry = ((ShadowDataSourceActiveEvent) event).getTarget();
        if (entry == null) {
            return EventResult.IGNORE;
        }

        DataSource source = entry.getValue();
        Thread.currentThread().setContextClassLoader(source.getClass().getClassLoader());

        if (!(source instanceof DruidDataSource)) {
            return EventResult.IGNORE;
        }
        DruidDataSource druidDataSource = (DruidDataSource) source;

        ShadowDatabaseConfig config = entry.getKey();
        int dsType = config.getDsType();

        DbDruidMediatorDataSource media = null;
        // 找到对应的数据源对
        Iterator<Map.Entry<DataSourceMeta, DbDruidMediatorDataSource>> it = DataSourceWrapUtil.pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, DbDruidMediatorDataSource> entry1 = it.next();
            DruidDataSource dataSource = entry1.getValue().getDataSourceBusiness();
            if (dataSource.equals(druidDataSource)) {
                media = entry1.getValue();
                break;
            }
        }
        try {
            // 没有找到对应的数据源对
            if (media == null) {
                buildShadowDataSource(dsType, druidDataSource, config);
                return EventResult.success("module-alibaba-druid: handler shadow datasource active event success.");
            }
            // 找到了成对的数据源
            refreshShadowDataSource(dsType, druidDataSource, config, media);
            return EventResult.success("module-alibaba-druid: handler shadow datasource active event success.");
        } catch (Exception e) {
            LOGGER.error("module-alibaba-druid: handler shadow datasource active event occur exception", e);
            return EventResult.error("active-shadow-datasource-event", "module-alibaba-druid: handler shadow datasource active event occur exception.");
        }
    }

    private void buildShadowDataSource(int dsType, DruidDataSource dataSource, ShadowDatabaseConfig config) {
        DataSourceMeta<DruidDataSource> dataSourceMeta = new DataSourceMeta<DruidDataSource>(dataSource.getUrl(), dataSource.getUsername(), dataSource);
        // 影子表
        if (dsType == 1) {
            DbDruidMediatorDataSource dbMediatorDataSource = new DbDruidMediatorDataSource();
            dbMediatorDataSource.setDataSourceBusiness(dataSource);
            DbDruidMediatorDataSource old = DataSourceWrapUtil.pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
            if (old != null) {
                LOGGER.info("[druid] destroyed shadow table datasource success. url:{} ,username:{}", dataSource.getUrl(), dataSource.getUsername());
                old.close();
            }
            return;
        }
        // 影子库 影子库/表
        DruidDataSource ptDataSource = DruidDatasourceUtils.generateDatasourceFromConfiguration(dataSource, config);
        if (ptDataSource == null) {
            LOGGER.error("[druid] handler shadow datasource active event failed, create shadow datasource error. maybe datasource config is not correct, url: {} username:{} configuration:{}", dataSource.getUrl(), dataSource.getUsername(), config);
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0003")
                    .setMessage("影子库配置异常，无法由配置正确生成影子库！")
                    .setDetail("url: " + dataSource.getUrl() + " username: " + dataSource.getUsername())
                    .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                    .report();
            return;
        }
        DbDruidMediatorDataSource dbMediatorDataSource = new DbDruidMediatorDataSource();
        dbMediatorDataSource.setDataSourceBusiness(dataSource);
        dbMediatorDataSource.setDataSourcePerformanceTest(ptDataSource);
        DbDruidMediatorDataSource old = DataSourceWrapUtil.pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
        if (old != null) {
            LOGGER.info("[druid] destroyed shadow table datasource success. url:{} ,username:{}", dataSource.getUrl(), dataSource.getUsername());
            old.close();
        }
    }

    /**
     * 重新构建影子数据源
     *
     * @param dsType
     * @param dataSource
     * @param config
     */
    private void refreshShadowDataSource(int dsType, DruidDataSource dataSource, ShadowDatabaseConfig config, DbDruidMediatorDataSource media) {
        DruidDataSource ptDataSource = media.getDataSourcePerformanceTest();
        // 影子表模式不修改
        if (dsType == 1) {
            if (ptDataSource != null) {
                media.close();
                media.setDataSourcePerformanceTest(null);
                LOGGER.info("[druid] biz datasource with url:{}, username:{} change to shadow table type, close shadow datasource!", dataSource.getUrl(), dataSource.getUsername());
            }
            return;
        }
        if (ptDataSource != null) {
            String ptKey = buildDataSourceKey(ptDataSource);
            String configKey = DbUrlUtils.getKey(config.getShadowUrl(), config.getShadowUsername());
            // 影子数据源没修改
            if (ptKey.equals(configKey)) {
                return;
            }
            media.close();
        }
        ptDataSource = DruidDatasourceUtils.generateDatasourceFromConfiguration(dataSource, config);
        LOGGER.info("[druid] handler shadow datasource active event, refresh shadow datasource, url:{}, username:{}", dataSource.getUrl(), dataSource.getUsername());
        media.setDataSourcePerformanceTest(ptDataSource);
    }

    @Override
    public int order() {
        return 14;
    }

    private String buildDataSourceKey(DruidDataSource dataSource) {
        return DbUrlUtils.getKey(dataSource.getUrl(), dataSource.getUsername());
    }
}
