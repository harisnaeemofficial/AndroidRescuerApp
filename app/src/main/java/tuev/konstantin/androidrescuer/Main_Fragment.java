package tuev.konstantin.androidrescuer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.android.ex.chips.recipientchip.DrawableRecipientChip;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

import eu.chainfire.libsuperuser.Shell;

import static tuev.konstantin.androidrescuer.MainActivity.TAG;

public class Main_Fragment extends BaseFragment {

    boolean keyShown = false;
    private MainActivity mainActivity;
    private BroadcastReceiver sent;

    @Override
    public void onKeyboardShow() {
        keyShown = true;
        if (!landscape) {
            send.animate()
                    .translationY(send.getHeight() + 10)
                    .alpha(0.0f)
                    .setDuration(getResources().getInteger(R.integer.defAnimationDuration))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            send.setVisibility(View.GONE);
                        }
                    });
        }
    }

    @Override
    public void onKeyboardHide() {
        keyShown = false;
        if (!landscape) {
            send.setVisibility(View.VISIBLE);
            send.setAlpha(0.0f);
            send.animate()
                    .translationY(0)
                    .alpha(1.0f)
                    .setDuration(getResources().getInteger(R.integer.defAnimationDuration))
                    .setListener(null);
        }
    }

    View send;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setRetainInstance(false);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragment = inflater.inflate(R.layout.main_fragment, container, false);

        mainActivity = (MainActivity) getActivity();
        send = fragment.findViewById(R.id.send);
        if (landscape) {
            send.setVisibility(View.GONE);
        } else {
            send.setOnClickListener(view -> send());
        }
        fragment.findViewById(R.id.send).setOnClickListener(view -> send());

        return fragment;
    }

    private void getTag(String tag, int item, JSONObject target) throws JSONException {
        if (item == 1) {
            target.put(tag, "1");
        }
        if (item == 2) {
            target.put(tag, "0");
        }
    }

    private String getTag(String tag, int item, String target) {
        if (item == 1) {
            target += tag + ":1\n";
        }
        if (item == 2) {
            target += tag + ":0\n";
        }
        return target;
    }

    @SuppressLint("ResourceType")
    public void send() {
        DrawableRecipientChip[] chips = mainActivity.phoneRetv.getSortedRecipients();
        String appPass = mainActivity.appPass.getText().toString();
        if (chips.length != 0 && chips[0] != null && !appPass.isEmpty()) {
            String phone = chips[0].getEntry().getDestination().replaceAll("[^0-9+]", "");
            RadioGroup radioGroup = new RadioGroup(getContext());
            RadioButton internet = new RadioButton(getContext());
            internet.setText(R.string.via_net);
            internet.setId(0);
            RadioButton sms = new RadioButton(getContext());
            sms.setText(R.string.via_sms);
            sms.setId(1);
            radioGroup.addView(sms);
            radioGroup.addView(internet);
            new AlertDialog.Builder(getContext())
                    .setTitle("Send:")
                    .setView(radioGroup)
                    .setPositiveButton("OK", (dialogInterface, i) -> {
                        switch (radioGroup.getCheckedRadioButtonId()) {
                            case 0:
                                sendNet(phone, appPass);
                                break;
                            case 1:
                                sendSms(phone, appPass);
                                break;
                        }
                    })
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    })
                    .show();
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Empty fields!")
                    .setPositiveButton("OK", (dialog, which) -> {
                        mainActivity.tb.setChecked(true);
                    })
                    .setOnCancelListener(dialog -> {
                        mainActivity.tb.setChecked(true);
                    }).show();
        }
    }

    Integer countDeliver = null;
    Integer countSend = null;

    private void sendSms(String phone, String appPass) {
        countDeliver = null;
        countSend = null;
        SharedPreferences prefs = ControlRow.getPrefs(getContext());
        Map<String, ?> items = prefs.getAll();
        try {
            @SuppressLint("InflateParams") View material_progress_dialog = LayoutInflater.from(mainActivity).inflate(R.layout.material_progress_dialog, null);
            final AlertDialog progress = new AlertDialog.Builder(mainActivity)
                    .setView(material_progress_dialog)
                    .setCancelable(false)
                    .show();
            boolean realtimeLoc = false;
            if (items.containsKey("realTimeLoc")) {
                realtimeLoc = ((Integer) items.get("realTimeLoc")) == 1;
            }
            String out = "ctrl_sms\n";
            out += "pass:"+appPass+"\n";
            for (String key : items.keySet()) {
                boolean equal = key.equals("wrongPassLocation");
                if (!equal || (equal && !realtimeLoc)) {
                    Object value = items.get(key);
                    if (value instanceof Integer) {
                        int item = (int) value;
                        out = getTag(key, item, out);
                    }
                }
            }

            Log.d(TAG, "sendSms: out: "+out);
            delivered = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Runnable runnable = () -> {
                        Log.d(TAG, "onReceive: delivered");
                        String message = getString(R.string.message_delivery_error);
                        if (getResultCode() == Activity.RESULT_OK) {
                            message = getString(R.string.sms_delivered);
                        }
                        new AlertDialog.Builder(getActivity())
                                .setTitle(message)
                                .setPositiveButton("OK", (dialog, which) -> {}).show();
                        getContext().unregisterReceiver(delivered);
                    };
                    Log.d(TAG, "onReceive: countDeliver: "+countDeliver);
                    if (countDeliver != null) {
                        countDeliver -= 1;
                        if (countDeliver <= 0) {
                            runnable.run();
                        }
                    } else {
                        runnable.run();
                    }
                }
            };
            sent = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Runnable runnable = () -> {
                        Log.d(TAG, "onReceive: sent");
                        String message = null;
                        switch (getResultCode()) {
                            case Activity.RESULT_OK:
                                message = getString(R.string.sms_sent);
                        }
                        if (message != null) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(message)
                                    .setPositiveButton("OK", (dialog, which) -> {})
                                    .show();
                        }
                        getContext().unregisterReceiver(sent);
                    };
                    Log.d(TAG, "onReceive: countSend: "+countSend);
                    if (countSend != null) {
                        countSend -= 1;
                        if (countSend <= 0) {
                            runnable.run();
                        }
                    } else {
                        runnable.run();
                    }
                }
            };
            getContext().registerReceiver(sent, new IntentFilter("tuev.konstantin.androidrescuer.SENT_SMS"));
            getContext().registerReceiver(delivered, new IntentFilter("tuev.konstantin.androidrescuer.DELIVERED_SMS"));

            Intent intent = new Intent("tuev.konstantin.androidrescuer.DELIVERED_SMS");

            Intent intentSend = new Intent("tuev.konstantin.androidrescuer.SENT_SMS");

            ArrayList<String> parts = SmsManager.getDefault().divideMessage(out);
            if (parts.size() == 1) {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 45, intent, PendingIntent.FLAG_ONE_SHOT);
                PendingIntent pendingIntentSend = PendingIntent.getBroadcast(getContext(), 47, intentSend, PendingIntent.FLAG_ONE_SHOT);

                SmsManager.getDefault().sendTextMessage(phone, null, out, pendingIntentSend, pendingIntent);
            } else {
                countDeliver = parts.size();
                countSend = parts.size();
                ArrayList<PendingIntent> sentPendingIntents = new ArrayList<>();
                ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<>();
                for (int i = 0; i < parts.size(); i++) {
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 45+i, intent, PendingIntent.FLAG_ONE_SHOT);
                    PendingIntent pendingIntentSend = PendingIntent.getBroadcast(getContext(), 47+i, intentSend, PendingIntent.FLAG_ONE_SHOT);
                    sentPendingIntents.add(i, pendingIntentSend);

                    deliveredPendingIntents.add(i, pendingIntent);
                }
                SmsManager.getDefault().sendMultipartTextMessage(phone, null, parts, sentPendingIntents, deliveredPendingIntents);
            }
            progress.cancel();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    BroadcastReceiver delivered = null;

    private void sendNet(String phone, String appPass) {
        SharedPreferences prefs = ControlRow.getPrefs(getContext());
        Map<String, ?> items = prefs.getAll();
        JSONObject json = new JSONObject();
        try {

            @SuppressLint("InflateParams") View material_progress_dialog = LayoutInflater.from(mainActivity).inflate(R.layout.material_progress_dialog, null);
            final AlertDialog progress = new AlertDialog.Builder(mainActivity)
                    .setView(material_progress_dialog)
                    .setCancelable(false)
                    .show();
            boolean realtimeLoc = false;
            if (items.containsKey("realTimeLoc")) {
                realtimeLoc = ((Integer) items.get("realTimeLoc")) == 1;
            }
            json.put("phone", phone);
            json.put("pass", appPass);
            JSONObject dataJ = new JSONObject();
            for (String key : items.keySet()) {
                boolean equal = key.equals("wrongPassLocation");
                if (!equal || (equal && !realtimeLoc)) {
                    Object value = items.get(key);
                    if (value instanceof Integer) {
                        int item = (int) value;
                        getTag(key, item, dataJ);
                    }
                }
            }
            String dataStr = dataJ.toString();
            dataStr = dataStr.substring(dataStr.indexOf('{') + 1, dataStr.lastIndexOf('}'));
            json.put("data", dataStr);
            json.put("test", Config.test);
            new Helper.CallAPI(Helper.getServerUrl(getContext(), Helper.url.THENETMESSAGETHING), json, out -> {
                try {
                    String responseText = out.getString(Config.ResponseJson.TEXT.toString());
                    Boolean responseError = out.getBoolean(Config.ResponseJson.ERROR.toString());
                    progress.cancel();
                    if (responseError) {
                        new AlertDialog.Builder(mainActivity)
                                .setTitle(R.string.error)
                                .setMessage(responseText)
                                .setPositiveButton("OK", (dialog, which) -> {}).show();
                    } else {
                        new AlertDialog.Builder(mainActivity)
                                .setTitle(R.string.success_message)
                                .setPositiveButton("OK", (dialog, which) -> {}).show();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
