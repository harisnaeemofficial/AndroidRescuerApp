package tuev.konstantin.androidrescuer;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserHandle;
import android.util.Log;

import static tuev.konstantin.androidrescuer.MainActivity.TAG;

public class Admin extends DeviceAdminReceiver {

    public DevicePolicyManager devicePolicyManager;
    public ComponentName demoDeviceAdmin;

    @Override
    public void onEnabled(Context context, Intent intent) {
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        demoDeviceAdmin = new ComponentName(context, Admin.class);
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        if (Helper.getPrefs(context).getBoolean("wrongPassPic", false)) {
            Intent startService = new Intent(context, ProtectorService.class);
            startService.putExtra("action", "takePic");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startService);
            } else {
                context.startService(startService);
            }
        }
        if (Helper.getPrefs(context).getBoolean("wrongPassLocation", false)) {
            Intent startService = new Intent(context, ProtectorService.class);
            startService.putExtra("action", "sendLocation");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startService);
            } else {
                context.startService(startService);
            }
        }
        if (Logger.log(context)) {
            Logger.d("wrong Password Entered");
        }
        Log.d(TAG, "onPasswordFailed");
    }
}
