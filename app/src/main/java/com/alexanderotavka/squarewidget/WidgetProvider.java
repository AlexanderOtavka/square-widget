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
public class WidgetProvider extends AppWidgetProvider {

    @SuppressWarnings("unused")
    private static final String TAG = "WidgetProvider";

    private static final String ACTION_SCHEDULED_UPDATE =
            "com.alexanderotavka.squarewidget.SCHEDULED_UPDATE";

    private static final int HOUR_MIDNIGHT = 0;
    private static final int HOUR_NOON = 12;
    private static final int HOUR_EVENING = 18;

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case Intent.ACTION_TIMEZONE_CHANGED:
            case Intent.ACTION_TIME_CHANGED:
            case ACTION_SCHEDULED_UPDATE:
                AppWidgetManager manager = AppWidgetManager.getInstance(context);
                int[] ids = manager.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
                onUpdate(context, manager, ids);
                break;
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
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
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
        Intent intent = new Intent(context, WidgetProvider.class).setAction(ACTION_SCHEDULED_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        Calendar now = Calendar.getInstance();
        long midnight = _getDateMillis(now, HOUR_MIDNIGHT);
        long noon = _getDateMillis(now, HOUR_NOON);
        long evening = _getDateMillis(now, HOUR_EVENING);
        long soonestBreakpoint = Math.min(midnight, Math.min(noon, evening));

        // Schedule to update when convenient for the system, will not wakeup device
        alarmManager.set(AlarmManager.RTC, soonestBreakpoint, pendingIntent);
    }

    private static long _getDateMillis(Calendar now, int hourOfDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, 0);

        // One second later to be sure we are within the breakpoint
        calendar.set(Calendar.SECOND, 1);
        calendar.set(Calendar.MILLISECOND, 0);

        // Ensure date is in the future
        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return calendar.getTimeInMillis();
    }

}