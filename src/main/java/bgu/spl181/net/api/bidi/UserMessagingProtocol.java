package bgu.spl181.net.api.bidi;

import bgu.spl181.net.api.json.User;
import bgu.spl181.net.api.json.UsersList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class UserMessagingProtocol<T> implements BidiMessagingProtocol<T>, Supplier<BidiMessagingProtocol<T>> {

    protected UsersList usersList;
    protected ArrayList<User> users;
    protected ConcurrentHashMap<Integer, String> loggedIn;
    protected ConcurrentHashMap<String, String> passwords;
    protected ConcurrentHashMap<String, User> usersInfo;

    protected ConnectionsImpl connections;
    protected Integer connectionId;

    protected Object lock = new Object();

    public ConnectionsImpl getConnections() {
        return connections;
    }

    public Integer getConnectionId() {
        return connectionId;
    }

    public UserMessagingProtocol(UsersList users) {
        this.usersList = users;
        this.users = (ArrayList) users.getUsers();
        this.passwords = new ConcurrentHashMap<>();
        this.usersInfo = new ConcurrentHashMap<>();

        for (User user : this.users) {
            passwords.put(user.getUsername(), user.getPassword());
            usersInfo.put(user.getUsername(), user);
        }
    }

    @Override
    public void start(int connectionId, Connections connections) {
        this.connections = (ConnectionsImpl) connections;
        this.connectionId = connectionId;
        this.loggedIn = this.connections.getLoggedIn();
    }

    private void login(String str) {
        boolean error = false;
        int pos2 = str.indexOf(" ");
        String username = str.substring(0, pos2);
        String password = str.substring(pos2 + 1);

        if ((passwords.get(username) == null)) {//not in user list
            error = true;
        }
        if (loggedIn.get(connectionId) != null && !error) { // case client id is already logged in
            error = true;
        }
        if (loggedIn.containsValue(username) && !error) { // case other username is already logged in//
            error = true;
        }
        if ((passwords.get(username) != null) && !error) {
            if (!(passwords.get(username).equals(password))) { // wrong password case sensitive
                error = true;
            }
        }

        if (!error) {
            loggedIn.put(connectionId, username);
            connections.send(connectionId, "ACK login succeeded");
        } else {
            connections.send(connectionId, "ERROR login failed");
        }

    }

    protected void register(String str) throws IOException {
        boolean error = false;
        int pos2 = str.indexOf(" ");
        //3 missing username / password
        if (pos2 == -1) {
            connections.send(connectionId, "ERROR register failed");
            return;
        }

        String username = str.substring(0, pos2);
        str = str.substring(pos2 + 1);
        String password;
        User newUser;

        //1
        if (!error && loggedIn.containsKey(connectionId)) {
            error = true;
        }
        //2
        if (!error && passwords.containsKey(username)) {
            error = true;
        }

        if (error) {
            connections.send(connectionId, "ERROR register failed");
            return;
        }

        int pos3 = str.indexOf(" ");
        if (pos3 == -1) { //no country
            password = str;
            newUser = new User(username, password);

        } else {//yes country

            password = str.substring(0, pos3);
            str = str.substring(pos3 + 1);

            //4
            if (!(str.toLowerCase()).contains("country=\"")) {
                connections.send(connectionId, "ERROR register failed");
                return;
            }

            int pos4 = str.indexOf("\"");
            String country = str.substring(pos4 + 1, str.length() - 1);

            newUser = new User(username, password);
            newUser.setCountry(country);
        }

        if (!error) {

            synchronized (lock){
                users.add(newUser);
                passwords.put(newUser.getUsername(), newUser.getPassword());
                usersInfo.put(newUser.getUsername(), newUser);
                updateUsersJSON();
            }

            connections.send(connectionId, "ACK registration succeeded");

            //System.out.println(newUser);
        }

    }

    protected synchronized void updateUsersJSON() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter writer = new FileWriter("Database/Users.json");
        writer.write(gson.toJson(usersList));
        writer.close();
    }

    private void requestUser(String str) throws IOException {
        int pos1 = str.indexOf(" ");
        String result;
        if (pos1 == -1) {
            result = str;
        } else {
            result = str.substring(0, pos1);
        }

        if (!loggedIn.containsKey(connectionId)) {
            connections.send(connectionId, "ERROR request " + result + " failed");
        } else {
            request(str);

        }
    }

    protected void request(String str) throws IOException {
    }

    private void signout() {
        boolean error = false;
        if (!(loggedIn.containsKey(connectionId))) {
            connections.send(connectionId, "ERROR signout failed");

        } else {

            loggedIn.remove(connectionId);

            connections.send(connectionId, "ACK signout succeeded");
            connections.getAllConnection().remove(connectionId);
        }
    }

    @Override
    public void process(Object message) throws IOException {

        String str = (String) message;

        if (str.toLowerCase().equals("signout")) {
            signout();
        } else {
            int pos1 = str.indexOf(" ");
            String first = str.substring(0, pos1);
            str = str.substring(pos1 + 1);

            if (first.toLowerCase().equals("login")) {
                login(str);
            } else if (first.toLowerCase().equals("register")) {
                register(str);
            } else if (first.toLowerCase().equals("request")) {
                requestUser(str);
            }
        }
    }

    public UsersList getUsersList() {
        return usersList;
    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }

    @Override
    public BidiMessagingProtocol<T> get() {
        UserMessagingProtocol mmp = new UserMessagingProtocol(usersList);
        mmp.start(connectionId, connections);
        return mmp;
    }
}
