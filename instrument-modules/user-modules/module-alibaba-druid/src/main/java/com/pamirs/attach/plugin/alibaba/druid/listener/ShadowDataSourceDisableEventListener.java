package com.pamirs.attach.plugin.alibaba.druid.listener;

import com.alibaba.druid.pool.DruidDataSource;
import com.pamirs.attach.plugin.alibaba.druid.obj.DbDruidMediatorDataSource;
import com.pamirs.attach.plugin.alibaba.druid.util.DataSourceWrapUtil;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceDisableEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ShadowDataSourceDisableEventListener implements PradarEventListener {

    private static Logger logger = LoggerFactory.getLogger(ShadowDataSourceDisableEventListener.class.getName());

    @Override
    public EventResult onEvent(IEvent event) {
        if (!(event instanceof ShadowDataSourceDisableEvent)) {
            return EventResult.IGNORE;
        }
        Set<String> closeShadowKeys = ((ShadowDataSourceDisableEvent) event).getTarget();
        if (closeShadowKeys == null || closeShadowKeys.isEmpty()) {
            return EventResult.IGNORE;
        }
        Iterator<Map.Entry<DataSourceMeta, DbDruidMediatorDataSource>> it = DataSourceWrapUtil.pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, DbDruidMediatorDataSource> entry = it.next();
            DruidDataSource shadowDataSource = entry.getValue().getDataSourcePerformanceTest();
            if (shadowDataSource == null) {
                continue;
            }
            if (!closeShadowKeys.contains(buildShadowKey(shadowDataSource))) {
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
        return EventResult.success("module-alibaba-druid: handler shadow datasource disable event success,  destroyed shadow table datasource success.");
    }

    @Override
    public int order() {
        return 3;
    }


    private String buildShadowKey(DruidDataSource shadowDataSource) {
        return DbUrlUtils.getKey(shadowDataSource.getUrl(), shadowDataSource.getUsername());
    }
}
