package tuev.konstantin.androidrescuer;

import android.Manifest;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import agency.tango.materialintroscreen.SlideFragment;
import agency.tango.materialintroscreen.parallax.ParallaxLinearLayout;

import static tuev.konstantin.androidrescuer.FirebaseInstanceID.sendRegistrationToServer;
import static tuev.konstantin.androidrescuer.Helper.getPrefs;
import static tuev.konstantin.androidrescuer.MainActivity.TAG;

@SuppressWarnings("ConstantConditions")
public class RegisterLogin extends SlideFragment {

    public static TextInputLayout phone;
    private TextInputLayout pass, mail;
    private Button registerLogin;
    private TextView login, forgottenPass, explainFields, title;
    private boolean register = false;
    private boolean goodTogo = false;
    private Activity activity;
    public static String phoneNumber = null;
    private SharedPreferences prefs;
    private LinearLayout controlsContainer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        prefs = Helper.sharedPrefs(activity);
        if (savedInstanceState != null) {
            goodTogo = savedInstanceState.getBoolean("goodtToGo", false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //outState.putBoolean("restore", true);
        outState.putBoolean("goodToGo", goodTogo);
        super.onSaveInstanceState(outState);
    }

    public PassNPhone passNPhone;

    @SuppressLint("HardwareIds")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ParallaxLinearLayout root = (ParallaxLinearLayout) inflater.inflate(R.layout.register_login, container, false);

        controlsContainer = root.findViewById(R.id.controls_container);
        phone = root.findViewById(R.id.phone);
        pass = root.findViewById(R.id.password);
        mail = root.findViewById(R.id.mail);
        registerLogin = root.findViewById(R.id.registerLogin);
        login = root.findViewById(R.id.login);
        forgottenPass = root.findViewById(R.id.forgotten_pass);
        explainFields = root.findViewById(R.id.explain_fields);
        title = root.findViewById(R.id.first_title);
        Glide.with(this).load(Helper.getServerImageUrl(getContext(), "register_login.png")).into((ImageView) root.findViewById(R.id.register_login));
        TelephonyManager telephonyManager = (TelephonyManager)
                getActivity().getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        passNPhone = Helper.getPassNPhone(getContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            LayoutTransition transition = new LayoutTransition();
            transition.enableTransitionType(LayoutTransition.CHANGING);
            root.setLayoutTransition(transition);
        }
        
        if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                if (telephonyManager != null) {
                    phoneNumber = telephonyManager.getLine1Number();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (phoneNumber != null && !phoneNumber.contains("?") && PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
                phone.getEditText().setText(phoneNumber);
            } else {
                if (prefs.contains(Config.PHONE_CONTAIN)) {
                    phoneNumber = passNPhone.getPhone();
                }
            }
        }

        login.setOnClickListener(v -> toggleLoginRegister());
        toggleLoginRegister();
        if (prefs.contains(Config.APP_PASS_CONTAIN) && prefs.contains(Config.MAIL) && prefs.contains(Config.PHONE_CONTAIN)) {
            String mail = prefs.getString(Config.MAIL, "");
            String mailOut = mail.split("@")[0];
            String name = mailOut.substring(0,1).toUpperCase() + mailOut.substring(1).toLowerCase();
            String text = getString(R.string.welcome) + " " + name +"!";
            title.setText(text);
            controlsContainer.setVisibility(View.GONE);
            goodTogo = true;
        }
        //goodTogo = true;
        registerLogin.setOnClickListener(v -> {
            final String phone = RegisterLogin.phone.getEditText().getText().toString().replace(" ", "");
            RegisterLogin.phone.getEditText().setText(phone);
            final String pass = RegisterLogin.this.pass.getEditText().getText().toString();
            Log.d(TAG, "onClick: register: "+register);
            @SuppressLint("InflateParams") View material_progress_dialog = LayoutInflater.from(activity).inflate(R.layout.material_progress_dialog, null);
            final AlertDialog prggressDialog = new AlertDialog.Builder(activity)
                    .setView(material_progress_dialog)
                    .setCancelable(false)
                    .create();
            if (register) {
                final String mail = RegisterLogin.this.mail.getEditText().getText().toString();
                if (phone.length() < 8 || !PhoneNumberUtils.isGlobalPhoneNumber(phone)) {
                    RegisterLogin.phone.setError(getString(R.string.incorrect_phone));
                    RegisterLogin.phone.setErrorEnabled(true);
                    RegisterLogin.phone.getEditText().getBackground().setColorFilter(getResources().getColor(R.color.red_500_primary), PorterDuff.Mode.SRC_ATOP);
                    return;
                } else {
                    RegisterLogin.phone.setErrorEnabled(false);
                    RegisterLogin.phone.getEditText().getBackground().clearColorFilter();
                }
                if (pass.length() < 8) {
                    RegisterLogin.this.pass.setError(getString(R.string.short_pass));
                    RegisterLogin.this.pass.setErrorEnabled(true);
                    RegisterLogin.this.pass.getEditText().getBackground().setColorFilter(getResources().getColor(R.color.red_500_primary), PorterDuff.Mode.SRC_ATOP);
                    return;
                } else {
                    RegisterLogin.this.pass.setErrorEnabled(false);
                    RegisterLogin.this.pass.getEditText().getBackground().clearColorFilter();
                }
                if (mail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
                    RegisterLogin.this.mail.setError(getString(R.string.incorrect_mail));
                    RegisterLogin.this.mail.setErrorEnabled(true);
                    RegisterLogin.this.mail.getEditText().getBackground().setColorFilter(getResources().getColor(R.color.red_500_primary), PorterDuff.Mode.SRC_ATOP);
                    return;
                } else {
                    RegisterLogin.this.mail.setErrorEnabled(false);
                    RegisterLogin.this.mail.getEditText().getBackground().clearColorFilter();
                }
                prggressDialog.show();
                String fcm_token = Helper.deviceSpecificPrefs(activity).getString(Config.FCM_TOKEN, null);
                Log.d(TAG, "onClick: Went to json!");
                JSONObject json = new JSONObject();
                try {
                    json.put("phone", phone);
                    json.put("pass", pass);
                    json.put("mail", mail);
                    if (fcm_token != null) {
                        json.put("fcm_token", fcm_token);
                    }
                    json.put("test", Config.test);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String url = Helper.getServerUrl(activity.getApplicationContext(), Helper.url.REGISTER);
                Log.d(TAG, "onClick: json: "+json+" url: "+url);
                Log.d(TAG, "sendRegistrationToServer: url: "+url+" json: "+json.toString());
                new Helper.CallAPI(url, json.toString(), out -> {
                    try {
                        String responseText = out.getString(Config.ResponseJson.TEXT.toString());
                        Boolean responseError = out.getBoolean(Config.ResponseJson.ERROR.toString());
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        if (responseError) {
                            builder.setTitle(R.string.error);
                            builder.setMessage(responseText);
                        } else {
                            prefs.edit().putString(Config.MAIL, mail).apply();
                            Helper.putPassNPhone(getContext(), pass, phone);
                            builder.setTitle(R.string.reg_success);
                            goodTogo = true;
                            title.setText(R.string.reg_success);
                            controlsContainer.setVisibility(View.GONE);
                        }
                        prggressDialog.cancel();
                        builder.setPositiveButton("OK", (dialog, which) -> {

                        }).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                //TODO: write explanations(maybe fix icon animation)
                prggressDialog.show();
                JSONObject json = new JSONObject();
                try {
                    json.put("phone", phone);
                    json.put("pass", pass);
                    json.put("test", Config.test);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                final String[] mail = {""};
                String url = Helper.getServerUrl(activity.getApplicationContext(), Helper.url.LOGIN);
                Log.d(TAG, "onClick: json: "+json+" url: "+url);
                Log.d(TAG, "sendRegistrationToServer: url: "+url+" json: "+json.toString());
                new Helper.CallAPI(url, json.toString(), out -> {
                    try {
                        String responseText = out.getString(Config.ResponseJson.TEXT.toString());
                        Boolean responseError = out.getBoolean(Config.ResponseJson.ERROR.toString());
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        if (responseError) {
                            builder.setTitle(R.string.error);
                            builder.setMessage(responseText);
                        } else {
                            mail[0] = responseText;
                            prefs.edit().putString(Config.MAIL, mail[0]).apply();
                            Helper.putPassNPhone(getContext(), pass, phone);
                            String mailOut = mail[0].split("@")[0];
                            String name = mailOut.substring(0,1).toUpperCase() + mailOut.substring(1).toLowerCase();
                            String text = getString(R.string.welcome) + " " + name +"!";
                            builder.setTitle(text);
                            title.setText(text);
                            controlsContainer.setVisibility(View.GONE);
                            String refreshedToken = FirebaseInstanceId.getInstance().getToken();
                            if (refreshedToken == null || refreshedToken.isEmpty()) {
                                refreshedToken = getPrefs(getContext()).getString(Config.FCM_TOKEN, null);
                            }
                            if (refreshedToken != null && !refreshedToken.isEmpty()) {
                                sendRegistrationToServer(refreshedToken, getContext(), new PassNPhone(pass, phone));
                            }
                            goodTogo = true;
                        }
                        prggressDialog.cancel();
                        builder.setPositiveButton("OK", (dialog, which) -> {

                        }).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
        forgottenPass.setOnClickListener(v -> {
            @SuppressLint("InflateParams") View material_progress_dialog = LayoutInflater.from(activity).inflate(R.layout.material_progress_dialog, null);
            final AlertDialog prggressDialog = new AlertDialog.Builder(activity)
                    .setView(material_progress_dialog)
                    .setCancelable(false)
                    .create();
            if (!prefs.getBoolean("sentForgot", false)) {
                final TextInputLayout phoneNumberLayout = new TextInputLayout(activity);
                final AppCompatEditText phoneNumber = new AppCompatEditText(activity);
                phoneNumber.setHint(R.string.your_phone_number);
                phoneNumber.setTextColor(getResources().getColor(android.R.color.white));
                phoneNumber.setTextAppearance(activity, R.style.TextAppearance_AppCompat_Subhead);
                phoneNumberLayout.addView(phoneNumber, 0, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                final TextView errorTV = new TextView(activity);
                errorTV.setGravity(Gravity.CENTER);
                errorTV.setTextAppearance(activity, R.style.TextAppearance_AppCompat_Subhead);
                errorTV.setTextColor(getResources().getColor(R.color.red_500_primary));
                phoneNumberLayout.addView(errorTV, 1, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                if (RegisterLogin.phoneNumber != null) {
                    phoneNumber.setText(RegisterLogin.phoneNumber);
                }
                final AlertDialog dialog = new AlertDialog.Builder(activity)
                        .setTitle("Forgotten password")
                        .setView(phoneNumberLayout)
                        .setMessage("Send verification code to the email associated with the phone specified and change the password here.")
                        .setPositiveButton("OK", (dialog13, which) -> {

                        })
                        .setNegativeButton("Cancel", (dialog12, which) -> {

                        }).create();
                dialog.show();
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v12 -> {
                    String phone = phoneNumber.getText().toString();
                    boolean canContinue = true;
                    if (phone.length() < 8 || !PhoneNumberUtils.isGlobalPhoneNumber(phone)) {
                        phoneNumberLayout.setError(getString(R.string.incorrect_phone));
                        phoneNumberLayout.setErrorEnabled(true);
                        phoneNumber.getBackground().setColorFilter(getResources().getColor(R.color.red_500_primary), PorterDuff.Mode.SRC_ATOP);
                        canContinue = false;
                    } else {
                        phoneNumberLayout.setErrorEnabled(false);
                        phoneNumber.getBackground().clearColorFilter();
                    }
                    if (canContinue) {
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                        prggressDialog.show();
                        prefs.edit().putString(Config.PHONE_CONTAIN, phone).apply();
                        JSONObject json = new JSONObject();
                        try {
                            json.put("phone", phone);
                            json.put("test", Config.test);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String url = Helper.getServerUrl(activity, Helper.url.FORGOTTENPASSWORD);
                        Log.d(TAG, "sendRegistrationToServer: url: " + url + " json: " + json.toString());
                        new Helper.CallAPI(url, json.toString(), out -> {
                            try {
                                boolean error = out.getBoolean("error");
                                String result = out.getString("result");
                                Log.d(TAG, "ready: error: "+error+" result: "+result);
                                if (!error) {
                                    prefs.edit().putBoolean("sentForgot", true).apply();
                                    dialog.cancel();
                                    new AlertDialog.Builder(activity)
                                            .setTitle(R.string.sent_verify)
                                            .setPositiveButton("OK", (dialog1, which) -> forgottenPass.callOnClick()).show();
                                } else {
                                    errorTV.setText(result);
                                    prefs.edit().putBoolean("sentForgot", false).apply();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                errorTV.setText(R.string.no_net_error);
                            }
                            Log.d(TAG, "ready: " + "response: " + out);
                            prggressDialog.cancel();
                        });
                    }
                });
            } else {
                @SuppressLint("InflateParams") View new_pass = LayoutInflater.from(activity).inflate(R.layout.new_pass, null);
                final TextInputLayout code = new_pass.findViewById(R.id.code);
                final TextInputLayout newPass = new_pass.findViewById(R.id.newPass);
                final TextInputLayout newPassVerify = new_pass.findViewById(R.id.newPassVerify);
                final TextView errorTV = new_pass.findViewById(R.id.error_tv);
                final AlertDialog newPassDialog = new AlertDialog.Builder(activity)
                        .setView(new_pass)
                        .setPositiveButton("OK", (dialog, which) -> {

                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {

                        })
                        .setCancelable(false)
                        .create();
                newPassDialog.show();
                newPassDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                    String codeText = code.getEditText().getText().toString();
                    String newPassText = newPass.getEditText().getText().toString();
                    String newPassVerifyText = newPassVerify.getEditText().getText().toString();
                    boolean canContinue = true;
                    if (newPassText.length() < 8) {
                        newPass.setError(getString(R.string.short_pass));
                        newPass.setErrorEnabled(true);
                        newPass.getEditText().getBackground().setColorFilter(getResources().getColor(R.color.red_500_primary), PorterDuff.Mode.SRC_ATOP);
                        canContinue = false;
                    } else {
                        newPass.setErrorEnabled(false);
                        newPass.getEditText().getBackground().clearColorFilter();
                    }
                    if (canContinue) {
                        if (!newPassText.equals(newPassVerifyText)) {
                            newPassVerify.setError(getString(R.string.no_match_pass));
                            newPassVerify.setErrorEnabled(true);
                            newPassVerify.getEditText().getBackground().setColorFilter(getResources().getColor(R.color.red_500_primary), PorterDuff.Mode.SRC_ATOP);
                            canContinue = false;
                        } else {
                            newPassVerify.setErrorEnabled(false);
                            newPassVerify.getEditText().getBackground().clearColorFilter();
                        }
                    }
                    if (canContinue) {
                        if (codeText.length() != 4) {
                            code.setError(getString(R.string.lenght_not_4));
                            code.setErrorEnabled(true);
                            code.getEditText().getBackground().setColorFilter(getResources().getColor(R.color.red_500_primary), PorterDuff.Mode.SRC_ATOP);
                            canContinue = false;
                        } else {
                            code.setErrorEnabled(false);
                            code.getEditText().getBackground().clearColorFilter();
                        }
                    }
                    if (canContinue) {
                        String phone = prefs.getString(Config.PHONE_CONTAIN, "");
                        JSONObject json = new JSONObject();
                        try {
                            json.put("phone", phone);
                            json.put("code", codeText);
                            json.put("newPass", newPassText);
                            json.put("test", Config.test);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        newPassDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                        prggressDialog.show();
                        String url = Helper.getServerUrl(activity, Helper.url.FORGOTTENPASSWORD);
                        Log.d(TAG, "sendRegistrationToServer: url: " + url + " json: " + json.toString());
                        new Helper.CallAPI(url, json.toString(), out -> {
                            try {
                                boolean error = out.getBoolean("error");
                                String result = out.getString("result");
                                if (!error) {
                                    prefs.edit().putBoolean("sentForgot", false).apply();
                                    newPassDialog.cancel();
                                    new AlertDialog.Builder(activity)
                                            .setTitle("Password changed successfully!")
                                            .setPositiveButton("OK", (dialog, which) -> {

                                            }).show();
                                } else {
                                    errorTV.setText(result);
                                    if (result.contains("Too many wrong codes entered") || result.contains("The code is active one calendar day")) {
                                        prefs.edit().putBoolean("sentForgot", false).apply();
                                        newPassDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                                            newPassDialog.cancel();
                                            forgottenPass.callOnClick();
                                        });
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                errorTV.setText(R.string.no_net_error);
                            }
                            prggressDialog.cancel();
                            Log.d(TAG, "ready: " + "response: " + out);
                        });
                    }
                });
            }
        });
        return root;
    }

    private void toggleLoginRegister() {
        if(register) {
            register = false;
            mail.setVisibility(View.GONE);
            forgottenPass.setVisibility(View.VISIBLE);
            explainFields.setVisibility(View.GONE);
            title.setText(R.string.login);
            registerLogin.setText(R.string.login);
            login.setText(getString(R.string.no_account_register));
        } else {
            register = true;
            mail.setVisibility(View.VISIBLE);
            forgottenPass.setVisibility(View.GONE);
            explainFields.setVisibility(View.VISIBLE);
            title.setText(R.string.register);
            registerLogin.setText(R.string.register);
            login.setText(getString(R.string.already_registred_nlogin));
        }
    }

    @Override
    public int backgroundColor() {
        return R.color.settings_slide;
    }

    @Override
    public int buttonsColor() {
        return R.color.buttons_settings;
    }

    @Override
    public boolean canMoveFurther() {
        return goodTogo;
    }

    @Override
    public String cantMoveFurtherErrorMessage() {
        return getString(R.string.reg_log);
    }

}
