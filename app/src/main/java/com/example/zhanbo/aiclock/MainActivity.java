package com.example.zhanbo.aiclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.Toolbar;

import com.example.zhanbo.aiclock.gson.Forecast;
import com.example.zhanbo.aiclock.service.RequestSuggestionService;
import com.example.zhanbo.aiclock.util.OkHttpUtil;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private DatePicker datePicker;
    private TimePicker timePicker;
    Button confirm, cancel;
    TextView textView;
    Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        datePicker = findViewById(R.id.date_picker);
        timePicker = findViewById(R.id.time_picker);
        confirm = findViewById(R.id.confirm);
        cancel = findViewById(R.id.cancel);
        textView = findViewById(R.id.time);
        // 这里的calender需要先初始化成当前时间，因为用户可能没有动datePicker和timePicker
        calendar = Calendar.getInstance();

        // API要求为26
        datePicker.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar.set(year, monthOfYear, dayOfMonth);
            }
        });

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
                String temp = format.format(calendar.getTime()) + "  " +
                        calendar.get(Calendar.HOUR_OF_DAY) + "时" + calendar.get(Calendar.MINUTE) + "分";
                textView.setText("        目标打卡时间为\n" + temp);
                // calendar保存着目标打卡时间
//                querySuggestions(calendar.getTimeInMillis() / 1000);
                Toast.makeText(MainActivity.this, "闹钟设置成功，请保持网络畅通", Toast.LENGTH_SHORT).show();
//                Log.i(TAG, "onClick: " + calendar.getTimeInMillis() / 1000);

                confirm.setVisibility(View.GONE);
                textView.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.VISIBLE);

                // 设置一个定时器，当距离打卡时间还有90分钟时触发service
                long triggerTime = calendar.getTimeInMillis() - 90 * 60 * 1000;
                long currentTime = System.currentTimeMillis();
                // 直接响铃
                if (currentTime >= calendar.getTimeInMillis()) {
                    startActivity(new Intent(MainActivity.this, AlarmActivity.class));
                } else if (currentTime > triggerTime && currentTime < calendar.getTimeInMillis()) {  // 直接启动服务
                    Intent intent = new Intent(MainActivity.this, RequestSuggestionService.class);
                    intent.putExtra("timeStamp", calendar.getTimeInMillis() / 1000);    // 秒形式的时间戳
                    startService(intent);
                } else {  // 定时启动服务
                    AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    Intent intent = new Intent(MainActivity.this, RequestSuggestionService.class);
                    intent.putExtra("timeStamp", calendar.getTimeInMillis() / 1000);    // 秒形式的时间戳
                    PendingIntent pendingIntent = PendingIntent.getService(MainActivity.this, 0, intent, 0);
                    manager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);  // 到达时间就会触发RequestSuggestionService
                }
            }
        });

        // 要同时停止服务以及取消可能已经设置好的定时器
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirm.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.GONE);
                Intent intent = new Intent(MainActivity.this, RequestSuggestionService.class);
                PendingIntent pendingIntent = PendingIntent.getService(MainActivity.this, 0, intent, 0);
                stopService(intent);
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
                textView.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "闹钟已取消", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
