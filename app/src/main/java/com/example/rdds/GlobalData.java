package com.example.rdds;

import android.app.Application;

public class GlobalData extends Application {
    private static GlobalData instance;
    //private String HTTP_Address = "http://192.168.137.1:5000";
    //private String HTTP_Address = "http://10.31.33.89:5000";
    private String HTTP_Address = "http://192.168.31.22:5000";
    //private String HTTP_Address = "http:192.168.31.164:5000";


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static GlobalData getInstance() {
        return instance;
    }

    public String getHTTP_Address() {
        return HTTP_Address;
    }

    public void setHTTP_Address(String value) {
        this.HTTP_Address = value;
    }
}