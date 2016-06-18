package com.alexanderotavka.squarewidget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Handle updating the greeting at scheduled times.
 *
 * Created by Zander on 6/16/16.
 */
public class WidgetUpdateBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("widget", "onReceive: received broadcast");

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int id = intent.getIntExtra(AppWidget.INTENT_EXTRA_ID, -1);
        if (BuildConfig.DEBUG && id != -1) throw new AssertionError();

        AppWidget.setAlreadyScheduled(context, id, false);
        AppWidget.updateAppWidget(context, manager, id);
    }

}
