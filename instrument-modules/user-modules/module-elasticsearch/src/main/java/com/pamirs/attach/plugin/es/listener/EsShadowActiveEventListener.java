package com.pamirs.attach.plugin.es.listener;

import com.pamirs.attach.plugin.es.shadowserver.ShadowEsClientHolder;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowEsActiveEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EsShadowActiveEventListener implements PradarEventListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(EsShadowActiveEventListener.class.getName());

    @Override
    public EventResult onEvent(IEvent iEvent) {
        if (!(iEvent instanceof ShadowEsActiveEvent)) {
            return EventResult.IGNORE;
        }
        ShadowEsActiveEvent event = (ShadowEsActiveEvent) iEvent;
        Thread.currentThread().setContextClassLoader(event.getRestClient().getClass().getClassLoader());

        RestClient bizClient = (RestClient) event.getRestClient();
        try {
            RestClient shadowRestClient = ShadowEsClientHolder.getShadowRestClient(bizClient);
            if (shadowRestClient == null) {
                event.handlerResult(String.format("业务node:%s, 生效配置失败,未知异常,请联系交付查看探针异常日志;", event.getBizNodes()));
                return EventResult.success(event.getBizNodes());
            }
            event.handlerResult("success");
            return EventResult.success(event.getBizNodes());
        } catch (Throwable e) {
            LOGGER.error("[es] active shadow es config occur exception", e);
            event.handlerResult(String.format("业务node:%s, 生效配置失败, 异常信息:%s;", event.getBizNodes(), e.getMessage()));
            return EventResult.success(event.getBizNodes());
        }
    }

    @Override
    public int order() {
        return 37;
    }
}
