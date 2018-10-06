package tuev.konstantin.androidrescuer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.Iterator;

public class FirebaseMessaging extends FirebaseMessagingService {

    private static final String ANDROID_CHANNEL_ID = "tuev.konstantin.androidrescuer.Topic";
    private static final String ANDROID_CHANNEL_NAME = "Android Rescuer Info";

    IHandleControlMessage handleControlMessage;
    private NotificationManager mManager;
    private String from;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: "+this.getClass().getName());
        handleControlMessage = new HandleControlMessage(getApplicationContext(), true);
    }

    private static final String TAG = FirebaseMessaging.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            from = remoteMessage.getFrom();
            try {
                JSONObject json = new JSONObject(remoteMessage.getData());
                handleDataMessage(json);
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
            }
        }
    }

    private void handleDataMessage(JSONObject json) {
        Log.d(TAG, "push json: " + json.toString());

        boolean notification = json.optBoolean("notification", false);
        new Thread(() -> {
            if (!notification) {
                try {
                    Iterator<String> keys = json.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        Helper.controlState state = Helper.controlState.fromJSON(json, key);
                        Helper.handle(handleControlMessage, key, state);
                        Thread.sleep(1200);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e.getMessage());
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        sendBroadcast(new Intent("tuev.konstantin.androidrescuer.UpdateSwitches"));
                    }
                });
            } else {
                String bodyRaw = json.optString("body");
                String title = json.optString("title");
                Log.d(TAG, "handleDataMessage: topic: "+from);
                boolean important = from.contains("important");
                String body = bodyRaw;
                if (bodyRaw.length() > 17) {
                    body = bodyRaw.substring(0, 17) + "...";
                }


                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                Intent openInfo = new Intent(getApplicationContext(), MainActivity.class);
                openInfo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                openInfo.putExtra("dialogTitle", title);
                openInfo.putExtra("dialogMsg", bodyRaw);

                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 4386, openInfo, PendingIntent.FLAG_CANCEL_CURRENT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel androidChannel = new NotificationChannel(ANDROID_CHANNEL_ID,
                            ANDROID_CHANNEL_NAME, important ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_DEFAULT);
                    // Sets whether notifications posted to this channel should display notification lights
                    androidChannel.enableLights(true);

                    // Sets whether notification posted to this channel should vibrate.
                    androidChannel.enableVibration(true);
                    // Sets the notification light color for notifications posted to this channel
                    androidChannel.setLightColor(Color.GREEN);
                    // Sets whether notifications posted to this channel appear on the lockscreen or not
                    androidChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

                    getManager().createNotificationChannel(androidChannel);

                    getManager().notify(4385, new Notification.Builder(getApplicationContext(), ANDROID_CHANNEL_ID)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setSmallIcon(R.drawable.ic_stat_name)
                            .setLargeIcon(icon)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true).build());
                } else {
                    Notification.Builder builder = new Notification.Builder(getApplicationContext())
                            .setContentTitle(title)
                            .setContentText(body)
                            .setSmallIcon(R.drawable.ic_stat_name)
                            .setLargeIcon(icon)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);
                    Notification notification1;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        builder.setPriority(important ? Notification.PRIORITY_HIGH : Notification.PRIORITY_DEFAULT);
                        notification1 = builder.build();
                    } else {
                        notification1 = builder.getNotification();
                    }
                    getManager().notify(4385, notification1);
                    if (important) {
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        PowerManager.WakeLock wakeLock;
                        if (pm != null) {
                            wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                    PowerManager.ON_AFTER_RELEASE, "WakeLock");
                            wakeLock.acquire(1000L);
                        }
                    }
                }
            }
        }).start();
    }

    private NotificationManager getManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }
}
