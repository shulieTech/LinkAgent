package com.pamirs.attach.plugin.dbcp2.listener;

import com.pamirs.attach.plugin.dbcp2.utils.DataSourceWrapUtil;
import com.pamirs.attach.plugin.dbcp2.utils.DbcpMediaDataSource;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceDisableEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.datasource.SqlParser;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

public class Dbcp2ShadowDisableEventListener implements PradarEventListener {

    private static Logger logger = LoggerFactory.getLogger(Dbcp2ShadowDisableEventListener.class.getName());

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
        if (!(dataSourceClass.getClass().getName().equals("org.apache.commons.dbcp2.BasicDataSource"))) {
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
                    logger.info("[dbcp2]: destroyed shadow datasource success. url:{} ,username:{}", entry.getKey().getUrl(), entry.getKey().getUsername());
                }
            } catch (Throwable e) {
                logger.error("[dbcp2]: closed datasource err! target:{}, url:{} username:{}", entry.getKey().getDataSource().hashCode(), entry.getKey().getUrl(), entry.getKey().getUsername(), e);
            }
        }
        SqlParser.clear();
        return EventResult.success("[dbcp2]: handler shadow datasource disable event success,  destroyed shadow table datasource success.");
    }

    @Override
    public int order() {
        return 20;
    }


    private String buildShadowKey(BasicDataSource shadowDataSource) {
        return DbUrlUtils.getKey(shadowDataSource.getUrl(), shadowDataSource.getUsername());
    }
}
