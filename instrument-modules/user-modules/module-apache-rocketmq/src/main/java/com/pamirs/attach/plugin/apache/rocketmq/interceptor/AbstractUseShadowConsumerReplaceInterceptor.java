package com.pamirs.attach.plugin.apache.rocketmq.interceptor;

import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/17 11:00 AM
 */
public abstract class AbstractUseShadowConsumerReplaceInterceptor extends CutoffInterceptorAdaptor {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public CutOffResult cutoff0(Advice advice) {
        return CutOffResult.passed();
//        if (!Pradar.isClusterTest()) {
//            return CutOffResult.passed();
//        }
//        /**
//         * 主要负责Consumer 注册，每一批的消息消费都会经过此方法
//         * 如果是已经注册过的，则忽略
//         */
//        DefaultMQPushConsumer defaultMQPushConsumer = (DefaultMQPushConsumer)advice.getTarget();
//        /**
//         * 如果影子消费者，也忽略
//         */
//        if (ConsumerRegistry.isShadowConsumer(defaultMQPushConsumer)) {
//            return CutOffResult.passed();
//        }
//
//        if (!ConsumerRegistry.hasRegistered(defaultMQPushConsumer)) {
//            throw new PressureMeasureError(String.format("Apache-RocketMQ: %s err, can't found shadow consumer, queue: %s ",
//                advice.getBehaviorName(), advice.getParameterArray()[0]));
//        }
//
//        DefaultMQPushConsumer consumer = ConsumerRegistry.getConsumer(defaultMQPushConsumer);
//        if (consumer == null) {
//            throw new PressureMeasureError("Apache-RocketMQ: shadow consumer is null this should never happened!");
//        }
//        return execute(consumer, advice);
    }

    protected abstract CutOffResult execute(DefaultMQPushConsumer consumer, Advice advice);
}
