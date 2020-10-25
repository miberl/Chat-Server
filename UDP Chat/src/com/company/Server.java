package com.company;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server {
    public static int port;
    public static int buffersize;
    public static DatagramSocket socket;
    public static UserCollection users;

    public static void Initiate() throws SocketException {
        port = 2000;
        buffersize = 1024;
        socket = new DatagramSocket(port);
        users = new UserCollection();
    }


    public static void main(String[] args) throws IOException {
        Initiate();

        while (true) {
            DatagramPacket rrq = new DatagramPacket(new byte[buffersize], buffersize);
            //Riceve pacchetto
            socket.receive(rrq);
            //Nuovo thread gestisce il pacchetto ricevuto
            new Thread(new MyThread(rrq)).start();
        }
    }






}
