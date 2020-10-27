package com.company;

import java.net.InetAddress;
import java.net.InetSocketAddress;
/*
    Classe che rappresenta un utente, identificato dal suo username (univoco nella lista degli utenti attivi) e dal
    socket (coppia IP-porta).
*/

public class User {

    public String username;
    public InetSocketAddress usersocket;

    public User(String username, InetSocketAddress usersocket) {
        this.username = username;
        this.usersocket = usersocket;
    }

    public User(String username, InetAddress addr, int port) {
        this.username = username;
        this.usersocket = new InetSocketAddress(addr, port);
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public InetSocketAddress getSocket() {
        return usersocket;
    }

    public void setSocket(InetSocketAddress usersocket) {
        this.usersocket = usersocket;
    }

}
