package com.pamirs.attach.plugin.shadow.preparation.command.processor;

import com.pamirs.attach.plugin.shadow.preparation.jdbc.JdbcDataSourceFetcher;
import com.pamirs.attach.plugin.shadow.preparation.mongo.MongoDataSourceFetcher;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowMongoDisableEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MongoConfigPushCommandProcessor {

    public static void processConfigPushCommand(List<ShadowDatabaseConfig> mongoConfigs) {
        Object[] compareResults = compareShadowDataSource(mongoConfigs);
        Set<String> needClosed = (Set<String>) compareResults[0];
        EventRouter.router().publish(new ShadowMongoDisableEvent(MongoDataSourceFetcher.isMongoV4(), needClosed));
    }

    /**
     * 返回新增的和需要陪关闭的影子数据源
     * 需要新增的数据源不管,主要处理需要关闭的数据源
     *
     * @param data
     * @return
     */
    private static Object[] compareShadowDataSource(List<ShadowDatabaseConfig> data) {
        // 需要被关闭的影子数据源
        Set<String> needClosed = new HashSet<>(MongoDataSourceFetcher.getShadowKeys());
        // 新增的数据源
        Set<ShadowDatabaseConfig> needAdd = new HashSet<>();

        for (ShadowDatabaseConfig config : data) {
            // 影子表模式直接加，反正不用创建数据源
            if (config.getDsType() == 1) {
                needAdd.add(config);
                continue;
            }
            // 当前影子数据源存在
            if (needClosed.remove(config.getShadowUrl())) {
                continue;
            }
            needAdd.add(config);
        }
        // 遇到特殊情况, 多个业务数据源的影子数据源是一样的, 需要禁用其中一个
        if (needClosed.isEmpty() && data.size() < JdbcDataSourceFetcher.getShadowDataSourceNum()) {
            // 因为没有保存业务数据源和影子数据源的映射关系，所以清除所有影子数据源，重新构建
            return new Object[]{new HashSet<>(JdbcDataSourceFetcher.getShadowKeys()), data};
        }
        return new Object[]{needClosed, needAdd};
    }


}
