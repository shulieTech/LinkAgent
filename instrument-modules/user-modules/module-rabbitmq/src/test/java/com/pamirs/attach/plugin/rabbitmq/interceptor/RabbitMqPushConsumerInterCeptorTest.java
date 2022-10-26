/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.rabbitmq.interceptor;


import com.rabbitmq.client.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

/**
 * @Author: fanxx
 * @Date: 2020/3/31 上午10:48
 * @Description:
 */
public class RabbitMqPushConsumerInterCeptorTest {

    public static final String RABBITMQ_NAMESERVER_ADDR = "127.0.0.1";
    public static final int RABBITMQ_PORT = 5672;
    public static final String RABBITMQ_USERNAME = "guest";
    public static final String RABBITMQ_PASSWORD = "guest";
    public static final String RABBITMQ_HOST = "test_vhost";
    public static final String QUENENAME = "testQueue";
    AMQP.BasicProperties basicProperties = new AMQP.BasicProperties();

//    @Before
    public void setUp() throws Exception {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(RABBITMQ_NAMESERVER_ADDR);
            factory.setPort(RABBITMQ_PORT);
            factory.setUsername(RABBITMQ_USERNAME);
            factory.setPassword(RABBITMQ_PASSWORD);
            factory.setVirtualHost(RABBITMQ_HOST);

            // 创建与RabbitMQ服务器的TCP连接
            Connection connection = factory.newConnection();
            // 创建一个频道
            Channel channel = connection.createChannel();
            // 声明默认的队列
            channel.queueDeclare(QUENENAME, true, false, true, null);
            //basic properties
            AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
            propsBuilder.contentType("text/plain");
            channel.basicPublish("", QUENENAME, basicProperties, "oehll".getBytes("UTF-8"));
        } catch (Exception e) {
            System.out.println(e);

        }
    }

//    @Test
    public void rabbitMqPushConsumer1() throws InterruptedException {
        System.out.println("当前测试方法:rabbitMqPushConsumer1");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(RABBITMQ_NAMESERVER_ADDR);
            factory.setPort(RABBITMQ_PORT);
            factory.setUsername("admin");
            factory.setPassword("shulie@123");
            factory.setVirtualHost(RABBITMQ_HOST);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUENENAME, true, false, true, null);
            // 创建队列消费者
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    String routingKey = envelope.getRoutingKey();
                    System.out.println(" receive data: " + message);
                    System.out.println(" routingKey : " + routingKey);
                }
            };
            // 消息确认机制99
            channel.basicConsume(QUENENAME, true, consumer);

//com.rabbitmq.client.impl.AMQImpl.Basic.Consume

            //channel.abort();

//            RabbitMqPushConsumerInterCeptor rabbitMqPushConsumerInterCeptor = new RabbitMqPushConsumerInterCeptor();
//            ChannelN channelN = mock(ChannelN.class);
//            channelN.setDefaultConsumer(consumer);
//
//            rabbitMqPushConsumerInterCeptor.doBefore(channelN, "basicConsume", new Object[]{QUENENAME, true, "", false, true, new HashMap<Object, Object>(), consumer});
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (TimeoutException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        //Assert.assertEquals("true", Arbiter.applicationAccessStatus.get("RabbitMQ-Pt"));
        countDownLatch.await();
    }
}
