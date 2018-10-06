package tuev.konstantin.androidrescuer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import agency.tango.materialintroscreen.SlideFragment;
import agency.tango.materialintroscreen.widgets.OverScrollViewPager;
import agency.tango.materialintroscreen.widgets.SwipeableViewPager;

import static android.app.Activity.RESULT_OK;
import static tuev.konstantin.androidrescuer.MainActivity.TAG;
import static tuev.konstantin.androidrescuer.RegisterLogin.phoneNumber;

public class PermissionsADevAdmin extends SlideFragment {

    private AppCompatButton permissions, devAdminBtn, drawPermission, grantDrive;
    private boolean permissionsOK, permissionDrawOK, devOK, driveOK = false;
    private TextView devAdmin, perm, title, googleDesc;
    private boolean goodTogo = false;
    public DevicePolicyManager devicePolicyManager;
    public ComponentName demoDeviceAdmin;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.perm_admin, container, false);
        perm = view.findViewById(R.id.perm_desc);
        devAdmin = view.findViewById(R.id.dev_admin_desc);
        title = view.findViewById(R.id.first_title);
        permissions = view.findViewById(R.id.grant_perm);
        grantDrive = view.findViewById(R.id.grant_google_acc);
        googleDesc = view.findViewById(R.id.google_acc_desc);
        Glide.with(this).load(Helper.getServerImageUrl(getContext(), "permission.png")).into((ImageView) view.findViewById(R.id.permisson_img));
        grantDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HideGoogleDrive();
                IntroActivity activity = (IntroActivity) getActivity();

                activity.mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                        .addApi(Drive.API)
                        .addScope(Drive.SCOPE_FILE)
                        .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(@Nullable Bundle bundle) {

                            }

                            @Override
                            public void onConnectionSuspended(int i) {

                            }
                        })
                        .addOnConnectionFailedListener(connectionResult -> {
                            if (connectionResult.hasResolution()) {
                                try {
                                    connectionResult.startResolutionForResult(getActivity(), IntroActivity.PLAY_SERVICES_RESOLUTION_REQUEST);
                                } catch (IntentSender.SendIntentException e) {
                                    // Unable to resolve, message user appropriately
                                }
                            } else {
                                GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
                                apiAvailability.getErrorDialog(getActivity(), connectionResult.getErrorCode(), IntroActivity.PLAY_SERVICES_RESOLUTION_REQUEST).show();
                            }
                            Log.d(TAG, "onConnectionFailed: " + connectionResult.toString());
                        })
                        .build();
                activity.mGoogleApiClient.connect();
            }
        });
        permissions.setOnClickListener(view12 -> {
            Log.d(TAG, "onClick: ");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA},
                        23);
            } else {
                permissionsOK = true;
                new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                        .setTitle(R.string.perm_granted)
                        .setPositiveButton("OK", (dialogInterface, i) -> HidePerm())
                        .setOnCancelListener(dialogInterface -> HidePerm()).show();
            }
        });
        drawPermission = view.findViewById(R.id.grant_draw_over);
        drawPermission.setOnClickListener(v -> {
            if (!HiddenCameraUtils.canOverDrawOtherApps(getContext())) {
                HiddenCameraUtils.openDrawOverPermissionSetting(PermissionsADevAdmin.this.getActivity());
            } else {
                permissionDrawOK = true;
                new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                        .setTitle(R.string.draw_over_perm)
                        .setPositiveButton("OK", (dialogInterface, i) -> HidePermDraw())
                        .setOnCancelListener(dialogInterface -> HidePermDraw()).show();
            }
        });
        devAdminBtn = view.findViewById(R.id.grant_dev_admin);
        devAdminBtn.setOnClickListener(view1 -> {
            devicePolicyManager = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
            demoDeviceAdmin = new ComponentName(getActivity(), Admin.class);
            if (devicePolicyManager == null || !devicePolicyManager.isAdminActive(demoDeviceAdmin)) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, demoDeviceAdmin);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.why_dev_admin));
                getActivity().startActivityForResult(intent, 1134);
            }
            if (devicePolicyManager != null && devicePolicyManager.isAdminActive(demoDeviceAdmin)) {
                new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                        .setTitle(R.string.app_is_admin)
                        .setPositiveButton("OK", (dialogInterface, i) -> HideDevAdmin())
                        .setOnCancelListener(dialogInterface -> HideDevAdmin()).show();
                devOK = true;
            }
        });
        if (goodTogo) {
            HideDevAdmin();
            HidePerm();
            HidePermDraw();
            title.append("\n"+getString(R.string.evr_ready));
        }
        return view;
    }

    public static boolean areAllGranted(int... grantResults) {
        for(int res : grantResults) if(res != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }

    public boolean allDontAsk(String... permissions) {
        for (String perm : permissions) if (shouldShowRequestPermissionRationale(perm)) return false;
        return true;
    }

    @SuppressLint("HardwareIds")
    public void PermissionResults(@NonNull String[] permissions1,
                                  @NonNull int[] grantResults) {
        if (!areAllGranted(grantResults)) {
            final boolean rationale = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
            if (rationale) {
                new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                        .setTitle(getString(R.string.loca_req))
                        .setMessage(getString(R.string.its_used_for) + " " + getString(R.string.loc_descr) + "\n"+getString(R.string.enable_in_settings))
                        .setPositiveButton(R.string.ena_in_sett_short, (dialogInterface, i) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                            intent.setData(uri);
                            getActivity().startActivityForResult(intent, 52);
                        }).setCancelable(false).show();
            } else {
                if (allDontAsk(permissions1)) {
                    permissionsOK = true;
                    new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme)
                            .setTitle(R.string.perm_set_finished)
                            .setPositiveButton("OK", (dialogInterface, i) -> HidePerm())
                            .setOnCancelListener(dialogInterface -> HidePerm()).show();
                } else {
                    boolean canContinue = true;
                    SpannableStringBuilder responsetoUsr = new SpannableStringBuilder();
                    responsetoUsr.append(getString(R.string.why_req)).append("\n");
                    for (int i = 0; i < permissions1.length; i++) {
                        String permission = permissions1[i];
                        int result = grantResults[i];
                        if (result == PackageManager.PERMISSION_DENIED) {
                            if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                responsetoUsr = permission(responsetoUsr, getString(R.string.loca_req), getString(R.string.loc_descr));
                                canContinue = false;
                            }
                            if (permission.equals(Manifest.permission.SEND_SMS)) {
                                responsetoUsr = permission(responsetoUsr, getString(R.string.send_sms), getString(R.string.send_sms_desc));
                            }
                            if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                responsetoUsr = permission(responsetoUsr, getString(R.string.ext_storage), getString(R.string.ext_storage_desc));
                            }
                            if (permission.equals(Manifest.permission.READ_CONTACTS)) {
                                responsetoUsr = permission(responsetoUsr, getString(R.string.read_cont), getString(R.string.read_cont_desc));
                            }
                            if (permission.equals(Manifest.permission.CAMERA)) {
                                responsetoUsr = permission(responsetoUsr, getString(R.string.camera), getString(R.string.camera_desc));
                            }
                        }
                        if (permission.equals(Manifest.permission.READ_PHONE_STATE)) {
                            if (result == PackageManager.PERMISSION_DENIED) {
                                responsetoUsr = permission(responsetoUsr, getString(R.string.read_phone_state), getString(R.string.read_phone_state_desc));
                            } else {
                                TelephonyManager telephonyManager = (TelephonyManager)
                                        getActivity().getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                                try {
                                    if (telephonyManager != null) {
                                        phoneNumber = telephonyManager.getLine1Number();
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                                if (phoneNumber != null && RegisterLogin.phone != null && RegisterLogin.phone.getEditText() != null && !phoneNumber.contains("?") && PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
                                    RegisterLogin.phone.getEditText().setText(phoneNumber);
                                }
                            }
                        }
                    }
                    Log.d(TAG, "PermissionResults: response: " + responsetoUsr);
                    AlertDialog.Builder ad = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                            .setTitle(R.string.not_all_perm)
                            .setMessage(responsetoUsr);
                    if (canContinue) {
                        ad.setNegativeButton("Cancel", (dialogInterface, i) -> permissionsOK = true);
                    }
                    ad.setPositiveButton(R.string.re_grant_all, (dialogInterface, i) -> permissions.callOnClick());
                    ad.setCancelable(false);
                    AlertDialog res = ad.create();
                    res.show();
                    TextView TV = res.findViewById(android.R.id.message);
                    if (TV != null) {
                        TV.setMovementMethod(LinkMovementMethod.getInstance());
                    }
                }
            }
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.perm_granted)
                    .setPositiveButton("OK", (dialogInterface, i) -> HidePerm())
                    .setOnCancelListener(dialogInterface -> HidePerm()).show();
            TelephonyManager telephonyManager = (TelephonyManager)
                    getActivity().getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            try {
                if (telephonyManager != null) {
                    phoneNumber = telephonyManager.getLine1Number();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (phoneNumber != null && RegisterLogin.phone != null && RegisterLogin.phone.getEditText() != null && !phoneNumber.contains("?") && PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
                RegisterLogin.phone.getEditText().setText(phoneNumber);
            }
            permissionsOK = true;
        }
    }

    private void HidePerm() {
        permissionsOK = true;
        permissions.setVisibility(View.GONE);
        if (permissionDrawOK) {
            perm.setVisibility(View.GONE);
            if (devOK && driveOK) {
                goodTogo = true;
                title.append("\n"+getString(R.string.evr_ready));
            }
        }
    }
    public void HidePermDraw() {
        permissionDrawOK = true;
        drawPermission.setVisibility(View.GONE);
        if (permissionsOK) {
            perm.setVisibility(View.GONE);
            if (devOK && driveOK) {
                goodTogo = true;
                title.append("\n"+getString(R.string.evr_ready));
            }
        }
    }
    public void HideGoogleDrive() {
        driveOK = true;
        if (permissionsOK && devOK && permissionDrawOK) {
            goodTogo = true;
            title.append("\n"+getString(R.string.evr_ready));
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void SettingsEnd() {
        Log.d(TAG, "SettingsEnd: ");
        OverScrollViewPager overScrollLayout = getActivity().findViewById(agency.tango.materialintroscreen.R.id.view_pager_slides);
        SwipeableViewPager viewPager = overScrollLayout.getOverScrollView();
        if (viewPager != null) {
            viewPager.setBackgroundColor(getResources().getColor(R.color.settings_slide));
        }
        tintButtons();
        if (getContext() != null && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            permissionsOK = true;
            if (hasPermissions(getContext(), Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA)) {
                HidePerm();
            }
        } else {
            new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme)
                    .setTitle(getString(R.string.loca_req))
                    .setMessage(getString(R.string.its_used_for) + " " + getString(R.string.loc_descr) + "\n"+getString(R.string.enable_in_settings))
                    .setPositiveButton(R.string.ena_in_sett_short, (dialogInterface, i) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                        intent.setData(uri);
                        getActivity().startActivityForResult(intent, 52);
                    }).setCancelable(false).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionResults(permissions, grantResults);
    }

    public void HideDevAdmin() {
        devOK = true;
        devAdmin.setVisibility(View.GONE);
        devAdminBtn.setVisibility(View.GONE);
        if (permissionsOK && permissionDrawOK && driveOK) {
            goodTogo = true;
            title.append("\n"+getString(R.string.evr_ready));
        }
    }

    public SpannableStringBuilder permission(SpannableStringBuilder out, final String permission, final String description) {
        SpannableString ss = new SpannableString(getString(R.string.why));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                        .setTitle(permission)
                        .setMessage(getString(R.string.its_used_for) + " " + description)
                        .setPositiveButton("OK", (dialogInterface, i) -> {

                        }).show();
            }
        };
        ss.setSpan(clickableSpan, 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.append("   ⚪")
                .append(permission)
                .append(" - ")
                .append(ss)
                .append("\n");
        return out;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        goodTogo = savedInstanceState != null && savedInstanceState.getBoolean("goodToGo", false);
        Log.d(TAG, "onCreate: goodToGo: "+goodTogo);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("goodToGo", goodTogo);
        Log.d(TAG, "onSaveInstanceState: goodToGo: "+goodTogo);
        super.onSaveInstanceState(outState);
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
        return getString(R.string.need_of_perm_dev_admin);
    }

    @Override
    public boolean hasNeededPermissionsToGrant() {
        return false;
    }

    private void tintButtons() {
        ColorStateList color = ColorStateList.valueOf(getResources().getColor(R.color.buttons_settings));
        ImageButton backButton = getActivity().findViewById(agency.tango.materialintroscreen.R.id.button_back);
        ImageButton nextButton = getActivity().findViewById(agency.tango.materialintroscreen.R.id.button_next);
        ImageButton skipButton = getActivity().findViewById(agency.tango.materialintroscreen.R.id.button_skip);
        ViewCompat.setBackgroundTintList(nextButton, color);
        ViewCompat.setBackgroundTintList(backButton, color);
        ViewCompat.setBackgroundTintList(skipButton, color);
    }
}
