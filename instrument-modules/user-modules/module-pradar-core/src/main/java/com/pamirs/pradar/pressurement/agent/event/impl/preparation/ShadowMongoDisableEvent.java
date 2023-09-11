package com.pamirs.pradar.pressurement.agent.event.impl.preparation;

import com.pamirs.pradar.pressurement.agent.event.IEvent;

import java.util.Set;

public class ShadowMongoDisableEvent implements IEvent {

    private boolean isV4;
    private Set<String> shadowUrls;

    public ShadowMongoDisableEvent(boolean isV4, Set<String> shadowUrls) {
        this.isV4 = isV4;
        this.shadowUrls = shadowUrls;
    }

    public boolean isV4() {
        return isV4;
    }

    public Set<String> getShadowUrls() {
        return shadowUrls;
    }

    @Override
    public Object getTarget() {
        return shadowUrls;
    }
}
