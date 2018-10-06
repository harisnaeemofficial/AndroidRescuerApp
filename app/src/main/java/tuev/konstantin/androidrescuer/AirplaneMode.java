package tuev.konstantin.androidrescuer;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import static android.content.Context.KEYGUARD_SERVICE;
import static tuev.konstantin.androidrescuer.MainActivity.TAG;

public class AirplaneMode extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (intent.getAction() != null && intent.getAction().equals("android.intent.action.AIRPLANE_MODE")) {
                Log.d(TAG, "onReceive: airplane");
                KeyguardManager kgMgr = (KeyguardManager) context.getApplicationContext().getSystemService(KEYGUARD_SERVICE);
                if (kgMgr != null && kgMgr.inKeyguardRestrictedInputMode() && Helper.isAirplaneModeOn(context) && Helper.sharedPrefs(context).getBoolean("airplane_lock", false)) {
                    (new Handler()).postDelayed(() -> Helper.setAirplaneMode(false), 1000);
                }
            }
        }
    }
}
