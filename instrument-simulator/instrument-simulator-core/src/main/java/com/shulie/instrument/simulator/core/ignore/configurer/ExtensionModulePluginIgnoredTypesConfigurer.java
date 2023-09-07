package com.shulie.instrument.simulator.core.ignore.configurer;

import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesConfigurer;

public class ExtensionModulePluginIgnoredTypesConfigurer implements IgnoredTypesConfigurer {
    @Override
    public void configure(IgnoredTypesBuilder builder) {
        //alibaba-dubbo
        builder.ignoreClass("com.alibaba.dubbo.")
                .allowClass("com.alibaba.dubbo.rpc.protocol.AbstractInvoker")
                .allowClass("com.alibaba.dubbo.remoting.exchange.support.header.HeaderExchangeHandler")
                .allowClass("com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker")
                .allowClass("com.alibaba.dubbo.remoting.http.servlet.DispatcherServlet");

        //aliyun-ons
        builder.ignoreClass("com.aliyun.openservices.")
                .allowClass("com.aliyun.openservices.ons.api.impl.rocketmq.ConsumerImpl")
                .allowClass("com.aliyun.openservices.ons.api.impl.rocketmq.OrderConsumerImpl")
                .allowClass("com.aliyun.openservices.ons.api.impl.rocketmq.BatchConsumerImpl");

        //aliyun-openservices
        builder.ignoreClass("com.aliyun.openservices.")
                .allowClass("com.aliyun.openservices.ons.api.impl.rocketmq.ConsumerImpl$MessageListenerImpl")
                .allowClass("com.aliyun.openservices.ons.api.impl.rocketmq.BatchConsumerImpl$BatchMessageListenerImpl")
                .allowClass("com.aliyun.openservices.ons.api.impl.rocketmq.OrderConsumerImpl$MessageListenerOrderlyImpl")
                .allowClass("com.aliyun.openservices.shade.com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer")
                .allowClass("com.aliyun.openservices.shade.com.alibaba.rocketmq.client.impl.consumer.DefaultMQPullConsumerImpl")
                .allowClass("com.aliyun.openservices.shade.com.alibaba.rocketmq.client.producer.DefaultMQProducer")
                .allowClass("com.aliyun.openservices.shade.com.alibaba.rocketmq.client.producer.TransactionMQProducer")
                .allowClass("com.aliyun.openservices.shade.com.alibaba.rocketmq.client.impl.producer.DefaultMQProducerImpl")
                .allowClass("com.aliyun.openservices.shade.com.alibaba.rocketmq.client.impl.consumer.ConsumeMessageConcurrentlyService$ConsumeRequest")
                .allowClass("com.aliyun.openservices.shade.com.alibaba.rocketmq.client.impl.consumer.ConsumeMessageOrderlyService$ConsumeRequest")
                .allowClass("com.aliyun.openservices.shade.com.alibaba.rocketmq.client.impl.consumer.ProcessQueue")
                .allowClass("com.aliyun.openservices.shade.com.alibaba.rocketmq.client.impl.consumer.ConsumeMessageOrderlyService")
                .allowClass("com.aliyun.openservices.ons.api.impl.rocketmq.ConsumerImpl$MessageListenerImpl")
                .allowClass("com.aliyun.openservices.ons.api.impl.rocketmq.BatchConsumerImpl$BatchMessageListenerImpl")
                .allowClass("com.aliyun.openservices.ons.api.impl.rocketmq.OrderConsumerImpl$MessageListenerOrderlyImpl")
                .allowClass("com.aliyun.openservices.ons.api.impl.ONSFactoryImpl");

        //amazon-s3
        builder.ignoreClass("com.amazonaws.")
                .allowClass("com.amazonaws.services.s3.AmazonS3Client")
                .allowClass("com.amazonaws.http.AmazonHttpClient");

        //best
        builder.ignoreClass("com.best.")
                .allowClass("com.best.oasis.q9xingng.service.bill.BillCodeDetailServiceImpl")
                .allowClass("com.best.gateway.http.server.impl.HttpServerHandler")
                .allowClass("com.best.xingng.org.apache.cxf.interceptor.MessageSenderInterceptor");

        //dau-stats
        builder.allowClass("io.shulie.takin.web.plugin.user.config.intercepter.LoginInterceptor");

        //druid-checker
        builder.allowClass("com.alibaba.druid.pool.DruidPooledStatement")
                .allowClass("com.alibaba.druid.pool.DruidPooledPreparedStatement")
                .allowClass("com.alibaba.druid.pool.DruidPooledCallableStatement");

        //edas-hsf
        builder.ignoreClass("com.taobao.hsf.")
                .allowClass("com.taobao.hsf.plugins.eagleeye.EagleEyeClientFilter")
                .allowClass("com.taobao.hsf.plugins.eagleeye.EagleEyeServerFilter")
                .allowClass("com.taobao.hsf.plugins.eagleeye.http.EagleEyeHttpHook");

        //extension-shadow-job
        builder.ignoreClass("com.sf.push.")
                .allowClass("com.taobao.pamirs.schedule.strategy.ManagerFactoryTimerTask")
                .allowClass("com.sf.timer.core.support.TaskWorker")
                .allowClass("com.sf.push.serviceimpl.PushDisorderSendThread")
                .allowClass("com.sf.push.nserviceimpl.PushDisorderSendThread")
                .allowClass("com.sf.push.serviceimpl.PushDisorderReadThread")
                .allowClass("com.sf.push.nserviceimpl.PushDisorderReadThread")
                .allowClass("com.sf.push.nserviceimpl.PushCustomPrivateReadThread")
                .allowClass("com.sf.push.nserviceimpl.PushCustomPrivateSendThread")
                .allowClass("com.sf.push.serviceimpl.PushCustomPrivateReadThread")
                .allowClass("com.sf.push.serviceimpl.PushCustomPrivateSendThread")
                .allowClass("com.sf.timer.push.job.CommonUploadOrderDataJob");

        //fandeng-kafka
        builder.ignoreClass("com.soybean.")
                .allowClass("com.soybean.eventbus.kafka.subscriber.BaseKafkaSubscriber");

        //guocai-bes-engine
        builder.ignoreClass("com.bes.")
                .allowClass("com.bes.enterprise.webtier.core.DefaultHostValve")
                .allowClass("com.bes.enterprise.webtier.connector.Request");

        //gw-customized
        builder.allowClass("feign.ReflectiveFeign$BuildEncodedTemplateFromArgs")
                .allowClass("feign.ReflectiveFeign$BuildFormEncodedTemplateFromArgs")
                .allowClass("feign.ReflectiveFeign$BuildTemplateByResolvingArgs");

        //huawei-alibaba-mqs
        builder.ignoreClass("com.alibaba.rocketmq.")
                .allowClass("com.alibaba.rocketmq.client.ext.consumer.DefaultMQPushConsumer")
                .allowClass("com.alibaba.rocketmq.client.ext.impl.consumer.DefaultMQPushConsumerImpl")
                .allowClass("com.alibaba.rocketmq.client.ext.impl.DefaultMQPullConsumerImpl")
                .allowClass("com.alibaba.rocketmq.client.ext.producer.DefaultMQProducer")
                .allowClass("com.alibaba.rocketmq.client.ext.producer.TransactionMQProducer")
                .allowClass("com.alibaba.rocketmq.client.ext.impl.producer.DefaultMQProducerImpl");


        //huawei-apache-mqs
        builder.ignoreClass("org.apache.rocketmq.")
                .allowClass("org.apache.rocketmq.client.ext.consumer.DefaultMQPushConsumer")
                .allowClass("org.apache.rocketmq.client.ext.impl.consumer.DefaultMQPushConsumerImpl")
                .allowClass("org.apache.rocketmq.client.ext.impl.DefaultMQPullConsumerImpl")
                .allowClass("org.apache.rocketmq.client.ext.producer.DefaultMQProducer")
                .allowClass("org.apache.rocketmq.client.ext.producer.TransactionMQProducer")
                .allowClass("org.apache.rocketmq.client.ext.impl.producer.DefaultMQProducerImpl");

        //ldy
        builder.ignoreClass("com.ldygo.")
                .allowClass("com.ldygo.rocketmq.listeners.AbstractMessageListener");

        //log4j
        builder.ignoreClass("org.apache.log4j.")
                .allowClass("org.apache.log4j.Category")
                .allowClass("org.apache.log4j.AppenderSkeleton")
                .allowClass("org.apache.logging.log4j.core.config.AppenderControl")
                .allowClass("org.apache.logging.log4j.core.config.LoggerConfig")
                .allowClass("org.apache.logging.log4j.spi.AbstractLogger");

        //media-rocketmq
        builder.allowClass("org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl");

        //oss
        builder.ignoreClass("com.aliyun.oss.")
                .allowClass("com.aliyun.oss.internal.OSSOperation")
                .allowClass("com.aliyun.oss.common.comm.ServiceClient")
                .allowClass("com.aliyun.oss.OSSClient")
                .allowClass("com.aliyun.oss.common.parser.RequestMarshallers$DeleteObjectsRequestMarshaller");

        //renshou
        builder.ignoreClass("com.yuancore.cms.")
                .allowClass("com.yuancore.cms.client.CmsClient")
                .allowClass("com.yuancore.cms.client.common.CmsRequestExecutor");

        //sf-kafka
        builder.ignoreClass("com.sf.kafka.api.")
                .allowClass("com.sf.kafka.api.consume.MessageConverListener")
                .allowClass("com.sf.kafka.api.consume.domain.KafkaMessage")
                .allowClass("com.sf.kafka.api.client.KafkaConsumer24$FetchDataThread")
                .allowClass("com.sf.kafka.api.consume.KafkaConsumer$FetchDataThread")
                .allowClass("com.sf.kafka.api.produce.ProducerPool")
                .allowClass("com.sf.kafka.api.produce.BaseProducer")
                .allowClass("com.sf.kafka.api.client.KafkaConsumer24")
                .allowClass("com.sf.kafka.api.consume.KafkaConsumer")
                .allowClass("com.sf.kafka.api.consume.TransactionalConsumer")
                .allowClass("com.sf.kafka.api.consume.NonTransactionalConsumer")
                .allowClass("com.sf.kafka.api.client.VersionTools");

        //shulie
        builder.allowClass("com.shulie.agent.plugin.kafka.consumer.consumer.BaseKafkaSubscriber");

        //slick
        builder.ignoreClass("slick.")
                .allowClass("slick.util.ManagedArrayBlockingQueue");

        //starnet-rabbitmq
        builder.ignoreClass("com.starnet.rabbitmq.")
                .allowClass("com.starnet.springframework.amqp.rabbit.core.RabbitAdmin")
                .allowClass("com.starnet.rabbitmq.client.impl.recovery.RecoveryAwareChannelN")
                .allowClass("com.starnet.rabbitmq.client.impl.ChannelN")
                .allowClass("com.starnet.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer")
                .allowClass("com.starnet.rabbitmq.client.QueueingConsumer")
                .allowClass("com.starnet.springframework.amqp.rabbit.support.PublisherCallbackChannelImpl")
                .allowClass("com.starnet.springframework.amqp.rabbit.listener.BlockingQueueConsumer$InternalConsumer")
                .allowClass("com.starnet.springframework.amqp.rabbit.listener.BlockingQueueConsumer")
                .allowClass("com.starnet.cloudmq.rabbitmq.publish.CloudMqSendAbstract")
                .allowClass("com.starnet.cloudmq.rabbitmq.consumer.CloudMqConsumerAbstract");


        //sto
        builder.ignoreClass("com.sto.").ignoreClass("cn.sto.")
                .allowClass("com.sto.event.ocean.client.service.Pusher")
                .allowClass("com.sto.event.ocean.client.Event")
                .allowClass("com.sto.event.ocean.client.service.EventExecService")
                .allowClass("com.sto.event.ocean.client.service.SubscribeImpl")
                .allowClass("cn.sto.galaxy.receive.mqtt.listener.NewIoTQpidApplicationListener");

        //t3-cache
        builder.ignoreClass("com.t3.ts.cache.")
                .allowClass("com.t3.ts.cache.redis.proxy.DBManager")
                .allowClass("com.t3.ts.marketing.act.service.remote.impl.MarketingActNewServiceImpl")
                .allowClass("com.t3.ts.marketing.act.service.remote.impl.MarketingActNewServiceImpl")
                .allowClass("com.t3.ts.marketing.act.dao.cache.ActCacheDao");

        //weblogic
        builder.ignoreClass("weblogic.")
                .allowClass("weblogic.servlet.internal.ServletRequestImpl")
                .allowClass("weblogic.servlet.internal.WebAppServletContext");

        //za24
        builder.ignoreClass("com.cmb.dtpframework.")
                .allowClass("com.cmb.dtpframework.datasource.DBCnnPool")
                .allowClass("com.cmb.dtpframework.cmmclient.AppClient")
                .allowClass("com.cmb.dtpframework.tcpserver.TcpExecute");

        //zto
        builder.ignoreClass("com.zto.")
                .allowClass("com.zto.consumer.KafkaConsumerProxy")
                .allowClass("org.springframework.context.support.AbstractRefreshableApplicationContext")
                .allowClass("org.springframework.context.support.ApplicationContextAwareProcessor");

        //ots
        builder.ignoreClass("com.alicloud.openservices.tablestore")
                .allowClass("com.alicloud.openservices.tablestore.InternalClient")
                .allowClass("com.alicloud.openservices.tablestore.core.LauncherFactory");

        //liantong-kafka
        builder.allowClass("com.tianyan.kafka.consumer.KafkaMsgSend");
    }
}
