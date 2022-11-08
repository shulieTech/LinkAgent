package com.pamirs.pradar.protocol.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * UDP发送实现类，非现成安全
 */
public class UdpTransport {
    private DatagramPacket udpPacket;
    private DatagramSocket udpSocket;

    public UdpTransport(InetSocketAddress socketAddress) throws SocketException {
        udpSocket = new DatagramSocket();
        udpSocket.connect(socketAddress);
        udpPacket = new DatagramPacket(new byte[0], 0, socketAddress);
    }

    public void send(byte[] data) throws IOException {
        udpPacket.setData(data);
        udpSocket.send(udpPacket);
    }

    public void close() {
        udpSocket.close();
    }

    public boolean isClosed() {
        return udpSocket.isClosed();
    }
}
