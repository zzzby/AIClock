package com.example.zhanbo.aiclock.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by zhanby on 2018/10/27.
 */

public class OkHttpUtil {

    public static void sendOkHttpRequest(String address, okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(address)
                .build();
        client.newCall(request).enqueue(callback);
    }
}
