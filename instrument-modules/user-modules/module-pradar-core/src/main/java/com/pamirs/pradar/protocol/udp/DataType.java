package com.pamirs.pradar.protocol.udp;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/12 3:59 下午
 */
public class DataType {
    /**
     * trace日志
     */
    public final static byte TRACE_LOG = 1;
    /**
     * metrics日志
     */
    public final static byte METRICS_LOG = 2;
    /**
     * 监控日志
     */
    public final static byte MONITOR_LOG = 3;
    /**
     * agent日志
     */
    public final static byte AGENT_LOG = 4;
}
