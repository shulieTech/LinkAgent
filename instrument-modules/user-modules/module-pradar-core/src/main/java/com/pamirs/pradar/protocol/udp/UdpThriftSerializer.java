package com.pamirs.pradar.protocol.udp;

import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TByteBuffer;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * 适配UDP的Thrift序列化器实现, 非线程安全
 *
 * @author lixs151
 */
public class UdpThriftSerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpThriftSerializer.class.getName());
    // 最大UDP包大小：65535 - TCP header length(20) - UDP header length(8)
    private static final int UDP_MAX_PACKET_LENGTH = 65507;
    private TByteBuffer thriftTransport;
    private TProtocol thriftProtocol;

    // Pinpoint Collector数据包头
    private final byte[] PINPOINT_COLLECTOR_HEADER = {
            (byte) 0xef, // pinpoint collector header signature
            (byte) 0x10, // pinpoint collector header v1
            (byte) 0x02, // the high-order 8 bits of data locator type of pinpoint collector
            (byte) 0x00, // the low-order 8 bits of data locator type of pinpoint collector
    };

    public UdpThriftSerializer() throws TTransportException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(UDP_MAX_PACKET_LENGTH);
        thriftTransport = new TByteBuffer(byteBuffer);
        thriftProtocol = new TCompactProtocol(thriftTransport);
    }

    public byte[] serialize(Object object) throws TException {
        byte[] data = new byte[0];

        // 只序列化Thrift类型的对象
        if (object instanceof TBase) {
            thriftTransport.write(PINPOINT_COLLECTOR_HEADER);
            ((TBase) object).write(thriftProtocol);

            thriftTransport.flip();
            data = thriftTransport.toByteArray();
            thriftTransport.clear();

        } else {
            LOGGER.warn("The object about to be serialized is not a compatible type of Thrift: {}, ignored", object.getClass());
        }

        if (data.length > UDP_MAX_PACKET_LENGTH) {
            LOGGER.warn("The length of serialized data exceeds the maximum UDP packet length: {} > {}, ignored", data.length, UDP_MAX_PACKET_LENGTH);
        }

        return data;
    }
}
