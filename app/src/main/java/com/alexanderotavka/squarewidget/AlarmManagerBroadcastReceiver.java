package com.alexanderotavka.squarewidget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Handle updating the greeting at scheduled times.
 *
 * Created by Zander on 6/16/16.
 */
public class AlarmManagerBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(AppWidget.INTENT_EXTRA_ID, -1);
        if (id != -1) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            AppWidget.updateAppWidget(context, manager, id);
        }
    }

}
