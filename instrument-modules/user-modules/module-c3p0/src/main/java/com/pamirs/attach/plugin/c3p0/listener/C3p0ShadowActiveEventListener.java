package com.pamirs.attach.plugin.c3p0.listener;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.pamirs.attach.plugin.c3p0.utils.C3p0MediaDataSource;
import com.pamirs.attach.plugin.c3p0.utils.DataSourceWrapUtil;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowDataSourceActiveEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.datasource.SqlParser;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class C3p0ShadowActiveEventListener implements PradarEventListener {

    private static Logger LOGGER = LoggerFactory.getLogger(C3p0ShadowDisableEventListener.class.getName());

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

        if (!(source.getClass().getName().equals("com.mchange.v2.c3p0.ComboPooledDataSource"))) {
            return EventResult.IGNORE;
        }
        Thread.currentThread().setContextClassLoader(source.getClass().getClassLoader());
        ComboPooledDataSource dbcpDataSource = (ComboPooledDataSource) source;

        ShadowDatabaseConfig config = entry.getKey();
        int dsType = config.getDsType();

        C3p0MediaDataSource media = null;
        // 找到对应的数据源对
        Iterator<Map.Entry<DataSourceMeta, C3p0MediaDataSource>> it = DataSourceWrapUtil.pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, C3p0MediaDataSource> entry1 = it.next();
            ComboPooledDataSource dataSource = entry1.getValue().getDataSourceBusiness();
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
                return EventResult.success("[c3p0]: handler shadow datasource active event success.");
            }
            // 找到了成对的数据源
            refreshShadowDataSource(dsType, dbcpDataSource, config, media);
            return EventResult.success("[c3p0]: handler shadow datasource active event success.");
        } catch (Exception e) {
            LOGGER.error("[c3p0]: handler shadow datasource active event occur exception", e);
            return EventResult.error("active-shadow-datasource-event", "module-alibaba-druid: handler shadow datasource active event occur exception.");
        }
    }

    private void buildShadowDataSource(int dsType, ComboPooledDataSource dataSource, ShadowDatabaseConfig config) {
        String url = extractUrl(dataSource);
        String username = extractUsername(dataSource);
        DataSourceMeta<ComboPooledDataSource> dataSourceMeta = new DataSourceMeta<ComboPooledDataSource>(url, username, dataSource);
        // 影子表
        if (dsType == 1) {
            C3p0MediaDataSource dbMediatorDataSource = new C3p0MediaDataSource();
            dbMediatorDataSource.setDataSourceBusiness(dataSource);
            C3p0MediaDataSource old = DataSourceWrapUtil.pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
            if (old != null) {
                LOGGER.info("[c3p0] destroyed shadow table datasource success. url:{} ,username:{}", url, username);
                old.close();
            }
            return;
        }
        // 影子库 影子库/表
        ComboPooledDataSource ptDataSource = DataSourceWrapUtil.generate(dataSource, config);
        if (ptDataSource == null) {
            LOGGER.error("[c3p0] handler shadow datasource active event failed, create shadow datasource error. maybe datasource config is not correct, url: {} username:{} configuration:{}", url, username, config);
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0003")
                    .setMessage("影子库配置异常，无法由配置正确生成影子库！")
                    .setDetail("url: " + url+ " username: " + username)
                    .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                    .report();
            return;
        }
        C3p0MediaDataSource dbMediatorDataSource = new C3p0MediaDataSource();
        dbMediatorDataSource.setDataSourceBusiness(dataSource);
        dbMediatorDataSource.setDataSourcePerformanceTest(ptDataSource);
        C3p0MediaDataSource old = DataSourceWrapUtil.pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
        if (old != null) {
            LOGGER.info("[c3p0] destroyed shadow table datasource success. url:{} ,username:{}", url, username);
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
    private void refreshShadowDataSource(int dsType, ComboPooledDataSource dataSource, ShadowDatabaseConfig config, C3p0MediaDataSource media) {
        String url = extractUrl(dataSource);
        String username = extractUsername(dataSource);
        ComboPooledDataSource ptDataSource = media.getDataSourcePerformanceTest();
        // 影子表模式不修改
        if (dsType == 1) {
            media.resetIniStated();
            if (ptDataSource != null) {
                media.close();
                media.setDataSourcePerformanceTest(null);
                LOGGER.info("[c3p0] biz datasource with url:{}, username:{} change to shadow table type, close shadow datasource!", url, username);
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
        LOGGER.info("[c3p0] handler shadow datasource active event, refresh shadow datasource, url:{}, username:{}", url, username);
        media.setDataSourcePerformanceTest(ptDataSource);
        media.resetIniStated();
    }

    @Override
    public int order() {
        return 21;
    }

    private String buildDataSourceKey(ComboPooledDataSource target) {
        return DbUrlUtils.getKey(extractUrl(target), extractUsername(target));
    }

    private String extractUrl(ComboPooledDataSource target){
        return ReflectionUtils.getFieldValues(target, "dmds", "jdbcUrl");
    }

    private String extractUsername(ComboPooledDataSource target){
        Properties properties = ReflectionUtils.getFieldValues(target, "dmds", "properties");
        return properties.getProperty("user");
    }
}
