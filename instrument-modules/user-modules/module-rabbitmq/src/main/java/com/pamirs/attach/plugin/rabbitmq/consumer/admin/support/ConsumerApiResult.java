package com.pamirs.attach.plugin.rabbitmq.consumer.admin.support;

import com.pamirs.attach.plugin.rabbitmq.consumer.ConsumerMetaData;
import com.rabbitmq.client.Consumer;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/17 5:11 下午
 */
public class ConsumerApiResult {
    //json example
    /*{
        "arguments": {},
        "prefetch_count": 200,
        "ack_required": true,
        "exclusive": false,
        "consumer_tag": "consumer10",
        "queue": {
            "name": "queue1",
            "vhost": "/"
        },
        "channel_details": {
        "name": "127.0.0.1:62756 -> 127.0.0.1:5672 (1)",
            "number": 1,
            "user": "root",
            "connection_name": "127.0.0.1:62756 -> 192.168.1.95:5672",
            "peer_port": 62756,
            "peer_host": "127.0.0.1"
        }
    }*/

    private int prefetch_count;

    private boolean ack_required;

    private boolean exclusive;

    private String consumer_tag;

    private Queue queue;

    public int getPrefetch_count() {
        return prefetch_count;
    }

    public void setPrefetch_count(int prefetch_count) {
        this.prefetch_count = prefetch_count;
    }

    public boolean isAck_required() {
        return ack_required;
    }

    public void setAck_required(boolean ack_required) {
        this.ack_required = ack_required;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public String getConsumer_tag() {
        return consumer_tag;
    }

    public void setConsumer_tag(String consumer_tag) {
        this.consumer_tag = consumer_tag;
    }

    public Queue getQueue() {
        return queue;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    public static class Queue {

        private String name;

        private String vhost;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVhost() {
            return vhost;
        }

        public void setVhost(String vhost) {
            this.vhost = vhost;
        }
    }

    public static class ChannelDetail {

        private String name;

        private String connection_name;

        private int number;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getConnection_name() {
            return connection_name;
        }

        public void setConnection_name(String connection_name) {
            this.connection_name = connection_name;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }

    public ConsumerMetaData toConsumerMetaData(Consumer consumer) {
        return new ConsumerMetaData(
            this.queue.getName(),
            this.consumer_tag,
            consumer,
            this.exclusive,
            !this.ack_required,
            this.prefetch_count
        );
    }

}
