package com.kian.phoneservice;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Kian on 2018/1/30.
 */

public class MainActivity extends AppCompatActivity{

    private static final int CALL_PHONE_REQUEST_CODE = 123;
    private static final String TAG = "MainActivity";
    private Button startButton, confirmButton, stopButton;
    private EditText token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPermission();
        startButton = (Button)findViewById(R.id.startService);
        stopButton= (Button)findViewById(R.id.stopService);
        confirmButton = (Button)findViewById(R.id.confirm);
        token = (EditText)findViewById(R.id.token);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = token.getText().toString();
                if(id.trim().length() != 0){
                    saveToken(id);
                    if(MQTTClient.getInstance() != null) {
                        MQTTClient.getInstance().subscribeToTopic("/dev2app/" + id + "/call");
                    }else{
                        Toast.makeText(MainActivity.this, "请先点击start按钮启动服务", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mBootIntent = new Intent(ContextUtil.getInstance(), PhoneService.class);
                startService(mBootIntent);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mBootIntent = new Intent(ContextUtil.getInstance(), PhoneService.class);
                stopService(mBootIntent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"call phone permission granted.");
            } else {
                // Permission Denied
                Log.d(TAG,"call phone permission not granted.");
            }
        }
    }

    private void saveToken(String token){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("token", token);
        editor.commit();
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm :permissions){
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.
            }else{
                //
            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()){
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }
    }
}
