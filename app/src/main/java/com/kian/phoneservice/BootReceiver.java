package com.kian.phoneservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Kian on 2018/1/30.
 */

public class BootReceiver extends BroadcastReceiver{
    private static final String TAG = "BootReceiver";
    @Override
    public void onReceive(Context context, Intent intent){
        Log.i(TAG,"Receive boot completed intent.");
        Intent mBootIntent = new Intent(context,PhoneService.class);
        context.startService(mBootIntent);
    }
}
