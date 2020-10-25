package com.company;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Test {
    public static void main(String[] args) throws IOException {
        DatagramSocket s = new DatagramSocket(4000);
        DatagramPacket p1 = new DatagramPacket(new byte[0],0, InetAddress.getByName("127.0.0.1"),2000);
        DatagramPacket p2 = new DatagramPacket(new byte[1024],1024);
        s.send(p1);
        s.receive(p2);
    }
}
