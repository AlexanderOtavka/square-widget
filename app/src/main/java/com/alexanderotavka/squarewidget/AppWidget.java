package com.alexanderotavka.squarewidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

/**
 * Manages widget and schedules updates.
 */
public class AppWidget extends AppWidgetProvider {

    public static final String INTENT_EXTRA_ID = "id";

    private static final String PENDING_INTENTS = "pendingIntents";

    private static final int HOUR_MIDNIGHT = 0;
    private static final int HOUR_NOON = 12;
    private static final int HOUR_EVENING = 18;

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                       int appWidgetId) {
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

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);

        _scheduleNextUpdate(context, appWidgetId);
    }

    public static boolean getAlreadyScheduled(Context context, int id) {
        return context.getSharedPreferences(PENDING_INTENTS, Context.MODE_PRIVATE)
                .getBoolean(String.valueOf(id), false);
    }

    public static void setAlreadyScheduled(Context context, int id, boolean alreadyScheduled) {
        context.getSharedPreferences(PENDING_INTENTS, Context.MODE_PRIVATE).edit()
                .putBoolean(String.valueOf(id), alreadyScheduled)
                .apply();
    }

    private static void _scheduleNextUpdate(Context context, int id) {
        if (!getAlreadyScheduled(context, id)) {
            setAlreadyScheduled(context, id, true);

            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, WidgetUpdateBroadcastReceiver.class);
            intent.putExtra(INTENT_EXTRA_ID, id);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            long nowMS = System.currentTimeMillis();
            long midnightMS = _getDateMillis(nowMS, HOUR_MIDNIGHT);
            long noonMS = _getDateMillis(nowMS, HOUR_NOON);
            long eveningMS = _getDateMillis(nowMS, HOUR_EVENING);

            long soonestBreakpoint = Math.min(midnightMS, Math.min(noonMS, eveningMS));
            alarmManager.set(AlarmManager.RTC, soonestBreakpoint, pendingIntent);
        }
    }

    private static long _getDateMillis(long nowMS, int hourOfDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);

        // One minute later to be sure we are within the breakpoint.
        calendar.set(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
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

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("widget", "onUpdate: updating...");
        // There may be multiple widgets active, so update all of them
        for (int id : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences(PENDING_INTENTS, Context.MODE_PRIVATE).edit();
        for (int id : appWidgetIds) {
            editor.remove(String.valueOf(id));
        }

        editor.apply();
    }

}

