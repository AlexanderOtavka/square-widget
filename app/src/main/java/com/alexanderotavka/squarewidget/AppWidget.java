package com.alexanderotavka.squarewidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.Calendar;

/**
 * Manages widget and schedules updates.
 */
public class AppWidget extends AppWidgetProvider {

    @SuppressWarnings("unused")
    private static final String TAG = "widget";

    private static final String ACTION_SCHEDULED_UPDATE = "com.alexanderotavka.squarewidget" +
            ".SCHEDULED_UPDATE";

    private static final int HOUR_MIDNIGHT = 0;
    private static final int HOUR_NOON = 12;
    private static final int HOUR_EVENING = 18;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_SCHEDULED_UPDATE)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, AppWidget.class));
            onUpdate(context, manager, ids);
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        CharSequence greeting;

        if (hour >= HOUR_MIDNIGHT && hour < HOUR_NOON) {
            greeting = context.getString(R.string.widget_greetingMorning);
        } else if (hour >= HOUR_NOON && hour < HOUR_EVENING) {
            greeting = context.getString(R.string.widget_greetingAfternoon);
        } else {
            greeting = context.getString(R.string.widget_greetingEvening);
        }

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);
        views.setTextViewText(R.id.widget_greeting, greeting);
        views.setContentDescription(R.id.widget_greeting, greeting);

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        _scheduleNextUpdate(context);
    }

    private static void _scheduleNextUpdate(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AppWidget.class);
        intent.setAction(ACTION_SCHEDULED_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        long nowMS = System.currentTimeMillis();
        long midnightMS = _getDateMillis(nowMS, HOUR_MIDNIGHT);
        long noonMS = _getDateMillis(nowMS, HOUR_NOON);
        long eveningMS = _getDateMillis(nowMS, HOUR_EVENING);

        long soonestBreakpoint = Math.min(midnightMS, Math.min(noonMS, eveningMS));

        // Schedule to update when convenient for the system, will not wakeup device
        alarmManager.set(AlarmManager.RTC, soonestBreakpoint, pendingIntent);
    }

    private static long _getDateMillis(long nowMS, int hourOfDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);

        // One second later to be sure we are within the breakpoint.
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 1);
        calendar.set(Calendar.MILLISECOND, 0);

        long millis = calendar.getTimeInMillis();

        // Just in case we are right on the breakpoint, we schedule the next one.
        final long BUFFER_TIME_MS = 1000;
        if (millis < nowMS + BUFFER_TIME_MS) {
            final long DAY_MS = 24 * 60 * 60 * 1000;
            millis += DAY_MS;
        }

        return millis;
    }

}