package com.pamirs.attach.plugin.es.listener;

import com.pamirs.attach.plugin.es.shadowserver.ShadowEsClientHolder;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowEsDisableEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;

public class EsShadowDisableEventListener implements PradarEventListener {

    @Override
    public EventResult onEvent(IEvent iEvent) {
        if (!(iEvent instanceof ShadowEsDisableEvent)) {
            return EventResult.IGNORE;
        }
        ShadowEsDisableEvent event = (ShadowEsDisableEvent) iEvent;
        Thread.currentThread().setContextClassLoader(event.getBizClassLoader());
        ShadowEsClientHolder.closeShadowClient(event.getShadowHosts());
        event.getLatch().countDown();
        return EventResult.success(iEvent.getTarget());
    }

    @Override
    public int order() {
        return 36;
    }
}
