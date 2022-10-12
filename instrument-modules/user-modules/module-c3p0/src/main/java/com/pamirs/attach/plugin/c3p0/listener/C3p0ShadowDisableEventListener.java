package com.pamirs.attach.plugin.c3p0.listener;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.pamirs.attach.plugin.c3p0.utils.C3p0MediaDataSource;
import com.pamirs.attach.plugin.c3p0.utils.DataSourceWrapUtil;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowDataSourceDisableEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.datasource.SqlParser;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class C3p0ShadowDisableEventListener implements PradarEventListener {

    private static Logger logger = LoggerFactory.getLogger(C3p0ShadowDisableEventListener.class.getName());

    @Override
    public EventResult onEvent(IEvent event) {
        if (!(event instanceof ShadowDataSourceDisableEvent)) {
            return EventResult.IGNORE;
        }
        Map.Entry<String, String> target = ((ShadowDataSourceDisableEvent) event).getTarget();
        if (target == null) {
            return EventResult.IGNORE;
        }
        String dataSourceClass = target.getKey();
        if (!(dataSourceClass.getClass().getName().equals("com.mchange.v2.c3p0.ComboPooledDataSource"))) {
            return EventResult.IGNORE;
        }
        Iterator<Map.Entry<DataSourceMeta, C3p0MediaDataSource>> it = DataSourceWrapUtil.pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, C3p0MediaDataSource> entry = it.next();
            ComboPooledDataSource shadowDataSource = entry.getValue().getDataSourcePerformanceTest();
            if (shadowDataSource == null) {
                continue;
            }
            if (!target.getValue().equals(buildShadowKey(shadowDataSource))) {
                continue;
            }
            it.remove();
            try {
                shadowDataSource.close();
                if (logger.isInfoEnabled()) {
                    logger.info("[dbcp]: destroyed shadow datasource success. url:{} ,username:{}", entry.getKey().getUrl(), entry.getKey().getUsername());
                }
            } catch (Throwable e) {
                logger.error("[dbcp]: closed datasource err! target:{}, url:{} username:{}", entry.getKey().getDataSource().hashCode(), entry.getKey().getUrl(), entry.getKey().getUsername(), e);
            }
        }
        SqlParser.clear();
        return EventResult.success("[dbcp]: handler shadow datasource disable event success,  destroyed shadow table datasource success.");
    }

    @Override
    public int order() {
        return 22;
    }


    private String buildShadowKey(ComboPooledDataSource target) {
        String url = ReflectionUtils.getFieldValues(target, "dmds", "jdbcUrl");
        Properties properties = ReflectionUtils.getFieldValues(target, "dmds", "properties");
        String username = properties.getProperty("user");
        return DbUrlUtils.getKey(url, username);
    }
}
