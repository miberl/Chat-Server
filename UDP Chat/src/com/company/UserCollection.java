package com.company;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;

/*
    Lista di user con implementazione di metodi utili alla loro gestione.
*/
public class UserCollection {
    LinkedList<User> users;

    public UserCollection() {
        this.users = new LinkedList<User>();
    }

    public boolean addUser(String username, InetAddress address, int port) {
        if (unique(username))
            return users.add(new User(username, address, port));
        return false;
    }

    public boolean addUser(User u) {
        if (unique(u.username))
            return users.add(u);
        return false;
    }

    public boolean removeUser(User u) {
        return users.remove(u);
    }

    public boolean removeUser(InetSocketAddress usersocket) {
        User u;
        if ((u = getUser(usersocket)) != null)
            return users.remove(u);
        return false;
    }

    //Verifica che uno username non sia già presente nella lista
    public boolean unique(String username) {
        for (User u : users) {
            if (u.getUsername().equals(username))
                return false;
        }
        return true;
    }

    public User getUser(InetSocketAddress usersocket) {
        for (User u : users) {
            if (u.getSocket().equals(usersocket)) {
                return u;
            }
        }
        return null;
    }


    public User getUser(String username) {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        return null;
    }

    public ArrayList<String> getUsernames() {
        ArrayList<String> usernamelist = new ArrayList<String>();
        for (User u : users)
            usernamelist.add(u.username);
        return usernamelist;
    }


    public LinkedList<User> getUsers() {
        return users;
    }


}
