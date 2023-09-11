package io.shulie.instrument.module.messaging.consumer.module;

/**
 * @author Licey
 * @date 2022/7/27
 */
public abstract class ConsumerConfig {

    /**
     * 返回消费key， 如 topic#groupId
     *
     * 用于匹配是否配置了影子消费者配置
     * @return 必须返回
     */
    public abstract String keyOfConfig();

    /**
     * 返回消费者服务端连接key，如 nameserverList
     *
     * 用于匹配是否配置了影子集群
     * @return 返回null表示不支持影子集群模式
     */
    public abstract String keyOfServer();

    public String key(){
        return keyOfServer() + "[" + keyOfConfig() + "]";
    }

    /**
     * 是否允许一个consumer订阅多个topic
     *
     * @return
     */
    public boolean canSubscribeMultiTopics() {
        return true;
    }

    @Override
    public int hashCode() {
        return key().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConsumerConfig)) {
            return false;
        }
        return ((ConsumerConfig) obj).key().equals(key());
    }
}
