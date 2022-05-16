package com.pamirs.attach.plugin.apache.kafka.interceptor;

import com.pamirs.attach.plugin.apache.kafka.destroy.KafkaDestroy;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

@Destroyable(KafkaDestroy.class)
public class AbstractConfigGetInstanceInterceptor extends CutoffInterceptorAdaptor {

    private final Logger log = LoggerFactory.getLogger(AbstractConfigGetInstanceInterceptor.class);

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        Class<?> clazz = (Class<?>) advice.getParameterArray()[1];
        if (!"org.apache.kafka.clients.consumer.ConsumerInterceptor".equals(clazz.getName())) {
            return CutOffResult.PASSED;
        }
        log.info("拦截Consumer interceptor获取, 返回空集合");
        return CutOffResult.cutoff(new ArrayList());
    }

}
