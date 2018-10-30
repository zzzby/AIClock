package com.example.zhanbo.aiclock.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.zhanbo.aiclock.AlarmActivity;
import com.example.zhanbo.aiclock.gson.Forecast;
import com.example.zhanbo.aiclock.util.OkHttpUtil;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class RequestSuggestionService extends Service {

    private static final String TAG = "RequestService";
    AlarmManager manager;
    boolean stopAlarm = false;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: " + "Service created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: " + "Service stopped");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 获取时间戳
        final long timeStamp = intent.getLongExtra("timeStamp", 0);
        querySuggestions(timeStamp);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "run: " + "10s after");
                if(!stopAlarm) {
                    manager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    int interval = 50 * 1000; // 请求的间隔为1分钟
                    long triggerAtTime = System.currentTimeMillis() + interval;
                    Intent i = new Intent(RequestSuggestionService.this, RequestSuggestionService.class);
                    i.putExtra("timeStamp", timeStamp);
                    // 每隔1分钟就启动一次自己
                    PendingIntent pi = PendingIntent.getService(RequestSuggestionService.this, 0, i, 0);
                    manager.cancel(pi);
                    manager.set(AlarmManager.RTC_WAKEUP, triggerAtTime, pi);
                }
            }
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }

    // expectTime是目标打卡时间的时间戳
    private void querySuggestions(long expectTime) {
        String tail = "?expect_time=" + expectTime;
        Log.i(TAG, "querySuggestions: " + expectTime);
        String address = "http://120.78.167.156:8080/aiclock.php" + tail;
        OkHttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // 获取到返回的数据，用gson转化为json对象
                String responseText = response.body().string();
                Log.i(TAG, "onResponse: " + responseText);
                Gson gson = new Gson();
                Forecast forecast = gson.fromJson(responseText, Forecast.class);
                // 如果请求到结果是WAKEUP，完全停止此服务，闹钟响起
                if (forecast.data.suggestion.equals("WAKEUP")) {
                    Intent intent = new Intent(RequestSuggestionService.this, AlarmActivity.class);
                    startActivity(intent);
                    stopSelf();
                    stopAlarm = true;
                }
            }
        });
    }
}
