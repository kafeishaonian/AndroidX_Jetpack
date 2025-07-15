package com.example.module_fundamental.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BroadcastHelper {

    private static final String TAG = BroadcastHelper.class.getSimpleName();

    public static void sendBroadcast(Context context, String action) {
        sendBroadcast(context, new Intent(action));
    }

    public static void sendBroadcast(Context context, Intent action) {
        Log.i(TAG, "tang-----发送广播 " + action.getAction() + "    " + action.getExtras());
        LocalBroadcastManager.getInstance(context).sendBroadcast(action);
    }

    public static void registerBroadcast(Context context, BroadcastReceiver receiver, String... action) {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
        if (receiver == null || action == null || action.length == 0) {
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        for (String str: action) {
            intentFilter.addAction(str);
        }
        broadcastManager.registerReceiver(receiver, intentFilter);
    }


    public static void registerBroadcast(Context context, BroadcastReceiver receiver, IntentFilter filter) {
        if (receiver == null || filter == null) {
            return;
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
    }

    public static void unregisterBroadcast(Context context, BroadcastReceiver receiver) {
        if (context == null || receiver == null) {
            return;
        }

        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }
}