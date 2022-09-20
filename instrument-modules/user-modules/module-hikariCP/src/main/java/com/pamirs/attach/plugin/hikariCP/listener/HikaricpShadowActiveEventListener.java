package com.pamirs.attach.plugin.hikariCP.listener;

import com.pamirs.attach.plugin.hikariCP.utils.DataSourceWrapUtil;
import com.pamirs.attach.plugin.hikariCP.utils.HikariMediaDataSource;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceActiveEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.datasource.SqlParser;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Iterator;
import java.util.Map;

public class HikaricpShadowActiveEventListener implements PradarEventListener {

    private static Logger LOGGER = LoggerFactory.getLogger(HikaricpShadowDisableEventListener.class.getName());

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
        if (!(source.getClass().getName().equals("com.zaxxer.hikari.HikariDataSource"))) {
            return EventResult.IGNORE;
        }
        Thread.currentThread().setContextClassLoader(source.getClass().getClassLoader());
        HikariDataSource druidDataSource = (HikariDataSource) source;

        ShadowDatabaseConfig config = entry.getKey();
        int dsType = config.getDsType();

        HikariMediaDataSource media = null;
        // 找到对应的数据源对
        Iterator<Map.Entry<DataSourceMeta, HikariMediaDataSource>> it = DataSourceWrapUtil.pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, HikariMediaDataSource> entry1 = it.next();
            HikariDataSource dataSource = entry1.getValue().getDataSourceBusiness();
            if (dataSource.equals(druidDataSource)) {
                media = entry1.getValue();
                break;
            }
        }
        SqlParser.clear();
        try {
            // 没有找到对应的数据源对
            if (media == null) {
                buildShadowDataSource(dsType, druidDataSource, config);
                return EventResult.success("[module-hikariCP]: handler shadow datasource active event success.");
            }
            // 找到了成对的数据源
            refreshShadowDataSource(dsType, druidDataSource, config, media);
            return EventResult.success("[module-hikariCP]: handler shadow datasource active event success.");
        } catch (Exception e) {
            LOGGER.error("[module-hikariCP]: handler shadow datasource active event occur exception", e);
            return EventResult.error("active-shadow-hikaricp-event", "[module-hikariCP]: handler shadow datasource active event occur exception");
        }
    }

    private void buildShadowDataSource(int dsType, HikariDataSource dataSource, ShadowDatabaseConfig config) {
        DataSourceMeta<HikariDataSource> dataSourceMeta = new DataSourceMeta<HikariDataSource>(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource);
        // 影子表
        if (dsType == 1) {
            HikariMediaDataSource dbMediatorDataSource = new HikariMediaDataSource();
            dbMediatorDataSource.setDataSourceBusiness(dataSource);
            HikariMediaDataSource old = DataSourceWrapUtil.pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
            if (old != null) {
                LOGGER.info("[module-hikariCP] destroyed shadow table datasource success. url:{} ,username:{}", dataSource.getJdbcUrl(), dataSource.getUsername());
                old.close();
            }
            return;
        }
        // 影子库 影子库/表
        HikariDataSource ptDataSource = DataSourceWrapUtil.generate(dataSource, config);
        if (ptDataSource == null) {
            LOGGER.error("[module-hikariCP] handler shadow datasource active event failed, create shadow datasource error. maybe datasource config is not correct, url: {} username:{} configuration:{}", dataSource.getJdbcUrl(), dataSource.getUsername(), config);
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0003")
                    .setMessage("影子库配置异常，无法由配置正确生成影子库！")
                    .setDetail("url: " + dataSource.getJdbcUrl() + " username: " + dataSource.getUsername())
                    .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                    .report();
            return;
        }
        HikariMediaDataSource dbMediatorDataSource = new HikariMediaDataSource();
        dbMediatorDataSource.setDataSourceBusiness(dataSource);
        dbMediatorDataSource.setDataSourcePerformanceTest(ptDataSource);
        HikariMediaDataSource old = DataSourceWrapUtil.pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
        if (old != null) {
            LOGGER.info("[module-hikariCP] destroyed shadow table datasource success. url:{} ,username:{}", dataSource.getJdbcUrl(), dataSource.getUsername());
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
    private void refreshShadowDataSource(int dsType, HikariDataSource dataSource, ShadowDatabaseConfig config, HikariMediaDataSource media) {
        HikariDataSource ptDataSource = media.getDataSourcePerformanceTest();
        // 影子表模式不修改
        if (dsType == 1) {
            media.resetIniStated();
            if (ptDataSource != null) {
                media.close();
                media.setDataSourcePerformanceTest(null);
                LOGGER.info("[module-hikariCP] biz datasource with url:{}, username:{} change to shadow table type, close shadow datasource!", dataSource.getJdbcUrl(), dataSource.getUsername());
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

        ptDataSource = DataSourceWrapUtil.generate(dataSource, config);
        LOGGER.info("[module-hikariCP] handler shadow datasource active event, refresh shadow datasource, url:{}, username:{}", dataSource.getJdbcUrl(), dataSource.getUsername());
        media.setDataSourcePerformanceTest(ptDataSource);
        media.resetIniStated();
    }

    @Override
    public int order() {
        return 15;
    }

    private String buildDataSourceKey(HikariDataSource dataSource) {
        return DbUrlUtils.getKey(dataSource.getJdbcUrl(), dataSource.getUsername());
    }
}
