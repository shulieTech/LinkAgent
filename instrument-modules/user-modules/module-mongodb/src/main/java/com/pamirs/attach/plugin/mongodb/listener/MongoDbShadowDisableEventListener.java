package com.pamirs.attach.plugin.mongodb.listener;

import com.pamirs.attach.plugin.mongodb.utils.Caches;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowMongoDisableEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;

public class MongoDbShadowDisableEventListener implements PradarEventListener {

    @Override
    public EventResult onEvent(IEvent iEvent) {
        if (!(iEvent instanceof ShadowMongoDisableEvent)) {
            return EventResult.IGNORE;
        }
        ShadowMongoDisableEvent event = (ShadowMongoDisableEvent) iEvent;
        boolean isV4 = event.isV4();
        if (isV4) {
            return EventResult.IGNORE;
        }
        // 直接清空所有缓存
        Caches.clean();
        return EventResult.success(((ShadowMongoDisableEvent) iEvent).getShadowUrls());
    }

    @Override
    public int order() {
        return 26;
    }
}
