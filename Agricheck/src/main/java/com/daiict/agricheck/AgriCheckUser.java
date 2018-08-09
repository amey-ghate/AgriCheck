package com.daiict.agricheck;

/**
 * Created by amey on 09-08-2018.
 */

class AgriCheckUser {
    private String userName;
    private static final AgriCheckUser ourInstance = new AgriCheckUser();

    static AgriCheckUser getInstance() {
        return ourInstance;
    }

    private AgriCheckUser() {
        userName="Amey";
    }

    public String getUser()
    {
        return userName;
    }
}
