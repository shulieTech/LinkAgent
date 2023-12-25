package com.shulie.instrument.simulator.core.ignore.configurer;

import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesConfigurer;

public class ModulePluginIgnoredTypesConfigurer implements IgnoredTypesConfigurer {

    @Override
    public void configure(IgnoredTypesBuilder builder) {
        //aerospike
        builder.ignoreClass("com.aerospike.client.")
                .allowClass("com.aerospike.client.Key")
                .allowClass("com.aerospike.client.AerospikeClient")
                .allowClass("com.aerospike.client.async.AsyncClient");

        //activemq
        builder.ignoreClass("org.apache.activemq.")
                .allowClass("org.apache.activemq.ActiveMQSession")
                .allowClass("org.apache.activemq.ActiveMQMessageConsumer")
                .allowClass("org.apache.activemq.ActiveMQMessageProducer");

        //akka

        //alibaba-druid
        builder.ignoreClass("com.alibaba.druid.")
                .allowClass("com.alibaba.druid.pool.DruidDataSource");

        //alibaba-rocketmq
        builder.ignoreClass("com.alibaba.rocketmq.")
                .allowClass("com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer")
                .allowClass("com.alibaba.rocketmq.client.impl.consumer.DefaultMQPullConsumerImpl")
                .allowClass("com.alibaba.rocketmq.client.producer.DefaultMQProducer")
                .allowClass("com.alibaba.rocketmq.client.impl.producer.DefaultMQProducerImpl")
                .allowClass("com.alibaba.rocketmq.client.impl.consumer.ConsumeMessageConcurrentlyService$ConsumeRequest")
                .allowClass("com.alibaba.rocketmq.client.impl.consumer.ConsumeMessageOrderlyService$ConsumeRequest")
                .allowClass("com.alibaba.rocketmq.client.consumer.listener.MessageListenerOrderly")
                .allowClass("com.alibaba.rocketmq.client.impl.consumer.ProcessQueue")
                .allowClass("com.alibaba.rocketmq.client.impl.consumer.ConsumeMessageOrderlyService");

        //apache-axis
        builder.ignoreClass("org.apache.axis.")
                .allowClass("org.apache.axis.client.Call");

        //apache-cxf
        builder.ignoreClass("org.apache.axis.")
                .allowClass("org.apache.cxf.endpoint.ClientImpl");

        //apache-dubbo
        builder.ignoreClass("org.apache.dubbo.")
                .allowClass("org.apache.dubbo.rpc.protocol.AbstractInvoker")
                .allowClass("org.apache.dubbo.rpc.proxy.AbstractProxyInvoker");

        //apache-hbase
        builder.ignoreClass("org.apache.hadoop.hbase.")
                .allowClass("org.apache.hadoop.hbase.client.ConnectionFactory")
                .allowClass("org.apache.hadoop.hbase.client.ConnectionManager$HConnectionImplementation")
                .allowClass("org.apache.hadoop.hbase.client.HTable");


        // kafka
        builder.ignoreClass("org.apache.kafka.")
                .allowClass("org.apache.kafka.clients.producer.KafkaProducer")
                .allowClassLoader("kafka.javaapi.producer.Producer")
                .allowClass("org.springframework.kafka.listener.adapter.RecordMessagingMessageListenerAdapter")
                .allowClass("org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter")
                .allowClass("org.springframework.kafka.listener.adapter.BatchMessagingMessageListenerAdapter")
                .allowClass("org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer")
                .allowClass("org.apache.kafka.clients.consumer.KafkaConsumer")
                .allowClass("org.apache.kafka.common.config.AbstractConfig")
                .allowClass("org.springframework.kafka.core.DefaultKafkaConsumerFactory");

        //kafka-stream
        builder.allowClass("org.apache.kafka.streams.kstream.internals.KStreamMap$KStreamMapProcessor")
                .allowClass("org.apache.kafka.streams.kstream.internals.KStreamPeek$KStreamPeekProcessor");

        //apache-rocketmq
        builder.ignoreClass("org.apache.rocketmq.client.")
                .allowClass("org.apache.rocketmq.client.consumer.DefaultMQPushConsumer")
                .allowClass("org.apache.rocketmq.client.impl.consumer.DefaultMQPullConsumerImpl")
                .allowClass("org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl")
                .allowClass("org.apache.rocketmq.client.impl.consumer.ConsumeMessageConcurrentlyService$ConsumeRequest")
                .allowClass("org.apache.rocketmq.client.impl.consumer.ConsumeMessageOrderlyService$ConsumeRequest")
                .allowClass("org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly")
                .allowClass("org.apache.rocketmq.client.impl.consumer.ProcessQueue")
                .allowClass("org.apache.rocketmq.client.impl.consumer.ConsumeMessageOrderlyService");

        //apache-tomcat-jdbc
        builder.ignoreClass("org.apache.tomcat.jdbc.")
                .allowClass("org.apache.tomcat.jdbc.pool.DataSourceProxy")
                .allowClass("org.apache.tomcat.jdbc.pool.ConnectionPool");

        //async-httpclient
        builder.ignoreClass("org.asynchttpclient.")
                .allowClass("org.asynchttpclient.netty.request.NettyRequestSender");

        //atomikos
        builder.ignoreClass("com.atomikos.jdbc.")
                .allowClass("com.atomikos.jdbc.nonxa.AtomikosNonXADataSourceBean")
                .allowClass("com.atomikos.jdbc.AbstractDataSourceBean");

        //c3p0
        builder.ignoreClass("com.mchange.v2.c3p0.")
                .allowClass("com.mchange.v2.c3p0.impl.AbstractPoolBackedDataSource")
                .allowClass("com.mchange.v2.c3p0.impl.C3P0PooledConnectionPool");

        //caffeine
        builder.ignoreClass("om.github.benmanes.caffeine.")
                .allowClass("com.github.benmanes.caffeine.cache.UnboundedLocalCache")
                .allowClass("com.github.benmanes.caffeine.cache.BoundedLocalCache")
                .allowClass("com.github.benmanes.caffeine.cache.CacheLoader");

        //catalina
        builder.ignoreClass("org.apache.catalina.")
                .ignoreClass("org.apache.coyote.")
                .ignoreClass("org.apache.juli.")
                .ignoreClass("org.apache.naming.")
                .allowClass("org.apache.catalina.core.StandardHostValve")
                .allowClass("org.apache.catalina.connector.Request")
                .allowClass("org.springframework.web.servlet.handler.AbstractHandlerMethodMapping$MappingRegistry");

        //cluster-test-check
        builder.allowClass("com.caucho.burlap.server.BurlapServlet")
                .allowClass("org.apache.catalina.servlets.CGIServlet")
                .allowClass("org.apache.catalina.servlets.DefaultServlet")
                .allowClass("org.apache.catalina.servlets.WebdavServlet")
                .allowClass("org.eclipse.jetty.servlet.DefaultServlet")
                .allowClass("com.caucho.servlets.DirectoryServlet")
                .allowClass("com.alibaba.dubbo.remoting.http.servlet.DispatcherServlet")
                .allowClass("org.apache.dubbo.remoting.http.servlet.DispatcherServlet")
                .allowClass("org.springframework.web.servlet.DispatcherServlet")
                .allowClass("com.caucho.burlap.EJBServlet")
                .allowClass("com.caucho.ejb.EJBServlet")
                .allowClass("com.caucho.ejb.burlap.EJBServlet")
                .allowClass("com.caucho.ejb.hessian.EJBServlet")
                .allowClass("com.caucho.hessian.EJBServlet")
                .allowClass("javax.servlet.GenericServlet")
                .allowClass("com.caucho.hessian.server.HessianServlet")
                .allowClass("org.apache.catalina.manager.host.HostManagerServlet")
                .allowClass("org.springframework.web.context.support.HttpRequestHandlerServlet")
                .allowClass("javax.servlet.http.HttpServlet")
                .allowClass("org.eclipse.jetty.servlet.Invoker")
                .allowClass("org.springframework.web.context.support.HttpRequestHandlerServlet")
                .allowClass("org.apache.catalina.manager.JMXProxyServlet")
                .allowClass("org.springframework.http.server.reactive.JettyHttpHandlerAdapter")
                .allowClass("org.springframework.http.server.reactive.TomcatHttpHandlerAdapter")
                .allowClass("org.apache.catalina.servlets.WebdavServlet");

        builder.allowClass("org.springframework.http.server.reactive.ContextPathCompositeHandler")
                .allowClass("org.springframework.web.server.adapter.HttpWebHandlerAdapter")
                .allowClass("org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext.ServerManager");

        builder.allowClass("org.springframework.web.reactive.DispatcherHandler")
                .allowClass("org.springframework.web.server.handler.ExceptionHandlingWebHandler")
                .allowClass("org.springframework.web.server.handler.FilteringWebHandler")
                .allowClass("org.springframework.web.reactive.resource.ResourceWebHandler")
                .allowClass("org.springframework.web.server.handler.WebHandlerDecorator");

        builder.allowClass("io.netty.channel.ChannelInitializer")
                .allowClass("io.lettuce.core.PlainChannelInitializer")
                .allowClass("io.netty.handler.codec.MessageToMessageCodec")
                .allowClass("io.netty.bootstrap.ServerBootstrap.ServerBootstrapAcceptor")
                .allowClass("org.apache.rocketmq.remoting.netty.NettyRemotingClient.NettyClientHandler");

        builder.allowClass("org.apache.dubbo.rpc.proxy.AbstractProxyInvoker")
                .allowClass("com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker")
                .allowClass("io.grpc.internal.ServerImpl$ServerTransportListenerImpl")
                .allowClass("com.weibo.api.motan.transport.ProviderMessageRouter");

        //dbcp
        builder.ignoreClass("org.apache.commons.dbcp2.")
                .allowClass("org.apache.commons.dbcp.BasicDataSource")
                .allowClass("org.apache.commons.dbcp.PoolingDataSource");

        //dbcp2
        builder.ignoreClass("org.apache.commons.dbcp2.")
                .allowClass("org.apache.commons.dbcp2.BasicDataSource")
                .allowClass("org.apache.commons.dbcp2.PoolingDataSource");

        //ehcache
        builder.ignoreClass("net.sf.ehcache.")
                .allowClass("org.ehcache.core.EhcacheBase")
                .allowClass("org.ehcache.core.Ehcache")
                .allowClass("org.ehcache.core.EhcacheWithLoaderWriter")
                .allowClass("org.ehcache.core.PersistentUserManagedEhcache")
                .allowClass("net.sf.ehcache.Cache")
                .allowClass("net.sf.ehcache.constructs.blocking.BlockingCache")
                .allowClass("net.sf.ehcache.Element")
                .allowClass("org.ehcache.impl.internal.util.CheckerUtil")
                .allowClass("org.ehcache.impl.store.BaseStore")
                .allowClass("org.ehcache.impl.internal.store.heap.OnHeapStore")
                .allowClass("net.sf.ehcache.util.PreferredLoaderObjectInputStream");

        //elasticsearch
        builder.ignoreClass("org.elasticsearch.")
                .ignoreClass("org.apache.lucene.")
                .allowClass("org.elasticsearch.client.support.AbstractClient")
                .allowClass("org.elasticsearch.client.RestHighLevelClient")
                .allowClass("org.elasticsearch.action.bulk.BulkRequest")
                .allowClass("org.elasticsearch.client.RestClient");

        //feign
        builder.ignoreClass("feign.")
                .ignoreClass("com.alibaba.cloud.sentinel.")
                .allowClass("feign.ReflectiveFeign$FeignInvocationHandler")
                .allowClass("feign.hystrix.HystrixInvocationHandler")
                .allowClass("com.alibaba.cloud.sentinel.feign.SentinelInvocationHandler");

        //guava
        builder.ignoreClass("com.google.common.")
                .allowClass("com.google.common.collect.ImmutableMap")
                .allowClass("com.google.common.cache.LocalCache$LoadingValueReference")
                .allowClass("com.google.common.cache.LocalCache$LocalManualCache")
                .allowClass("com.google.common.cache.LocalCache$LocalLoadingCache")
                .allowClass("com.google.common.cache.ForwardingCache");

        //google-httpclient
        builder.ignoreClass("com.google.api.client.")
                .allowClass("com.google.api.client.http.HttpRequest");

        //grpc
        builder.ignoreClass("io.grpc.")
                .allowClass("io.grpc.internal.ManagedChannelImpl$RealChannel")
                .allowClass("io.grpc.internal.OobChannel")
                .allowClass("io.grpc.internal.ManagedChannelImpl")
                .allowClass("io.grpc.internal.ForwardingManagedChannel")
                .allowClass("io.grpc.internal.ManagedChannelOrphanWrapper")
                .allowClass("io.grpc.internal.ClientCallImpl")
                .allowClass("io.grpc.internal.ServerImpl$ServerTransportListenerImpl")
                .allowClass("io.grpc.internal.ServerCallImpl$ServerStreamListenerImpl");

        //aliyun-hbase
        builder.ignoreClass("org.apache.hadoop.hbase.")
                .allowClass("com.flipkart.hbaseobjectmapper.WrappedHBTable")
                .allowClass("org.apache.hadoop.hbase.client.HTable")
                .allowClass("org.apache.hadoop.hbase.client.AliHBaseUETable")
                .allowClass("org.apache.hadoop.hbase.client.AliHBaseMultiTable")
                .allowClass("org.apache.hadoop.hbase.ipc.AbstractRpcClient")
                .allowClass("org.hbase.async.HBaseClient")
                .allowClass("org.apache.hadoop.hbase.TableName")
                .allowClass("com.alibaba.lindorm.client.core.LindormWideColumnService")
                .allowClass("com.flipkart.hbaseobjectmapper.WrappedHBTable")
                .allowClass("org.hbase.async.HBaseRpc")
                .allowClass("org.hbase.async.Scanner")
                .allowClass("org.apache.hadoop.hbase.client.AliHBaseUEAdmin");

        //hessian
        builder.ignoreClass("com.caucho.hessian.")
                .ignoreClass("com.caucho.burlap.")
                .allowClass("com.caucho.hessian.client.HessianProxy")
                .allowClass("com.caucho.hessian.client.HessianProxyFactory")
                .allowClass("com.caucho.hessian.server.HessianServlet")
                .allowClass("com.caucho.burlap.server.BurlapServlet")
                .allowClass("org.springframework.remoting.caucho.HessianServiceExporter");

        //hikaricp
        builder.ignoreClass("com.zaxxer.hikari.")
                .allowClass("com.zaxxer.hikari.HikariDataSource")
                .allowClass("com.zaxxer.hikari.HikariConfig")
                .allowClass("com.zaxxer.hikari.pool.HikariPool");

        //httpclient
        builder.ignoreClass("org.apache.commons.httpclient.")
                .allowClass("org.apache.commons.httpclient.HttpClient")
                .allowClass("org.apache.http.impl.client.CloseableHttpClient")
                .allowClass("org.apache.http.impl.nio.client.CloseableHttpAsyncClient")
                .allowClass("org.apache.hc.client5.http.impl.classic.CloseableHttpClient")
                .allowClass("org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient");

        //httpserver
        builder.ignoreClass("com.sun.net.")
                .allowClass("com.sun.net.httpserver.Filter$Chain");

        //hystrix
        builder.ignoreClass("com.netflix.hystrix.")
                .allowClass("com.netflix.hystrix.strategy.concurrency.HystrixContextScheduler$HystrixContextSchedulerWorker")
                .allowClass("com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction")
                .allowClass("rx.internal.operators.OnSubscribeDefer");

        //jdk-http
        builder.ignoreClass("sun.net.www.")
                .allowClass("sun.net.www.protocol.https.AbstractDelegateHttpsURLConnection")
                .allowClass("sun.net.www.http.HttpClient")
                .allowClass("sun.net.www.protocol.http.HttpURLConnection");

        //jersey
        builder.ignoreClass("org.glassfish.jersey.grizzly2.httpserver.")
                .allowClass("org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer");

        //jetcache
        builder.ignoreClass("com.alicp.jetcache.")
                .allowClass("com.alicp.jetcache.embedded.AbstractEmbeddedCache")
                .allowClass("com.alicp.jetcache.external.AbstractExternalCache");

        //jetty
        builder.ignoreClass("org.eclipse.jetty.")
                .allowClass("org.eclipse.jetty.server.handler.HandlerWrapper")
                .allowClass("org.mortbay.jetty.Server")
                .allowClass("org.mortbay.jetty.handler.HandlerWrapper")
                .allowClass("org.eclipse.jetty.server.Request");

        //log4j
        builder.ignoreClass("org.apache.log4j.")
                .allowClass("org.apache.log4j.Category")
                .allowClass("org.apache.log4j.AppenderSkeleton")
                .allowClass("org.apache.logging.log4j.core.config.AppenderControl")
                .allowClass("org.apache.logging.log4j.core.config.LoggerConfig")
                .allowClass("org.apache.logging.log4j.spi.AbstractLogger");

        //logback
        builder.ignoreClass("ch.qos.logback.core.")
                .allowClass("ch.qos.logback.core.spi.AppenderAttachableImpl");

        //mongodb
        builder.ignoreClass("com.mongodb.")
                .allowClass("com.mongodb.async.client.OperationExecutorImpl")
                .allowClass("com.mongodb.client.internal.MongoClientDelegate$DelegateOperationExecutor")
                .allowClass("com.mongodb.Mongo")
                .allowClass("com.mongodb.DBCollection")
                .allowClass("com.mongodb.DBCollectionImpl")
                .allowClass("com.mongodb.MongoCollectionImpl")
                .allowClass("com.mongodb.client.internal.MongoCollectionImpl")
                .allowClass("com.mongodb.operation.AggregateOperationImpl")
                .allowClass("com.mongodb.internal.operation.AggregateOperationImpl");

        //mongodb4
        builder.ignoreClass("com.mongodb.")
                .allowClass("com.mongodb.client.internal.MongoClientDelegate$DelegateOperationExecutor")
                .allowClass("com.mongodb.internal.operation.AggregateOperationImpl");

        //motan
        builder.ignoreClass("com.weibo.api.motan.")
                .allowClass("com.weibo.api.motan.rpc.AbstractReferer")
                .allowClass("com.weibo.api.motan.transport.ProviderMessageRouter");

        //mule
        builder.ignoreClass("org.mule.")
                .allowClass("org.mule.module.http.internal.listener.grizzly.GrizzlyRequestDispatcherFilter");

        //neo4j
        builder.ignoreClass("org.neo4j.")
                .allowClass("org.neo4j.ogm.session.Neo4jSession");

        //netty-time-wheel
        builder.ignoreClass("io.netty.")
                .allowClass("io.netty.util.HashedWheelTimer")
                .allowClass("io.netty.util.HashedWheelTimer$HashedWheelTimeout");

        //okhttp
        builder.ignoreClass("okhttp3.")
                .ignoreClass("com.squareup.okhttp.")
                .allowClass("okhttp3.Request$Builder")
                .allowClass("okhttp3.RealCall")
                .allowClass("okhttp3.RealCall$AsyncCall")
                .allowClass("com.squareup.okhttp.Request$Builder")
                .allowClass("com.squareup.okhttp.Call")
                .allowClass("com.squareup.okhttp.Call$AsyncCall");

        //oscache
        builder.ignoreClass("com.opensymphony.oscache.")
                .allowClass("com.opensymphony.oscache.base.Cache");

        //proxool
        builder.ignoreClass("org.logicalcobwebs.proxool.")
                .allowClass("org.logicalcobwebs.proxool.ProxoolDataSource")
                .allowClass("org.logicalcobwebs.proxool.ProxyStatement")
                .allowClass("org.logicalcobwebs.proxool.ConnectionPoolManager")
                .allowClass("org.logicalcobwebs.proxool.ProxoolDriver");

        //pulsar
        builder.ignoreClass("org.apache.pulsar.")
                .allowClass("org.apache.pulsar.client.impl.ProducerImpl")
                .allowClass("org.apache.pulsar.client.impl.ConsumerBase");

        //rabbitmq
        builder.ignoreClass("com.rabbitmq.")
                .allowClass("org.springframework.amqp.rabbit.core.RabbitAdmin")
                .allowClass("com.rabbitmq.client.impl.AMQConnection")
                .allowClass("com.rabbitmq.client.impl.StrictExceptionHandler")
                .allowClass("com.rabbitmq.client.impl.ConsumerDispatcher")
                .allowClass("com.rabbitmq.client.impl.ChannelN")
                .allowClass("com.rabbitmq.client.impl.recovery.RecoveryAwareChannelN")
                .allowClass("com.rabbitmq.client.impl.ChannelN")
                .allowClass("com.rabbitmq.client.QueueingConsumer")
                .allowClass("org.springframework.amqp.rabbit.core.RabbitAdmin")
                .allowClass("org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer");

        //redis-jedis
        builder.ignoreClass("redis.clients.jedis.")
                .allowClass("redis.clients.jedis.BinaryJedis")
                .allowClass("redis.clients.jedis.Jedis")
                .allowClass("redis.clients.jedis.BinaryJedisCluster")
                .allowClass("redis.clients.jedis.PipelineBase")
                .allowClass("redis.clients.jedis.MultiKeyPipelineBase")
                .allowClass("redis.clients.jedis.Jedis")
                .allowClass("redis.clients.jedis.JedisCluster")
                .allowClass("redis.clients.jedis.Client")
                .allowClass("redis.clients.jedis.Pipeline")
                .allowClass("redis.clients.jedis.PipelineBlock")
                .allowClass("redis.clients.jedis.ShardedJedisPipeline")
                .allowClass("redis.clients.jedis.Transaction")
                .allowClass("redis.clients.jedis.TransactionBlock")
                .allowClass("redis.clients.jedis.util.Pool")
                .allowClass("redis.clients.jedis.JedisSlotBasedConnectionHandler")
                .allowClass("redis.clients.jedis.JedisClusterConnectionHandler")
                .allowClass("redis.clients.jedis.Connection")
                .allowClass("org.springframework.data.redis.connection.jedis.JedisClientUtils");

        //redis-lettuce
        builder.ignoreClass("io.lettuce.core.")
                .allowClass("io.lettuce.core.RedisChannelHandler")
                .allowClass("io.lettuce.core.cluster.ClusterDistributionChannelWriter")
                .allowClass("io.lettuce.core.cluster.ClusterNodeEndpoint")
                .allowClass("io.lettuce.core.protocol.CommandExpiryWriter")
                .allowClass("io.lettuce.core.CommandListenerWriter")
                .allowClass("io.lettuce.core.protocol.DefaultEndpoint")
                .allowClass("io.lettuce.core.masterreplica.MasterReplicaChannelWriter")
                .allowClass("io.lettuce.core.masterslave.MasterSlaveChannelWriter")
                .allowClass("io.lettuce.core.cluster.PubSubClusterEndpoint")
                .allowClass("io.lettuce.core.pubsub.PubSubEndpoint")
                .allowClass("io.lettuce.core.protocol.TransactionalCommand")
                .allowClass("io.lettuce.core.protocol.CommandWrapper")
                .allowClass("io.lettuce.core.CommandListenerWriter$RedisCommandListenerCommand")
                .allowClass("io.lettuce.core.protocol.TimedAsyncCommand")
                .allowClass("io.lettuce.core.protocol.PristineFallbackCommand")
                .allowClass("io.lettuce.core.protocol.AsyncCommand")
                .allowClass("io.lettuce.core.protocol.LatencyMeteredCommand")
                .allowClass("io.lettuce.core.protocol.TracedCommand")
                .allowClass("io.lettuce.core.protocol.DefaultEndpoint$ActivationCommand")
                .allowClass("org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory")
                .allowClass("io.lettuce.core.masterreplica.MasterReplica")
                .allowClass("io.lettuce.core.masterslave.SentinelConnector")
                .allowClass("io.lettuce.core.masterslave.MasterSlaveTopologyRefresh")
                .allowClass("io.lettuce.core.masterslave.MasterSlave")
                .allowClass("io.lettuce.core.RedisClient")
                .allowClass("io.lettuce.core.AbstractRedisClient")
                .allowClass("io.lettuce.core.cluster.RedisClusterClient")
                .allowClass("io.lettuce.core.DefaultConnectionFuture")
                .allowClass("io.lettuce.core.AbstractRedisAsyncCommands")
                .allowClass("io.lettuce.core.AbstractRedisReactiveCommands")
                .allowClass("io.lettuce.core.RedisAsyncCommandsImpl")
                .allowClass("io.lettuce.core.cluster.RedisAdvancedClusterAsyncCommandsImpl")
                .allowClass("io.lettuce.core.cluster.RedisClusterPubSubAsyncCommandsImpl")
                .allowClass("io.lettuce.core.pubsub.RedisPubSubAsyncCommandsImpl")
                .allowClass("io.lettuce.core.cluster.RedisAdvancedClusterReactiveCommandsImpl")
                .allowClass("io.lettuce.core.cluster.RedisClusterPubSubReactiveCommandsImpl")
                .allowClass("io.lettuce.core.pubsub.RedisPubSubReactiveCommandsImpl")
                .allowClass("io.lettuce.core.RedisReactiveCommandsImpl")
                .allowClass("io.lettuce.core.sentinel.RedisSentinelReactiveCommandsImpl")
                .allowClass("io.lettuce.core.AbstractRedisAsyncCommands")
                .allowClass("io.lettuce.core.api.sync.RedisStringCommands")
                .allowClass("io.lettuce.core.AbstractRedisReactiveCommands")
                .allowClass("io.lettuce.core.RedisAsyncCommandsImpl")
                .allowClass("io.lettuce.core.cluster.RedisAdvancedClusterAsyncCommandsImpl")
                .allowClass("io.lettuce.core.cluster.RedisClusterPubSubAsyncCommandsImpl")
                .allowClass("io.lettuce.core.pubsub.RedisPubSubAsyncCommandsImpl")
                .allowClass("io.lettuce.core.cluster.RedisAdvancedClusterReactiveCommandsImpl")
                .allowClass("io.lettuce.core.cluster.RedisClusterPubSubReactiveCommandsImpl")
                .allowClass("io.lettuce.core.pubsub.RedisPubSubReactiveCommandsImpl")
                .allowClass("io.lettuce.core.RedisReactiveCommandsImpl")
                .allowClass("io.lettuce.core.sentinel.RedisSentinelReactiveCommandsImpl")
                .allowClass("io.lettuce.core.AbstractRedisAsyncCommands");

        //redis-redisson
        builder.ignoreClass("org.redisson.")
                .allowClass("org.redisson.RedissonExpirable")
                .allowClass("org.redisson.RedissonMapCache")
                .allowClass("org.redisson.reactive.RedissonMapCacheReactive")
                .allowClass("org.redisson.Redisson")
                .allowClass("org.redisson.RedissonRx")
                .allowClass("org.redisson.RedissonReactive")
                .allowClass("org.redisson.reactive.RedissonBatchReactive")
                .allowClass("org.redisson.RedissonBatch")
                .allowClass("org.redisson.RedissonBatchRx")
                .allowClass("org.redisson.RedissonStream")
                .allowClass("org.redisson.RedissonBinaryStream")
                .allowClass("org.redisson.RedissonGeo")
                .allowClass("org.redisson.RedissonTimeSeries")
                .allowClass("org.redisson.RedissonBucket")
                .allowClass("org.redisson.RedissonRateLimiter")
                .allowClass("org.redisson.RedissonBuckets")
                .allowClass("org.redisson.RedissonHyperLogLog")
                .allowClass("org.redisson.RedissonList")
                .allowClass("org.redisson.RedissonListMultimap")
                .allowClass("org.redisson.RedissonLocalCachedMap")
                .allowClass("org.redisson.RedissonMap")
                .allowClass("org.redisson.RedissonSetMultimap")
                .allowClass("org.redisson.RedissonListMultimapCache")
                .allowClass("org.redisson.RedissonSetCache")
                .allowClass("org.redisson.RedissonMapCache")
                .allowClass("org.redisson.RedissonLock")
                .allowClass("org.redisson.RedissonFairLock")
                .allowClass("org.redisson.RedissonReadWriteLock")
                .allowClass("org.redisson.RedissonScript")
                .allowClass("org.redisson.RedissonExecutorService")
                .allowClass("org.redisson.RedissonRemoteService")
                .allowClass("org.redisson.RedissonSortedSet")
                .allowClass("org.redisson.RedissonScoredSortedSet")
                .allowClass("org.redisson.RedissonLexSortedSet")
                .allowClass("org.redisson.RedissonTopic")
                .allowClass("org.redisson.RedissonPatternTopic")
                .allowClass("org.redisson.RedissonDelayedQueue")
                .allowClass("org.redisson.RedissonQueue")
                .allowClass("org.redisson.RedissonBlockingQueue")
                .allowClass("org.redisson.RedissonDeque")
                .allowClass("org.redisson.RedissonBlockingDeque")
                .allowClass("org.redisson.RedissonAtomicLong")
                .allowClass("org.redisson.RedissonLongAdder")
                .allowClass("org.redisson.RedissonDoubleAdder")
                .allowClass("org.redisson.RedissonAtomicDouble")
                .allowClass("org.redisson.RedissonCountDownLatch")
                .allowClass("org.redisson.RedissonBitSet")
                .allowClass("org.redisson.RedissonSemaphore")
                .allowClass("org.redisson.RedissonPermitExpirableSemaphore")
                .allowClass("org.redisson.RedissonBloomFilter")
                .allowClass("org.redisson.RedissonKeys")
                .allowClass("org.redisson.RedissonBatch")
                .allowClass("org.redisson.RedissonLiveObjectService")
                .allowClass("org.redisson.RedissonPriorityQueue")
                .allowClass("org.redisson.RedissonPriorityBlockingQueue")
                .allowClass("org.redisson.RedissonPriorityBlockingDeque")
                .allowClass("org.redisson.RedissonPriorityDeque")
                .allowClass("org.redisson.RedissonRx")
                .allowClass("org.redisson.rx.RxProxyBuilder$1$1")
                .allowClass("org.redisson.RedissonReactive")
                .allowClass("org.redisson.reactive.RedissonReadWriteLockReactive")
                .allowClass("org.redisson.reactive.RedissonMapCacheReactive")
                .allowClass("org.redisson.reactive.RedissonListReactive")
                .allowClass("org.redisson.reactive.RedissonListMultimapReactive")
                .allowClass("org.redisson.reactive.RedissonSetMultimapReactive")
                .allowClass("org.redisson.reactive.RedissonMapReactive")
                .allowClass("org.redisson.reactive.RedissonSetReactive")
                .allowClass("org.redisson.reactive.RedissonScoredSortedSetReactive")
                .allowClass("org.redisson.reactive.RedissonLexSortedSetReactive")
                .allowClass("org.redisson.reactive.RedissonTopicReactive")
                .allowClass("org.redisson.reactive.RedissonBlockingQueueReactive")
                .allowClass("org.redisson.reactive.RedissonSetCacheReactive")
                .allowClass("org.redisson.reactive.RedissonBatchReactive")
                .allowClass("org.redisson.reactive.RedissonKeysReactive")
                .allowClass("org.redisson.reactive.RedissonTransactionReactive")
                .allowClass("org.redisson.reactive.RedissonBlockingDequeReactive")
                .allowClass("org.redisson.reactive.ReactiveProxyBuilder$1")
                .allowClass("org.redisson.reactive.ReactiveProxyBuilder$1$1")
                .allowClass("org.redisson.command.CommandAsyncService")
                .allowClass("org.redisson.command.CommandBatchService")
                .allowClass("org.redisson.command.CommandSyncService")
                .allowClass("org.redisson.reactive.CommandReactiveService")
                .allowClass("org.redisson.reactive.CommandReactiveBatchService")
                .allowClass("org.redisson.rx.CommandRxBatchService");

        //resin
        builder.ignoreClass("com.caucho.")
                .allowClass("com.caucho.server.http.HttpServletRequestImpl")
                .allowClass("com.caucho.server.dispatch.ServletInvocation");

        //saturn
        builder.ignoreClass("com.vip.saturn.")
                .allowClass("com.vip.saturn.job.java.SaturnJavaJob");

        //shadow-job
        builder.ignoreClass("com.xxl.job.")
                .ignoreClass("com.github.ltsopensource.")
                .ignoreClass("com.dangdang.ddframe.job.")
                .ignoreClass("org.quartz.")
                .allowClass("org.activiti.engine.impl.cmd.AcquireAsyncJobsDueCmd")
                .allowClass("org.quartz.core.QuartzScheduler")
                .allowClass("com.xxl.job.core.executor.XxlJobExecutor")
                .allowClass("org.springframework.context.support.ApplicationContextAwareProcessor")
                .allowClass("org.springframework.context.support.AbstractRefreshableApplicationContext")
                .allowClass("com.github.ltsopensource.spring.tasktracker.JobRunnerHolder")
                .allowClass("com.github.ltsopensource.jobtracker.support.JobReceiver")
                .allowClass("com.github.ltsopensource.tasktracker.TaskTracker")
                .allowClass("org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter")
                .allowClass("org.springframework.aop.framework.ReflectiveMethodInvocation")
                .allowClass("com.xxl.job.core.handler.impl.MethodJobHandler")
                .allowClass("com.dangdang.ddframe.job.executor.JobExecutorFactory")
                .allowClass("org.springframework.scheduling.support.ScheduledMethodRunnable")
                .allowClass("org.quartz.JobExecutionContext")
                .allowClass("org.quartz.core.JobRunShell")
                .allowClass("org.quartz.impl.StdScheduler")
                .allowClass("org.springframework.scheduling.config.ScheduledTaskRegistrar")
                .allowClass("org.springframework.scheduling.concurrent.ReschedulingRunnable")
                .allowClass("com.dangdang.ddframe.job.lite.api.JobScheduler");

        //spring-cache
        builder.ignoreClass("org.springframework.cache.")
                .allowClass("org.springframework.cache.concurrent.ConcurrentMapCache")
                .allowClass("org.springframework.cache.ehcache.EhCacheCache")
                .allowClass("org.springframework.cache.guava.GuavaCache")
                .allowClass("org.springframework.cache.jcache.JCacheCache");

        //spring-cloud-gateway
        builder.ignoreClass("org.springframework.cloud.gateway.")
                .allowClass("org.springframework.cloud.gateway.handler.FilteringWebHandler")
                .allowClass("org.springframework.cloud.gateway.filter.NettyRoutingFilter")
                .allowClass("org.springframework.cloud.gateway.filter.NettyRoutingFilter")
                .allowClass("org.springframework.cloud.gateway.handler.FilteringWebHandler$DefaultGatewayFilterChain")
                .allowClass("org.springframework.beans.factory.support.DefaultListableBeanFactory$DependencyObjectProvider");

        //spring-web
        builder.allowClass("org.springframework.web.server.handler.WebHandlerDecorator")
                .allowClass("org.springframework.web.server.handler.DefaultWebFilterChain")
                .allowClass("org.springframework.web.server.adapter.HttpWebHandlerAdapter");

        //tomcat-dbcp
        builder.ignoreClass("org.apache.tomcat.dbcp.")
                .allowClass("org.apache.tomcat.dbcp.dbcp2.BasicDataSource")
                .allowClass("org.apache.tomcat.dbcp.dbcp2.PoolingDataSource");

        //undertow
        builder.ignoreClass("io.undertow.")
                .allowClass("io.undertow.server.Connectors")
                .allowClass("io.undertow.servlet.spec.HttpServletRequestImpl");

        //webflux
        builder.ignoreClass("org.springframework.web.reactive.")
                .allowClass("org.springframework.web.reactive.DispatcherHandler")
                .allowClass("org.springframework.http.server.reactive.AbstractServerHttpRequest")
                .allowClass("org.springframework.http.ReadOnlyHttpHeaders")
                .allowClass("org.springframework.web.reactive.function.client.ExchangeFunctions$DefaultExchangeFunction");

        //websphere
        builder.ignoreClass("com.ibm.ws.")
                .allowClass("com.ibm.ws.webcontainer.srt.SRTServletRequest")
                .allowClass("com.ibm.ws.webcontainer.WSWebContainer");

        //xmemcached
        builder.ignoreClass("net.rubyeye.xmemcached.")
                .allowClass("net.rubyeye.xmemcached.XMemcachedClient");

        //zuul
        builder.ignoreClass("com.netflix.zuul.")
                .allowClass("com.netflix.zuul.netty.filter.ZuulFilterChainHandler")
                .allowClass("com.netflix.zuul.netty.filter.ZuulFilterChainRunner");

    }

}
