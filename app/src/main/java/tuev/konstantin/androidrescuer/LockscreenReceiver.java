package tuev.konstantin.androidrescuer;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;


@SuppressWarnings("WrongConstant")
public class LockscreenReceiver extends BroadcastReceiver {
    private KeyguardManager kgMgr;

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }
        if (this.kgMgr == null) {
            this.kgMgr = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        }
        if (intent.getAction().equals("android.intent.action.CLOSE_SYSTEM_DIALOGS")) {
            ProtectorService.Log.v("event Close");
            String actionIntent = intent.getStringExtra(context.getString(R.string.receiver_reason));
            boolean showing = this.kgMgr.inKeyguardRestrictedInputMode();
            ProtectorService.Log.v("Keyguard: " + showing);
            if (actionIntent != null && actionIntent.equals(context.getString(R.string.receiver_global)) && showing) {
                try {
                    Thread.sleep(Helper.getDelayValue(context));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ProtectorService.Log.v("send call close");
                Intent closeDialog = new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS");
                context.sendBroadcast(closeDialog);
                ProtectorService.Log.v("closing");
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
                context.sendBroadcast(closeDialog);
            }
        }
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            ProtectorService.Log.v("init watcher service on boot");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, ProtectorService.class));
            } else {
                context.startService(new Intent(context, ProtectorService.class));
            }
        }
    }
}
