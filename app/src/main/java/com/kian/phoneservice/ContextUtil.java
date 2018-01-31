package com.kian.phoneservice;

import android.app.Application;

/**
 * Created by Kian on 2018/1/30.
 */

public class ContextUtil extends Application{
    private static ContextUtil instance;

    public static ContextUtil getInstance(){
        return instance;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        instance = this;
    }
}
