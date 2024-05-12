package com.example.googol.backend;

import java.rmi.*;
import java.util.ArrayList;

public interface ClientRMI extends Remote {

    public ArrayList<String> processString(String input) throws RemoteException;
    boolean isServerActive() throws RemoteException;
}
