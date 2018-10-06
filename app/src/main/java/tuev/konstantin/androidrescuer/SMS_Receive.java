package tuev.konstantin.androidrescuer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsMessage;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SMS_Receive extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        IHandleControlMessage handleControlMessage = new HandleControlMessage(context.getApplicationContext(), true);
        List<Map.Entry<String, String>> msg = RetrieveMessages(intent);

        if (msg == null || msg.size() <= 0) {
            return;
        }


        Map.Entry<String, String> phoneBody = null;
        for (int i = 0; i < msg.size(); i++) {
            Map.Entry<String, String> phoneBody1 = msg.get(i);
            String body = phoneBody1.getValue();
            if (body.contains("ctrl_sms")) {
                phoneBody = phoneBody1;
                break;
            }
        }

        if (phoneBody != null) {
            Map.Entry<String, String> finalPhoneBody = phoneBody;
            new Thread(() -> {
                String body = finalPhoneBody.getValue();
                boolean rightPass = false;
                for (String line : body.split("\n")) {
                    if (line.startsWith("pass")) {
                        String password = line.split(":")[1];
                        if (Helper.sharedPrefs(context).getString(Config.APP_PASS_CONTAIN, "").equals(password)) {
                            rightPass = true;
                        }
                    }
                }
                if (rightPass) {
                    for (String line : body.split("\n")) {
                        Map.Entry<String, Helper.controlState> entry = Helper.controlState.fromLine(line);
                        if (entry != null) {
                            Helper.handle(handleControlMessage, entry.getKey(), entry.getValue());
                            try {
                                Thread.sleep(1200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            context.sendBroadcast(new Intent("tuev.konstantin.androidrescuer.UpdateSwitches"));
                        }
                    });
                }
            }).start();
        }
    }

    private static List<Map.Entry<String, String>> RetrieveMessages(Intent intent) {
        Map<String, String> msg = null;
        SmsMessage[] msgs;
        Bundle bundle = intent.getExtras();

        if (bundle != null && bundle.containsKey("pdus")) {
            Object[] pdus = (Object[]) bundle.get("pdus");

            if (pdus != null) {
                int nbrOfpdus = pdus.length;
                msg = new HashMap<>(nbrOfpdus);
                msgs = new SmsMessage[nbrOfpdus];

                // There can be multiple SMS from multiple senders, there can be a maximum of nbrOfpdus different senders
                // However, send long SMS of same sender in one message
                for (int i = 0; i < nbrOfpdus; i++) {
                    msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);

                    String originatinAddress = msgs[i].getOriginatingAddress();

                    // Check if index with number exists
                    if (!msg.containsKey(originatinAddress)) {
                        // Index with number doesn't exist
                        // Save string into associative array with sender number as index
                        msg.put(msgs[i].getOriginatingAddress(), msgs[i].getMessageBody());

                    } else {
                        // Number has been there, add content but consider that
                        // msg.get(originatinAddress) already contains sms:sndrNbr:previousparts of SMS,
                        // so just add the part of the current PDU
                        String previousparts = msg.get(originatinAddress);
                        String msgString = previousparts + msgs[i].getMessageBody();
                        msg.put(originatinAddress, msgString);
                    }
                }
            }
        }

        if (msg != null) {
            return new ArrayList<>(msg.entrySet());
        } else {
            return null;
        }
    }
}
