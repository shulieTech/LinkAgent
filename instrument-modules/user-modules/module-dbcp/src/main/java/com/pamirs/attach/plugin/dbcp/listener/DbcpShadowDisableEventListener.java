package com.pamirs.attach.plugin.dbcp.listener;

import com.pamirs.attach.plugin.dbcp.utils.DataSourceWrapUtil;
import com.pamirs.attach.plugin.dbcp.utils.DbcpMediaDataSource;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceDisableEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.datasource.SqlParser;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

public class DbcpShadowDisableEventListener implements PradarEventListener {

    private static Logger logger = LoggerFactory.getLogger(DbcpShadowDisableEventListener.class.getName());

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
        if (!(dataSourceClass.getClass().getName().equals("org.apache.commons.dbcp.BasicDataSource"))) {
            return EventResult.IGNORE;
        }
        Iterator<Map.Entry<DataSourceMeta, DbcpMediaDataSource>> it = DataSourceWrapUtil.pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, DbcpMediaDataSource> entry = it.next();
            BasicDataSource shadowDataSource = entry.getValue().getDataSourcePerformanceTest();
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
        return 18;
    }


    private String buildShadowKey(BasicDataSource shadowDataSource) {
        return DbUrlUtils.getKey(shadowDataSource.getUrl(), shadowDataSource.getUsername());
    }
}
