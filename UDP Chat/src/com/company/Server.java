package com.company;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/*
    Server: rimane in attesa di connessioni sulla porta 2000.
    Ricevuto un pacchetto lo passa ad un thread che lo gestisce, il server rimane in ascolto per altri pacchetti.
 */

public class Server {
    public static int port;
    public static int buffersize;
    public static DatagramSocket socket;
    public static UserCollection users;

    //Inizializza il server con le impostazioni predefinite
    public static void Initiate() throws SocketException {
        port = 2000;
        buffersize = 1024;
        //Crea il socket in ascolto sulla porta specificata
        socket = new DatagramSocket(port);
        //Lista degli utenti
        users = new UserCollection();
    }


    public static void main(String[] args) throws IOException {
        Initiate();
        DatagramPacket rrq = null;
        while (true) {
            rrq = new DatagramPacket(new byte[buffersize], buffersize);
            //Riceve pacchetto
            socket.receive(rrq);
            //Nuovo thread gestisce il pacchetto ricevuto, il thread terminerà autonomamente
            new Thread(new MyThread(rrq)).start();
        }
    }


}
