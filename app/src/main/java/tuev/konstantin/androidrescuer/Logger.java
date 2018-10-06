package tuev.konstantin.androidrescuer;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import android.text.format.DateFormat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static tuev.konstantin.androidrescuer.MainActivity.TAG;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Logger {
    private static Context context;
    private static ExecutorService thread;
    private static File logFile = new File(Environment.getExternalStorageDirectory()+"/rescue");
    private static OutputStream stream;

    public static void startLogger(Context context) {
        if (Logger.context == null) {
            Logger.context = context;
            Logger.thread = Executors.newSingleThreadExecutor();
            Helper.sharedPrefs(context).edit().putBoolean("logging", true).apply();
            thread.submit(() -> {
                if (!logFile.exists()) {
                    logFile = new File(Environment.getExternalStorageDirectory() + "/Rescue");
                    if (!logFile.exists()) {
                        logFile.mkdirs();
                    }
                }
                logFile = new File(logFile + "/data");
                logFile.mkdirs();
                logFile = new File(logFile + "/log.txt");

                try {
                    if (!logFile.exists()) {
                        logFile.createNewFile();
                    }
                    stream = new BufferedOutputStream(new FileOutputStream(logFile));
                } catch (Exception e) {
                    e.printStackTrace();
                    Helper.sharedPrefs(context).edit().putBoolean("logging", false).apply();
                }
            });
        }
    }

    public static void d(String text) {
        Log.d(TAG, text);
        if (Logger.context != null) {
            String format = "dd/MM/yyyy HH:mm:ss";
            String out = DateFormat.format(format, new Date()) + ": " + text+"\n";
            thread.submit(() -> {
                try {
                    stream.write(out.getBytes());
                    stream.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void endLog() {
        Helper.sharedPrefs(context).edit().putBoolean("logging", false).apply();
        if (thread != null) {
            thread.submit(() -> {
                try {
                    stream.close();
                    Logger.context = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            thread.shutdown();
        }
    }

    public static boolean log(Context context) {
        return context != null && Helper.sharedPrefs(context).getBoolean("logging", false);
    }
}
