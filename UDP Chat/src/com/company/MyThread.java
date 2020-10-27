package com.company;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/*
    Thread, implementa l'interfaccia Runnable di Java.
    Un thread gestisce ed elabora il pacchetto ricevuto dal mittente,
    al termine dell'operazione (finito il metodo run()) viene terminato.
*/

public class MyThread implements Runnable {

    public DatagramPacket rcvpack;
    public byte data[];
    public int datalength;
    public InetSocketAddress requestsocket;

    public MyThread(DatagramPacket rcvpack) {
        this.rcvpack = rcvpack;
    }

    //Scrive su file di log gli eventi del server.
    public void logAction(String action) throws IOException {
        System.out.println(action);
        //Appende ogni riga alla fine del file
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

        //Se l'utente risulta registrato viene richiamato il metodo handler
        if (registeredUser()) {
            try {
                //Gestisce il pacchetto in base al codice presente nell'header
                handler();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            try {
                //Comunica al mittente che sta compiendo un'operazione non ammessa (non è ancora registrato)
                sendError("ERROR: you aren't a registered user!");
            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    //Restituisce la lunghezza dei dati del pacchetto ricevuto (specificata nei byte 1 e 2)
    public int getDataLength() {
        int length = 0;
        length += ((int) data[1]) * 256;
        length += (int) data[2];
        return length;
    }

    //Se l'utente è presente nella lista, oppure se sta effettuando il login
    public boolean registeredUser() {
        return (Server.users.getUser(requestsocket) != null || (int) data[0] == 11);
    }

    public boolean login() throws IOException {
        String username = new String(data, 3, datalength);
        //Specifiche tutti i caratteri sono validi (inclusi speciali), lunghezza tra 6 e 15
        if (username.length() >= 6 && username.length() <= 15) {
            if (Server.users.addUser(username, rcvpack.getAddress(), rcvpack.getPort())) {
                //Se l'aggiunta alla lista è avvenuta con successo viene inviato un ACK
                sendAcknowledgement();
                logAction(username + " logged in");
                return true;
            } else
                //Nel caso lo username risultasse duplicato, l'errore viene comunicato al mittente
                sendError("ERROR: Not logged in, username isn't unique.");
        } else
            //Nel caso la lunghezza risultasse errata, l'errore viene comunicato al mittente
            sendError("ERROR: Not logged in, username length is invalid.");
        return false;
    }

    //Logout di un utente, viene rimosso dalla lista.
    //Per accedere dovrà registrarsi nuovamente
    public boolean logout() throws IOException {
        User u = Server.users.getUser(requestsocket);
        if (Server.users.removeUser(u)) {
            //Viene inviato un ACK di avvenuto logout
            sendAcknowledgement();
            logAction(u.username + " logged out");
            return true;
        }
        return false;
    }

    //Messaggio pubblico, visualizzato da tutti gli utenti online
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

    //Messaggio privato tra utenti online
    public void privateMessage() throws IOException {
        String username = Server.users.getUser(requestsocket).getUsername();
        //Salta byte intestazione
        int pos = 3;
        //Posizione del byte 0 separatore
        while (data[pos++] != (byte) 0) ;

        //Ricava username destinatario
        String destinationuser = new String(data, 3, pos - 4);
        byte unicastmessage[] = new byte[username.length() + (datalength - destinationuser.length())];
        System.arraycopy(username.getBytes(), 0, unicastmessage, 0, username.length());
        System.arraycopy(data, 3 + destinationuser.length(), unicastmessage, username.length(), datalength - destinationuser.length());
        System.out.println("Destination user: '" + destinationuser + "'");

        //Controlla che il destinatario del messaggio sia presente nella lista utenti
        for (String user : Server.users.getUsernames()) {
            if (user.equals(destinationuser)) {
                //Invia messaggio privato
                sendPacket(23, unicastmessage, Server.users.getUser(destinationuser));
                logAction(username + " has sent a private message to " + destinationuser + ": " + new String(data, pos, datalength));
                /*
                    Importante: Secondo il protocollo questi messaggi non contengono alcun riferimento identificativo
                    (es codice univoco messaggio), sono quindi generici. Il client non avrà la certezza di quale messaggio
                    è/non è stato recapitato.
                */

                //Utente trovato -> notifica ACK al mittente
                sendAcknowledgement();
                return;
            }
        }

        //Non c'è l'utente richiesto -> notifica messaggio d'errore specificando il nome del destinatario errato.
        sendError("ERROR: no user with username: " + destinationuser);
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

    //Invia lista degli utenti attivi
    public void listActiveUsers() throws IOException {
        //Username del mittente
        String username = Server.users.getUser(requestsocket).getUsername();
        ArrayList<Byte> userlist = new ArrayList<Byte>();

        //Aggiunge username alla lista, con byte 0 separatore
        for (String user : Server.users.getUsernames()) {
            for (byte b : user.getBytes())
                userlist.add(b);
            userlist.add((byte) 0);
        }
        //Rimuove ultimo 0
        userlist.remove(userlist.size() - 1);

        byte usernames[] = new byte[userlist.size()];
        for (int i = 0; i < userlist.size(); i++)
            usernames[i] = userlist.get(i);
        sendPacket(43, usernames);
        logAction(username + " has requested the user list");
    }

    //Gestore: richiama metodo corretto in base al codice header
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

    //Invia OK generico (cod=0) alla sorgente del messaggio es: login avvenuto con successo
    public void sendAcknowledgement() throws IOException {
        sendPacket(0, new byte[0]);
    }


    //Invia stringa errore con cod=1 alla sorgente del messaggio
    public void sendError(String errormex) throws IOException {
        sendPacket(1, errormex.getBytes());
    }

    //Invio pacchetto già formato a user specifico
    public void sendPacket(byte send[], User u) throws IOException {
        DatagramPacket sendpacket = new DatagramPacket(send, send.length, u.usersocket);
        Server.socket.send(sendpacket);
    }

    //Invio pacchetto con aggiunta header a user specifico
    public void sendPacket(int code, byte content[], User u) throws IOException {
        byte send[] = addHeader(code, content);
        DatagramPacket sendpacket = new DatagramPacket(send, send.length, u.usersocket);
        Server.socket.send(sendpacket);
    }

    //Invio pacchetto al mittente
    public void sendPacket(int code, byte content[]) throws IOException {
        byte send[] = addHeader(code, content);
        DatagramPacket sendpacket = new DatagramPacket(send, send.length, rcvpack.getAddress(), rcvpack.getPort());
        Server.socket.send(sendpacket);
    }


    //Invio pacchetto a tutti gli utenti connessi
    public void sendPacket(int code, byte content[], boolean group) throws IOException {
        byte send[] = addHeader(code, content);
        if (group) {
            //Invia a tutti gli utenti della lista
            for (User u : Server.users.getUsers()) {
                sendPacket(send, u);
            }
        }
    }

    //Aggiunge header d'intestazione (cod e lunghezza) ai dati da inviare
    public byte[] addHeader(int code, byte content[]) {
        byte sendata[] = new byte[3 + content.length];
        sendata[0] = (byte) (code);
        sendata[1] = (byte) (content.length / 256);
        sendata[2] = (byte) (content.length % 256);
        System.arraycopy(content, 0, sendata, 3, content.length);
        return sendata;
    }

}
