package com.company;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MyThread implements Runnable {

    public DatagramPacket rcvpack;
    public byte data[];
    public int datalength;
    public InetSocketAddress requestsocket;

    public MyThread(DatagramPacket rcvpack) {
        this.rcvpack = rcvpack;
    }

    public void logAction(String action) throws IOException {
        System.out.println(action);
        BufferedWriter fr = new BufferedWriter(new FileWriter("log.txt", true));
        fr.write(action);
        fr.newLine();
        fr.close();
    }

    @Override
    public void run() {
        this.data = rcvpack.getData();
        this.datalength = getDataLength();
        this.requestsocket = new InetSocketAddress(rcvpack.getAddress(), rcvpack.getPort());

        if (registeredUser()) {
            try {
                handler();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            try {
                sendError("ERROR: you aren't a registered user!");
            } catch (IOException e) {
                e.printStackTrace();
            }

    }


    public int getDataLength() {
        int length = 0;
        length += ((int) data[1]) * 256;
        length += (int) data[2];
        return length;
    }

    public boolean registeredUser() {
        if (Server.users.getUser(requestsocket) == null)
            System.out.println("Error: user isn't found");
        return ((int) data[0] == 11 || Server.users.getUser(requestsocket) != null);
    }

    public boolean login() throws IOException {
        String username = new String(data, 3, datalength);
        if (username.length() >= 6 && username.length() <= 15) {
            if (Server.users.addUser(username, rcvpack.getAddress(), rcvpack.getPort())) {
                sendAcknowledgement();
                logAction(username + " logged in");
                return true;
            } else
                sendError("ERROR: Not logged in, username isn't unique.");
        } else
            sendError("ERROR: Not logged in, username length is invalid.");
        return false;
    }

    public boolean logout() throws IOException {
        User u = Server.users.getUser(requestsocket);
        if (Server.users.removeUser(u)) {
            sendAcknowledgement();
            logAction(u.username + " logged out");
            return true;
        }
        return false;
    }

    public void groupMessage() throws IOException {
        //Username mittente
        String username = Server.users.getUser(requestsocket).getUsername();
        byte broadmessage[] = new byte[datalength + username.length() + 1];
        //Copia lo username del mittente
        System.arraycopy(username.getBytes(), 0, broadmessage, 0, username.length());
        //0, delimita mittente da messaggio
        broadmessage[username.length()] = 0;
        //Contenuto del messaggio
        System.arraycopy(data, 3, broadmessage, username.length() + 1, datalength);
        sendPacket(21, broadmessage, true);
        logAction(username + " has sent a public message: " + new String(data, 3, datalength));
    }

    public void privateMessage() throws IOException {
        String username = Server.users.getUser(requestsocket).getUsername();
        int pos = 3;
        while (data[pos++] != (byte)0) ;
        String destinationuser = new String(data, 3, pos-4);
        byte unicastmessage[] = new byte[username.length() + (datalength - destinationuser.length())];
        System.arraycopy(username.getBytes(), 0, unicastmessage, 0, username.length());
        System.arraycopy(data, 3 + destinationuser.length(), unicastmessage, username.length(), datalength - destinationuser.length());
        System.out.println("Destination user: '"+destinationuser+"'");
        for (String user : Server.users.getUsernames()){
            if (user.equals(destinationuser)){
                sendPacket(23, unicastmessage, Server.users.getUser(destinationuser));
                logAction(username + " has sent a private message to " + destinationuser + ": " + new String(data, pos, datalength));
                return;
            }
        }
        System.out.println("ERR");


    }

    //Restituisce la data e l'ora corrente del server
    public void info() throws IOException {
        String username = Server.users.getUser(requestsocket).getUsername();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date d = new Date();
        String info = "ChatServer V_2020.0" + formatter.format(d);
        sendPacket(41, info.getBytes());
        logAction(username + " has requested server information");
    }

    public void listActiveUsers() throws IOException {
        String username = Server.users.getUser(requestsocket).getUsername();
        ArrayList<Byte> userlist = new ArrayList<Byte>();
        for (String user : Server.users.getUsernames()) {
            for (byte b : user.getBytes())
                userlist.add(b);
            userlist.add((byte) 0);
            System.out.println(user);
        }
        //Rimuove ultimo 0
        userlist.remove(userlist.size() - 1);

        byte usernames[] = new byte[userlist.size()];
        for (int i = 0; i < userlist.size(); i++)
            usernames[i] = userlist.get(i);
        sendPacket(43, usernames);
        logAction(username + " has requested the user list");
    }

    public void handler() throws IOException {
        int code = data[0];
        switch (code) {
            //Richiesta login
            case 11:
                login();
                break;
            //Richiesta logout
            case 12:
                logout();
                break;
            //Messaggio pubblico
            case 20:
                groupMessage();
                break;
            //Messaggio privato
            case 22:
                privateMessage();
                break;
            //Info request
            case 40:
                info();
                break;
            //User list
            case 42:
                listActiveUsers();
                break;
            //Errore nella formattazione della richiesta
            default:
                sendError("ERROR: Request formatted incorrectly.");
                break;

        }
    }

    //Invia l'OK alla sorgentee del messaggio
    public void sendAcknowledgement() throws IOException {
        sendPacket(0, new byte[0]);
    }

    public void sendError(String errormex) throws IOException {
        sendPacket(1, errormex.getBytes());
    }


    public void sendPacket(byte send[], User u) throws IOException {
        DatagramPacket sendpacket = new DatagramPacket(send, send.length, u.usersocket);
        Server.socket.send(sendpacket);
    }

    public void sendPacket(int code, byte content[], User u) throws IOException {
        byte send[] = addHeader(code, content);
        DatagramPacket sendpacket = new DatagramPacket(send, send.length, u.usersocket);
        Server.socket.send(sendpacket);
    }


    public void sendPacket(int code, byte content[]) throws IOException {
        byte send[] = addHeader(code, content);
        DatagramPacket sendpacket = new DatagramPacket(send, send.length, rcvpack.getAddress(), rcvpack.getPort());
        Server.socket.send(sendpacket);
    }


    //Pacchetto con possibilità messaggio di gruppo
    public void sendPacket(int code, byte content[], boolean group) throws IOException {
        byte send[] = addHeader(code, content);
        if (group) {
            //Invia a tutti gli utenti della lista
            for (User u : Server.users.getUsers()) {
                sendPacket(send, u);
            }
        }
    }


    public byte[] addHeader(int code, byte content[]) {
        byte sendata[] = new byte[3 + content.length];
        sendata[0] = (byte) (code);
        sendata[1] = (byte) (content.length / 256);
        sendata[2] = (byte) (content.length % 256);
        System.arraycopy(content, 0, sendata, 3, content.length);
        return sendata;
    }

}
