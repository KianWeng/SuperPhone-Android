package com.kian.phoneservice;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * Created by Kian on 2018/1/30.
 */

public class PhoneService extends Service implements MQTTClient.IMessageListener{

    private static final String TAG = "PhoneService";
    private MQTTClient mqttClient;

    @Override
    public void onCreate(){
        super.onCreate();
        mqttClient = MQTTClient.getInstance();
        mqttClient.setMessageListener(this);
        Log.i(TAG,"start phone service");
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onDestroy(){
        Log.i(TAG,"stop phone service");
        mqttClient.release();
    }
    /**
     * 根据联系人获取手机号码
     * @param name
     */
    public String getPhoneNumber(String name){
        Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()){
            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            if(name.equals(contactName)){
                Cursor phone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId, null, null);
                if(phone.moveToNext()){
                    String phoneNumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    Log.d(TAG,"获取到电话号码：" + phoneNumber);
                    return phoneNumber;
                }
            }
        }
        return null;
    }

    /**
     * 拨打电话
     * @param phoneNum
     */
    public boolean callPhone(String phoneNum){
        Intent intent = new Intent(Intent.ACTION_CALL);
        Uri data = Uri.parse("tel:" + phoneNum);
        intent.setData(data);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent);
            return true;
        }else{
            return false;
        }
    }

    @Override
    public void onMessageArrived(String topic, String msg){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String token = sp.getString("token", "");
        String myTopic = "/dev2app/" + token + "/call";
        Log.i(TAG,"myTopic is " + myTopic + " topic is " + topic);
        if(topic.equals(myTopic)){
            Log.i(TAG,"Start get phone number.");
            String phoneNum = getPhoneNumber(msg);
            Log.i(TAG,"phone number is " + phoneNum);
            if(phoneNum != null) {
                if(callPhone(phoneNum)) {
                    mqttClient.publish("/app2dev/" + token + "/callingstatus", "SUCCESS");
                }else {
                    mqttClient.publish("/app2dev/" + token + "/callingstatus", "FAIL");
                }
            }else {
                mqttClient.publish("/app2dev/" + token + "/callingstatus", "NO CONTACT");
            }
        }
    }
}
