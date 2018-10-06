package tuev.konstantin.androidrescuer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.json.JSONException;
import org.json.JSONObject;

import static tuev.konstantin.androidrescuer.MainActivity.TAG;

public class FirebaseInstanceID extends FirebaseInstanceIdService {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: "+this.getClass().getName());
    }

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        if (refreshedToken != null && !refreshedToken.isEmpty()) {
            sendRegistrationToServer(refreshedToken, this, Helper.getPassNPhone(FirebaseInstanceID.this));
        }
    }

    public static void sendRegistrationToServer(final String token, final Context context, PassNPhone passNPhone) {
        SharedPreferences prefs = Helper.sharedPrefs(context);
        Helper.deviceSpecificPrefs(context).edit().putString(Config.FCM_TOKEN, token).apply();
        if (prefs.contains(Config.PHONE_CONTAIN) && prefs.contains(Config.APP_PASS_CONTAIN)) {
            JSONObject json = new JSONObject();
            try {
                json.put("myphone", passNPhone.getPhone());
                json.put("pass", passNPhone.getPass());
                json.put("token", token);
                json.put("test", Config.test);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String url = Helper.getServerUrl(context, Helper.url.REGISTERTOKEN);
            new Helper.CallAPI(url, json.toString(), out -> Log.d(TAG+"FirebaseInstanceID","response: "+out));
        }
    }
}
