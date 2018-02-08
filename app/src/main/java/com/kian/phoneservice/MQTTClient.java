package com.kian.phoneservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by kian on 2018/1/30.
 */

public class MQTTClient {
    private static final String TAG = "MQTTService";

    public boolean isConnect = false;
    private IMessageListener messageListener;
    private static MqttAndroidClient client;
    private MqttConnectOptions conOpt;
    private static MQTTClient instance;
    private String host = "tcp://45.32.7.217:1883";
    private String userName = "admin";
    private String passWord = "password";
    private static String myTopic = "android/common";
    private String clientId = "Android_";

    private MQTTClient(){
        init();
    }

    public static MQTTClient getInstance(){
        synchronized (MQTTClient.class){
            if(instance == null){
                instance = new MQTTClient();
            }
        }
        return instance;
    }

    /**
     * MQTT client 推送消息
     * @param topic
     * @param msg
     */
    public void publish(String topic,String msg){
        publish(topic, msg, 0, false);
    }

    public void publish(String topic,String msg, Integer qos, Boolean retained){
        Log.i(TAG,"publish message: " + msg + " to topic: " + topic);
        try {
            client.publish(topic, msg.getBytes(), qos.intValue(), retained.booleanValue());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * MQTT client订阅主题
     * @param topic
     */
    public void subscribeToTopic(String topic){
        Log.i(TAG,"subscribe to topic: " + topic);
        try {
            client.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG,"Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG,"Failed to subscribe");
                }
            });
        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    /**
     * 初始化MQTT client
     */
    private void init() {
        // 服务器地址（协议+地址+端口号）
        String uri = host;
        clientId = clientId + getIMEI(ContextUtil.getInstance());
        client = new MqttAndroidClient(ContextUtil.getInstance(), uri, clientId);
        // 设置MQTT监听并且接受消息
        client.setCallback(mqttCallback);

        conOpt = new MqttConnectOptions();
        // 清除缓存
        conOpt.setCleanSession(true);
        // 设置超时时间，单位：秒
        conOpt.setConnectionTimeout(10);
        // 心跳包发送间隔，单位：秒
        conOpt.setKeepAliveInterval(20);
        //设置自动重连
        conOpt.setAutomaticReconnect(true);
        // 用户名
        conOpt.setUserName(userName);
        // 密码
        conOpt.setPassword(passWord.toCharArray());

        // last will message
        boolean doConnect = true;
        String message = "{\"terminal_uid\":\"" + clientId + "\"}";
        String topic = myTopic;
        Integer qos = 0;
        Boolean retained = false;
        if ((!message.equals("")) || (!topic.equals(""))) {
            // 最后的遗嘱
            try {
                conOpt.setWill(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
            } catch (Exception e) {
                Log.i(TAG, "Exception Occured", e);
                doConnect = false;
                iMqttActionListener.onFailure(null, e);
            }
        }

        if (doConnect) {
            doClientConnection();
        }

    }

    /** 连接MQTT服务器 */
    private void doClientConnection() {
        if (!client.isConnected() && isConnectIsNomarl()) {
            Log.i(TAG,"Connect MQTT server.");
            try {
                client.connect(conOpt, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

    }

    // MQTT是否连接成功
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "连接成功 ");
            // 订阅myTopic话题
            subscribeToTopic(myTopic);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ContextUtil.getInstance());
            String token = sp.getString("token", "");
            Log.d(TAG,"Get default token:" + token);
            if(token.trim().length() != 0){
                subscribeToTopic("/dev2app/" + token + "/call");
            }
            isConnect = true;
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            arg1.printStackTrace();
            // 连接失败，重连
            Log.i(TAG,"Failed to connect to: " + host);
        }
    };

    // MQTT监听并且接受消息
    private MqttCallbackExtended mqttCallback = new MqttCallbackExtended() {

        @Override
        public void connectComplete(boolean reconnect, String serverURI){
            if (reconnect) {
                // Because Clean Session is true, we need to re-subscribe
                subscribeToTopic(myTopic);
            } else {
                Log.i(TAG,"Connected to: " + serverURI);
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String str1 = new String(message.getPayload());
            String str2 = topic + ";qos:" + message.getQos() + ";retained:" + message.isRetained();
            Log.i(TAG, "messageArrived:" + str1);
            Log.i(TAG, str2);
            messageListener.onMessageArrived(topic, str1);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {

        }

        @Override
        public void connectionLost(Throwable arg0) {
            // 失去连接，重连
            Log.i(TAG,"The Connection was lost.");
            isConnect = false;
        }
    };

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) ContextUtil.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "MQTT当前网络名称：" + name);
            return true;
        } else {
            Log.i(TAG, "MQTT 没有可用网络");
            return false;
        }
    }

    /**
     * 获取手机IMEI号
     *
     * 需要动态权限: android.permission.READ_PHONE_STATE
     */
    public static String getIMEI(Context context) {
        String imei = "0123456789";
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        if(PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context,"android.permission.READ_PHONE_STATE")) {
            imei = telephonyManager.getDeviceId();
        }
        return imei;
    }

    public void release() {
        try {
            client.disconnect();
            instance = null;
            client = null;
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置messageListener 监听MQTT client 收到的消息
     * @param messageListener
     */
    public void setMessageListener(IMessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public interface IMessageListener {
        void onMessageArrived(String topic, String msg);
    }
}
