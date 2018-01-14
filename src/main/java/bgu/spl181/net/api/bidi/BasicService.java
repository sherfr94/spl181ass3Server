package bgu.spl181.net.api.bidi;

import bgu.spl181.net.api.json.UsersList;

public class BasicService{


    private UsersList usersList;

    public BasicService(UsersList usersList) {
        this.usersList = usersList;
    }

    public UsersList getUsersList() {
        return usersList;
    }

    public void setUsersList(UsersList usersList) {
        this.usersList = usersList;
    }

    String register(int connectionId){return "";}

    String login(int connectionId){return "";}

    String signout(int connectionId){return "";}




}
