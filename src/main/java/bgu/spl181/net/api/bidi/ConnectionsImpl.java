package bgu.spl181.net.api.bidi;

import bgu.spl181.net.srv.bidi.ConnectionHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsImpl<T> implements Connections<T>{

    private AtomicInteger connectionId = new AtomicInteger(0);
    private ConcurrentHashMap<Integer, ConnectionHandler<T>> connections =new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, String> loggedIn =new ConcurrentHashMap<>();

    public ConcurrentHashMap<Integer, String> getLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(ConcurrentHashMap<Integer, String> loggedIn) {
        this.loggedIn = loggedIn;
    }

    public int getNewConnectionId(){

        connectionId.getAndIncrement();
        int newConnection = connectionId.get();
        return newConnection;
    }

    public boolean send(int connectionId, T msg) {

        if (connections.containsKey(connectionId)) {
            connections.get(connectionId).send(msg);
            return true;
        }
        else {
            return false;
        }
    }

    public ConcurrentHashMap<Integer, ConnectionHandler<T>> getAllConnection() {
        return connections;
    }

    public void broadcast(T msg) {
        loggedIn.forEach( (k,v) -> connections.get(k).send(msg));
    }

    public void disconnect(int connectionId) throws IOException {
        connections.get(connectionId).close();
        connections.remove(connectionId);
    }

    public void add(Integer id, ConnectionHandler<T> connectionHandler) {
        connections.put(id, connectionHandler);

    }


}
