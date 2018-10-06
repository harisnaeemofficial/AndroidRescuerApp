package tuev.konstantin.androidrescuer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.sax.EndElementListener;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.securepreferences.SecurePreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.chainfire.libsuperuser.Shell;
import tuev.konstantin.adb.AdbConnection;
import tuev.konstantin.adb.AdbCrypto;
import tuev.konstantin.adb.AdbStream;
import tuev.konstantin.adb.AdbUtils;

class Helper {
    private static SharedPreferences prefs;
    private static String address, state, country, postalCode, knownName, city, latitude, longtitude;

    public static void handle(IHandleControlMessage handleControlMessage, String key, controlState state) {
        if (state != null && key != null) {
            (new Handler(Looper.getMainLooper())).post(() -> {
                switch (key) {
                    case "realTimeLoc":
                        handleControlMessage.locationTracking(state.getValue());
                        break;
                    case "wrongPassLocation":
                        handleControlMessage.locationOnWrongPass(state.getValue());
                        break;
                    case "wrongPassPic":
                        handleControlMessage.pictureOnWrongPass(state.getValue());
                        break;
                    case "backupContacts":
                        handleControlMessage.backupContacts();
                        break;
                    case "backupFolder":
                        handleControlMessage.backupRescueFolder();
                        break;
                    case "factoryReset":
                        handleControlMessage.factoryDataReset(!state.getValue());
                        break;
                    case "deleteInternal":
                        handleControlMessage.deleteInternalStorage();
                        break;
                    case "location":
                        handleControlMessage.locationGPS(state.getValue());
                        break;
                    case "mobileData":
                        handleControlMessage.mobileData(state.getValue());
                        break;
                    case "wifiData":
                        handleControlMessage.handleNetLost(state.getValue());
                }
            });
        }
    }

    public static PassNPhone getPassNPhone(Context context) {
        SharedPreferences preferences = Helper.getPrefs(context);
        return new PassNPhone(preferences.getString("pass", ""), preferences.getString("phone", ""));
    }

    public static void putPassNPhone(Context context, String pass, String phone) {
        SharedPreferences preferences = Helper.getPrefs(context);
        preferences.edit().putString("pass", pass).putString("phone", phone).apply();
    }

    public static boolean canWriteSystemSettings(Context context) {
        return ContextCompat.checkSelfPermission(context, "android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED;
    }

    public static String getServerImageUrl(Context context, String whichImage) {
        return nativeLib.getInstance().urlPathImage(home(context), whichImage);
    }

    public static void showPrepareForPCActivation(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Read before proceeding")
                .setMessage("This will require:\n\t*internet connection\n\t*computer\n\t*desktop app\n\t*data cable.\nThis CANNOT harm your phone, don't worry about it.")
                .setPositiveButton("All ok, proceed", (dialog, which) -> {
                    context.startActivity(new Intent(context, ComputerActivity.class));
                })
                .setNegativeButton("Give me time", (dialog, which) -> Toast.makeText(context, "You can do this later in the Control Panel.", Toast.LENGTH_SHORT).show()).show();

    }

    public interface onReady {
        void ready(JSONObject out);
    }

    public static class CallAPI extends AsyncTask<String, String, JSONObject> {
        onReady handler;
        String json = null;
        JSONObject jsonObject;
        private static JSONObject noNet = null;
        static {
            try {
                noNet = new JSONObject("{" +
                        "\"result\": \"Couldn't connect to server.\nCheck your internet connection.\"," +
                        "\"error\": true" +
                        "}");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        CallAPI(String url, String json, onReady handler) {
            this.handler = handler;
            this.json = json;
            this.execute(url);
        }

        CallAPI(String url, JSONObject json, onReady handler) {
            this.handler = handler;
            this.jsonObject = json;
            this.execute(url);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected JSONObject doInBackground(String... params) {

            String urlString = params[0]; // URL to call

            String resultToDisplay = "";

            InputStream in;
            try {

                URL url = new URL(urlString);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);

                OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());

                String data = "";
                 data += URLEncoder.encode("json", "UTF-8") + "="
                        + URLEncoder.encode(json == null ? jsonObject.toString() : json, "UTF-8");


                wr.write(data);
                wr.flush();


                int statusCode = urlConnection.getResponseCode();
                if (statusCode >= 200 && statusCode < 400) {
                    in = new BufferedInputStream(urlConnection.getInputStream());
                } else {
                    in = new BufferedInputStream(urlConnection.getErrorStream());
                }

                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line).append('\n');
                }
                resultToDisplay = total.toString();
                return new JSONObject(resultToDisplay);
            } catch (Exception e) {

                e.printStackTrace();

                return noNet;

            }
        }


        @Override
        protected void onPostExecute(JSONObject result) {
            handler.ready(result);
        }
    }

    private static void setMobileDataEnabled(Context context, boolean enabled) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final ConnectivityManager conman = (ConnectivityManager)  context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conman == null) {
            return;
        }
        final Class conmanClass = Class.forName(conman.getClass().getName());
        final Field connectivityManagerField = conmanClass.getDeclaredField("mService");
        connectivityManagerField.setAccessible(true);
        final Object connectivityManager = connectivityManagerField.get(conman);
        final Class<?> connectivityManagerClass =  Class.forName(connectivityManager.getClass().getName());
        final Method setMobileDataEnabledMethod = connectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", boolean.class);
        setMobileDataEnabledMethod.setAccessible(true);

        setMobileDataEnabledMethod.invoke(connectivityManager, enabled);
    }

    private static void setMobileNetworkfromLollipop(Context context, boolean enable) throws Exception {
        String command = "No command generated";
        int state;
        // Get the current state of the mobile network.
        state = enable ? 1 : 0;
        ProtectorService.Log.v("State: "+state);
        // Get the value of the "TRANSACTION_setDataEnabled" field.
        String transactionCode = getTransactionCode(context);
        ProtectorService.Log.v("transactionCode: "+transactionCode);
        // Android 5.1+ (API 22) and later.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            ProtectorService.Log.v("Bigger than Lollipop.");
            SubscriptionManager mSubscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            // Loop through the subscription list i.e. SIM list.
            if (mSubscriptionManager != null) {
                int subscriptionId = mSubscriptionManager.getActiveSubscriptionInfoList().get(0).getSubscriptionId();
                // Execute the command via `su` to turn off
                // mobile network for a subscription service.
                command = "service call phone " + transactionCode + " i32 " + subscriptionId + " i32 " + state;
                Shell.SU.run(command);
            } else {
                ProtectorService.Log.v("mSubscriptionManager is null.");
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            // Android 5.0 (API 21) only.
            if (transactionCode != null && transactionCode.length() > 0) {
                // Execute the command via `su` to turn off mobile network.
                command = "service call phone " + transactionCode + " i32 " + state;
                Shell.SU.run(command);
            }
        }
        ProtectorService.Log.v("command: "+command);
    }
    private static String getTransactionCode(Context context) throws Exception {
        final TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager == null) {
            return null;
        }
        final Class<?> mTelephonyClass = Class.forName(mTelephonyManager.getClass().getName());
        final Method mTelephonyMethod = mTelephonyClass.getDeclaredMethod("getITelephony");
        mTelephonyMethod.setAccessible(true);
        final Object mTelephonyStub = mTelephonyMethod.invoke(mTelephonyManager);
        final Class<?> mTelephonyStubClass = Class.forName(mTelephonyStub.getClass().getName());
        final Class<?> mClass = mTelephonyStubClass.getDeclaringClass();
        final Field field = mClass.getDeclaredField("TRANSACTION_setDataEnabled");
        field.setAccessible(true);
        return String.valueOf(field.getInt(null));
    }

    static void toggleLocation(Context context, boolean state) {
        if (canWriteSystemSettings(context)) {
            if (state) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
                } else {
                    Settings.Secure.putString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, "gps,network,wifi");
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
                } else {
                    Settings.Secure.putString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, "");
                }
            }
        } else {
            if (Shell.SU.available()) {
                if (state) {
                    Shell.SU.run("settings put secure location_providers_allowed +network,gps,wifi");
                } else {
                    Shell.SU.run("settings put secure location_providers_allowed -"+Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED));
                }
            } else {
                if (state) {
                    turnGPSOn(context);
                } else {
                    turnGPSOff(context);
                }
            }
        }
    }

    private static void turnGPSOn(Context context){
        String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if(!provider.contains("gps")){ //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            context.sendBroadcast(poke);
        }
    }

    private static void turnGPSOff(Context context){
        String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if(provider.contains("gps")){ //if gps is enabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("0"));
            context.sendBroadcast(poke);
        }
    }


    public interface End {
        void onConnectionResult(boolean fail);
    }
    public static void sendLocalConnectionCommandAndEnd(String command, Context context, End endListener) {
        new Thread(new Runnable() {
            static final int CONN_TIMEOUT = 5000;
            String host = "127.0.0.1";
            int port = 5555;
            AdbStream shellStream = null;
            AdbConnection connection = null;

            public void run() {
                Socket socket = new Socket();
                AdbCrypto crypto = AdbUtils.readCryptoConfig(context.getFilesDir());
                if (crypto == null) {
                    crypto = AdbUtils.writeNewCryptoConfig(context.getFilesDir());
                }
                if (crypto != null) {
                    try {
                        socket.connect(new InetSocketAddress(host, port), CONN_TIMEOUT);
                        try {
                            connection = AdbConnection.create(socket, crypto);
                            connection.connect();
                            shellStream = connection.open("shell:"+command);
                        } catch (Throwable th) {
                            if (endListener != null) {
                                endListener.onConnectionResult(true);
                            }
                            th.printStackTrace();
                            AdbUtils.safeClose(shellStream);
                            if (!AdbUtils.safeClose(connection)) {
                                try {
                                    socket.close();
                                    return;
                                } catch (IOException e6) {
                                    return;
                                }
                            }
                            return;
                        }
                        synchronized (this) {
                            while (!connection.connected) {
                                Thread.sleep(1000);
                            }

                            Thread.sleep(2500);

                            AdbUtils.safeClose(shellStream);
                            if (!AdbUtils.safeClose(connection)) {
                                try {
                                    socket.close();
                                } catch (IOException e5) {
                                    e5.printStackTrace();
                                }
                            }
                            if (endListener != null) {
                                endListener.onConnectionResult(false);
                            }
                        }
                    } catch (Exception e22) {
                        if (endListener != null) {
                            endListener.onConnectionResult(true);
                        }
                        e22.printStackTrace();
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }
    
    @SuppressLint("StaticFieldLeak")
    static void toggleData(Context context, boolean state) {
        if (Shell.SU.available()) {
            ProtectorService.Log.v("Root data");
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                if (state) {
                    Shell.SU.run("svc data enable");
                } else {
                    Shell.SU.run("svc data disable");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    setMobileNetworkfromLollipop(context, state);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            ProtectorService.Log.v("No root data");
            String command = null;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                if (state) {
                    command = "svc data enable";
                } else {
                    command = "svc data disable";
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int state1;
                // Get the current state of the mobile network.
                state1 = state ? 1 : 0;
                ProtectorService.Log.v("State: "+state);
                // Get the value of the "TRANSACTION_setDataEnabled" field.
                String transactionCode = null;
                try {
                    transactionCode = getTransactionCode(context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ProtectorService.Log.v("transactionCode: "+transactionCode);
                // Android 5.1+ (API 22) and later.
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    ProtectorService.Log.v("Bigger than Lollipop.");
                    SubscriptionManager mSubscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                    if (mSubscriptionManager != null) {
                        int subscriptionId = mSubscriptionManager.getActiveSubscriptionInfoList().get(0).getSubscriptionId();
                        // Execute the command via `su` to turn off
                        // mobile network for a subscription service.
                        command = "service call phone " + transactionCode + " i32 " + subscriptionId + " i32 " + state1;
                    } else {
                        ProtectorService.Log.v("mSubscriptionManager is null.");
                    }
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                    // Android 5.0 (API 21) only.
                    if (transactionCode != null && transactionCode.length() > 0) {
                        // Execute the command via `su` to turn off mobile network.
                        command = "service call phone " + transactionCode + " i32 " + state;
                        Shell.SU.run(command);
                    }
                }
            }
            if (command != null) {
                sendLocalConnectionCommandAndEnd(command, context, (fail) -> {
                    if (fail) {
                        try {
                            setMobileDataEnabled(context, state);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    // Clears notification tray messages
    public static void clearNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

    private static final String homeWifi = "M-Tel_1963";

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    private static Boolean homeWifiB = null;

    private static boolean home(Context context) {
        if (homeWifiB != null) {
            return homeWifiB;
        }
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = null;

        if (wifiManager != null) {
            wifiInfo = wifiManager.getConnectionInfo();
        }
        return homeWifiB = ((wifiInfo != null && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED && wifiInfo.getSSID().replace("\"", "").equalsIgnoreCase(homeWifi)) || isEmulator());
    }

    static boolean locationEnabled(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        boolean enabled;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int location = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
            enabled = location == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY || location == Settings.Secure.LOCATION_MODE_BATTERY_SAVING;
        } else {
            String location = Settings.Secure.getString(contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            enabled = location != null && (location.contains("network") || location.contains("gps"));
        }
        return enabled;
    }

    public enum url {
        REGISTER(0),
        LOGIN(1),
        REGISTERTOKEN(2),
        FORGOTTENPASSWORD(3),
        NEWUSERLOCATION(4),
        JUSTUSERLOCATION(5),
        THENETMESSAGETHING(6),
        BETATESTERS(7);

        public int value;

        url(int value)
        {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
    public enum controlState {
        ENABLE(true),
        DISABLE(false);
        public boolean value;

        controlState(boolean value)
        {
            this.value = value;
        }

        public boolean getValue() {
            return value;
        }

        public static controlState fromJSON(JSONObject json, String key) {
            try {
                switch (json.getInt(key)) {
                    case 0:
                        return controlState.DISABLE;
                    case 1:
                        return controlState.ENABLE;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        public static Map.Entry<String, controlState> fromLine(String line) {
            try {
                if (line.contains(":")) {
                    String[] parts = line.split(":");
                    String key = parts[0];
                    controlState value = null;
                    if (parts[1].contains("0")) {
                        value = controlState.DISABLE;
                    } else if (parts[1].contains("1")) {
                        value = controlState.ENABLE;
                    }
                    controlState finalValue = value;
                    return new Map.Entry<String, controlState>() {
                        @Override
                        public String getKey() {
                            return key;
                        }

                        @Override
                        public controlState getValue() {
                            return finalValue;
                        }

                        @Override
                        public controlState setValue(controlState value) {
                            return null;
                        }
                    };
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
            return null;
        }
    }

    public static boolean firstRun(Context context) {
        return deviceSpecificPrefs(context).getBoolean("firstrun", true);
    }

    public static void firstRunFalse(Context context) {
        deviceSpecificPrefs(context).edit().putBoolean("firstrun", false).apply();
    }

    public static SharedPreferences deviceSpecificPrefs(Context context) {
        return context.getSharedPreferences("device_specific_shit", Context.MODE_PRIVATE);
    }

    static String getServerUrl(Context context, url url) {
        return nativeLib.getInstance().urlPath(home(context), url);
    }

    public static SharedPreferences sharedPrefs(Context context) {
        if (prefs == null) {
            return prefs = new SecurePreferences(context.getApplicationContext());
        } else {
            return prefs;
        }
    }

    public static SharedPreferences getPrefs(Context context) {
        return sharedPrefs(context);
    }

    static boolean getLockscreenProtectorEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.pref_key_enable_lockscreen_protector), false);
    }

    static boolean getBlockStatusBar(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_enable_block_status_bar), true);
    }
    public static void setLockscreenProtectorEnabled(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.pref_key_enable_lockscreen_protector), value).apply();
    }


    public static long getDelayValue(Context context) {
        long value = 100;
        try {
            value = PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.pref_key_settings_delay), 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
    public static void setDelayValue(Context context, long value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(context.getString(R.string.pref_key_settings_delay), value).apply();
    }

    public static List<onSimRemove> actionsOnSimRemove(Context context) {
        SharedPreferences prefs = sharedPrefs(context);
        if (prefs.contains("onSimRemove")) {
            return new Gson().fromJson(prefs.getString("onSimRemove", ""), new TypeToken<List<onSimRemove>>(){}.getType());
        } else {
            return Collections.singletonList(onSimRemove.TAKE_PICTURE_FRONT);
        }
    }

    public static void putActionsOnSimRemove(Context context, List<onSimRemove> actions) {
        sharedPrefs(context).edit().putString("onSimRemove", new Gson().toJson(actions)).apply();
    }

    enum onSimRemove{
        WIPE(0),
        SEARCH_WIFI(1),
        SET_LOCK_PASS(2),
        TAKE_PICTURE_FRONT(3),
        NONE(4);

        public int getValue() {
            return value;
        }

        private int value;
        onSimRemove(int value) {
            this.value = value;
        }
    }

    public interface onResult {
        void rootResult(boolean hasRoot);
    }

    public static void hasRoot(Context context, onResult resultCallback) {
        AlertDialog progressDialog = null;
        if (context instanceof Activity) {
            @SuppressLint("InflateParams") View material_progress_dialog = LayoutInflater.from(context).inflate(R.layout.material_progress_dialog, null);
            progressDialog = new AlertDialog.Builder(context)
                    .setView(material_progress_dialog)
                    .setCancelable(false)
                    .show();
        }
        AlertDialog finalProgressDialog = progressDialog;
        new Thread(() -> {
            boolean res = Shell.SU.available();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalProgressDialog != null) {
                    finalProgressDialog.cancel();
                }
                resultCallback.rootResult(res);
            });
        }).start();
    }

    public static boolean isAirplaneModeOn(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

    public static void setAirplaneMode(boolean state) {
        new Thread(() -> {
            if (Shell.SU.available()) {
                Shell.SU.run(new String[]{"settings put "+(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1?"system":"global")+" airplane_mode_on "+ (state ? "1" : "0"), "am broadcast -a android.intent.action.AIRPLANE_MODE"});
            }
        }).start();
    }

    private static final String OPEN_WEATHER_MAP_API =
            "http://api.openweathermap.org/data/2.5/weather?q=%s&units=metric";

    public static JSONObject getWeather(String city){
        try {
            URL url = new URL(String.format(OPEN_WEATHER_MAP_API, city));
            HttpURLConnection connection =
                    (HttpURLConnection)url.openConnection();

            connection.addRequestProperty("x-api-key", "3a51104a01f2b6ad7f48e802817a3192");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            StringBuffer json = new StringBuffer(1024);
            String tmp="";
            while((tmp=reader.readLine())!=null)
                json.append(tmp).append("\n");
            reader.close();

            JSONObject data = new JSONObject(json.toString());

            // This value will be 404 if the request was not
            // successful
            if(data.getInt("cod") != 200){
                return null;
            }

            return data;
        }catch(Exception e){
            return null;
        }
    }

    private static void updateWeatherData(MainActivity context) {
        final Handler handler = new Handler();
        new Thread() {
            public void run() {
                final AlertDialog.Builder ad = new AlertDialog.Builder(context);
                if (city != null && !city.equals("Maybe water")) {
                    try {
                        final JSONObject json = getWeather(city);
                        if (json != null) {
                            String title = "";
                            if (json.getString("name") != null) {
                                title += json.getString("name").toUpperCase();
                            }
                            if (json.getJSONObject("sys").getString("country") != null) {
                                title += " in " + json.getJSONObject("sys").getString("country");
                            }
                            ad.setTitle(title);
                        }
                        JSONObject details = null;
                        if (json != null) {
                            details = json.getJSONArray("weather").getJSONObject(0);
                        }
                        JSONObject main = null;
                        if (json != null) {
                            main = json.getJSONObject("main");
                        }
                        String message = "";
                        if (details != null) {
                            if (details.getString("description") != null) {
                                message += (details.getString("description").toUpperCase() + "\n");
                            }
                            if (main.getString("humidity") != null) {
                                message += "Humidity: " + main.getString("humidity") + "%";
                            }
                            if (main.getString("pressure") != null) {
                                message += "\n" + "Pressure: " + main.getString("pressure") + " hPa\n";
                            }
                        }
                        if (main != null) {
                            message += ("Temperature: " + String.format(Locale.GERMANY, "%.2f", main.getDouble("temp")) + " ℃\n");
                        }
                        if (main != null) {
                            Log.d("EROR", main.getDouble("temp") + "");
                        }

                        DateFormat df = DateFormat.getDateTimeInstance();
                        String updatedOn = null;
                        if (json != null) {
                            updatedOn = df.format(new Date(json.getLong("dt") * 1000));
                        }
                        String sr = null;
                        if (json != null) {
                            sr = df.format(new Date(json.getJSONObject("sys").getLong("sunrise") * 1000));
                        }
                        String ss = null;
                        if (json != null) {
                            ss = df.format(new Date(json.getJSONObject("sys").getLong("sunset") * 1000));
                        }
                        if (sr != null) {
                            message += ("Sunrise: " + sr);
                        }
                        if (ss != null) {
                            message += "\nSunset: " + ss;
                        }
                        if (updatedOn != null) {
                            message += "\nLast update: " + updatedOn;
                        }
                        ad.setMessage(message);

                        final String finalMessage = message;
                        handler.post(() -> {
                            ad.setPositiveButton("OK", (dialog, which) -> context.toggleNavBar(false))
                                    .setOnCancelListener(dialog -> context.toggleNavBar(false))
                                    .setNegativeButton("More...", (dialog, which) -> showAddress(context));
                            if (finalMessage.replace("\n", "").equals("")) {
                                showAddress(context);
                            } else {
                                ad.show();
                            }
                        });
                    } catch (JSONException je) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                showAddress(context);
                            }
                        });
                    }
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            showAddress(context);
                        }
                    });
                }
            }
        }.start();
    }

    private static void showAddress(MainActivity context) {
        String message = "";
        message += ("City: " + city + "\n");

        if (address != null) {
            message += ("Address: " + address + "\n");
        }
        if (state != null) {
            message += ("State: " + state + "\n");
        }
        if (country != null) {
            message += ("Country: " + country + "\n");
        }
        if (postalCode != null) {
            message += ("Postal Code: " + postalCode + "\n");
        }
        if (knownName != null) {
            message += ("Known Name: " + knownName + "\n");
        }
        if (latitude != null) {
            message += ("Latitude: " + latitude + "\n");
        }
        if (longtitude != null) {
            message += ("Longitude: " + longtitude);
        }
        new AlertDialog.Builder(context)
                .setTitle("Address")
                .setMessage(message)
                .setPositiveButton("OK", (dialog1, which) -> context.toggleNavBar(false))
                .setOnCancelListener(dialog -> context.toggleNavBar(false)).show();
    }

    static void showAddressInfo(MainActivity context, LatLng location) {
        try {
            Geocoder geo = new Geocoder(context, Locale.ENGLISH);
            List<Address> addresses = geo.getFromLocation(location.latitude, location.longitude, 1);
            if (!addresses.isEmpty()) {
                address = addresses.get(0).getAddressLine(0);
                state = addresses.get(0).getAdminArea();
                city = addresses.get(0).getLocality();
                country = addresses.get(0).getCountryName();
                postalCode = addresses.get(0).getPostalCode();
                knownName = addresses.get(0).getFeatureName();
                latitude = String.valueOf(location.latitude);
                longtitude = String.valueOf(location.longitude);
                updateWeatherData(context);

            } else {
                address = null;
                state = null;
                city = "Maybe water";
                country = null;
                postalCode = null;
                knownName = null;
                latitude = String.valueOf(location.latitude);
                longtitude = String.valueOf(location.longitude);
                updateWeatherData(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
