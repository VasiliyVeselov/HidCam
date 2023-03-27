package com.noname.hidcam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.stopService(new Intent(context, MyForegroundService.class));
        Intent serviceIntent = new Intent(context, MyForegroundService.class);
        context.startForegroundService(serviceIntent);

        Log.e("myLog", "MyBroadcastReceiver start");

    }
}
