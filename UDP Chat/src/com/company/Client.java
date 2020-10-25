package com.company;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Client {
    public static DatagramSocket s;

    public static void main(String[] args) throws SocketException {
        s = new DatagramSocket();

    }



    public static void sendpkt(int cod, byte mex[], InetAddress add, int port) {
        byte b[] = new byte[3 + mex.length];
        b[0] = (byte) (cod);
        b[1] = (byte) (mex.length / 256);
        b[2] = (byte) (mex.length % 256);
        System.arraycopy(mex, 0, b, 3, mex.length);
        DatagramPacket p = new DatagramPacket(b, mex.length + 3, add, port);
        try {
            s.send(p);
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }
}
