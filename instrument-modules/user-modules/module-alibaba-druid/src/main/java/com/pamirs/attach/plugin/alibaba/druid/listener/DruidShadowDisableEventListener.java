package com.pamirs.attach.plugin.alibaba.druid.listener;

import com.alibaba.druid.pool.DruidDataSource;
import com.pamirs.attach.plugin.alibaba.druid.obj.DbDruidMediatorDataSource;
import com.pamirs.attach.plugin.alibaba.druid.util.DataSourceWrapUtil;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceDisableEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.datasource.SqlParser;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DruidShadowDisableEventListener implements PradarEventListener {

    private static Logger logger = LoggerFactory.getLogger(DruidShadowDisableEventListener.class.getName());

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
        if (!(dataSourceClass.getClass().getName().equals("com.alibaba.druid.pool.DruidDataSource"))) {
            return EventResult.IGNORE;
        }
        Iterator<Map.Entry<DataSourceMeta, DbDruidMediatorDataSource>> it = DataSourceWrapUtil.pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, DbDruidMediatorDataSource> entry = it.next();
            DruidDataSource shadowDataSource = entry.getValue().getDataSourcePerformanceTest();
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
                    logger.info("module-alibaba-druid: destroyed shadow datasource success. url:{} ,username:{}", entry.getKey().getUrl(), entry.getKey().getUsername());
                }
            } catch (Throwable e) {
                logger.error("module-alibaba-druid: closed datasource err! target:{}, url:{} username:{}", entry.getKey().getDataSource().hashCode(), entry.getKey().getUrl(), entry.getKey().getUsername(), e);
            }
        }
        SqlParser.clear();
        return EventResult.success("module-alibaba-druid: handler shadow datasource disable event success,  destroyed shadow table datasource success.");
    }

    @Override
    public int order() {
        return 13;
    }


    private String buildShadowKey(DruidDataSource shadowDataSource) {
        return DbUrlUtils.getKey(shadowDataSource.getUrl(), shadowDataSource.getUsername());
    }
}
