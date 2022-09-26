package com.pamirs.attach.plugin.dbcp2.listener;

import com.pamirs.attach.plugin.dbcp2.utils.DataSourceWrapUtil;
import com.pamirs.attach.plugin.dbcp2.utils.DbcpMediaDataSource;
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
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Iterator;
import java.util.Map;

public class Dbcp2ShadowActiveEventListener implements PradarEventListener {

    private static Logger LOGGER = LoggerFactory.getLogger(Dbcp2ShadowDisableEventListener.class.getName());

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

        if (!(source.getClass().getName().equals("org.apache.commons.dbcp2.BasicDataSource"))) {
            return EventResult.IGNORE;
        }
        Thread.currentThread().setContextClassLoader(source.getClass().getClassLoader());
        BasicDataSource dbcpDataSource = (BasicDataSource) source;

        ShadowDatabaseConfig config = entry.getKey();
        int dsType = config.getDsType();

        DbcpMediaDataSource media = null;
        // 找到对应的数据源对
        Iterator<Map.Entry<DataSourceMeta, DbcpMediaDataSource>> it = DataSourceWrapUtil.pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, DbcpMediaDataSource> entry1 = it.next();
            BasicDataSource dataSource = entry1.getValue().getDataSourceBusiness();
            if (dataSource.equals(dbcpDataSource)) {
                media = entry1.getValue();
                break;
            }
        }
        SqlParser.clear();
        try {
            // 没有找到对应的数据源对
            if (media == null) {
                buildShadowDataSource(dsType, dbcpDataSource, config);
                return EventResult.success("[dbcp2]: handler shadow datasource active event success.");
            }
            // 找到了成对的数据源
            refreshShadowDataSource(dsType, dbcpDataSource, config, media);
            return EventResult.success("[dbcp2]: handler shadow datasource active event success.");
        } catch (Exception e) {
            LOGGER.error("[dbcp2]: handler shadow datasource active event occur exception", e);
            return EventResult.error("active-shadow-datasource-event", "module-alibaba-druid: handler shadow datasource active event occur exception.");
        }
    }

    private void buildShadowDataSource(int dsType, BasicDataSource dataSource, ShadowDatabaseConfig config) {
        DataSourceMeta<BasicDataSource> dataSourceMeta = new DataSourceMeta<BasicDataSource>(dataSource.getUrl(), dataSource.getUsername(), dataSource);
        // 影子表
        if (dsType == 1) {
            DbcpMediaDataSource dbMediatorDataSource = new DbcpMediaDataSource();
            dbMediatorDataSource.setDataSourceBusiness(dataSource);
            DbcpMediaDataSource old = DataSourceWrapUtil.pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
            if (old != null) {
                LOGGER.info("[dbcp2] destroyed shadow table datasource success. url:{} ,username:{}", dataSource.getUrl(), dataSource.getUsername());
                old.close();
            }
            return;
        }
        // 影子库 影子库/表
        BasicDataSource ptDataSource = DataSourceWrapUtil.generate(dataSource, config);
        if (ptDataSource == null) {
            LOGGER.error("[dbcp2] handler shadow datasource active event failed, create shadow datasource error. maybe datasource config is not correct, url: {} username:{} configuration:{}", dataSource.getUrl(), dataSource.getUsername(), config);
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0003")
                    .setMessage("影子库配置异常，无法由配置正确生成影子库！")
                    .setDetail("url: " + dataSource.getUrl() + " username: " + dataSource.getUsername())
                    .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                    .report();
            return;
        }
        DbcpMediaDataSource dbMediatorDataSource = new DbcpMediaDataSource();
        dbMediatorDataSource.setDataSourceBusiness(dataSource);
        dbMediatorDataSource.setDataSourcePerformanceTest(ptDataSource);
        DbcpMediaDataSource old = DataSourceWrapUtil.pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
        if (old != null) {
            LOGGER.info("[dbcp2] destroyed shadow table datasource success. url:{} ,username:{}", dataSource.getUrl(), dataSource.getUsername());
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
    private void refreshShadowDataSource(int dsType, BasicDataSource dataSource, ShadowDatabaseConfig config, DbcpMediaDataSource media) {
        BasicDataSource ptDataSource = media.getDataSourcePerformanceTest();
        // 影子表模式不修改
        if (dsType == 1) {
            media.resetIniStated();
            if (ptDataSource != null) {
                media.close();
                media.setDataSourcePerformanceTest(null);
                LOGGER.info("[dbcp2] biz datasource with url:{}, username:{} change to shadow table type, close shadow datasource!", dataSource.getUrl(), dataSource.getUsername());
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
        LOGGER.info("[dbcp2] handler shadow datasource active event, refresh shadow datasource, url:{}, username:{}", dataSource.getUrl(), dataSource.getUsername());
        media.setDataSourcePerformanceTest(ptDataSource);
        media.resetIniStated();
    }

    @Override
    public int order() {
        return 19;
    }

    private String buildDataSourceKey(BasicDataSource dataSource) {
        return DbUrlUtils.getKey(dataSource.getUrl(), dataSource.getUsername());
    }
}
