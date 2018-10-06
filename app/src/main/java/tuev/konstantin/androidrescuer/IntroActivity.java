package tuev.konstantin.androidrescuer;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.widget.LinearLayout;

import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.widgets.OverScrollViewPager;

import static tuev.konstantin.androidrescuer.MainActivity.TAG;

public class IntroActivity extends MaterialIntroActivity {
    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 23;
    private CoordinatorLayout coordinatorLayoutPublic;
    private LinearLayout navigationViewPublic;
    public GoogleApiClient mGoogleApiClient;
    private PermissionsADevAdmin permissionsADevAdmin;

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case PLAY_SERVICES_RESOLUTION_REQUEST:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
                break;
            case 3545:
                if (resultCode == RESULT_OK || HiddenCameraUtils.canOverDrawOtherApps(getApplicationContext())) {
                    new AlertDialog.Builder(IntroActivity.this, R.style.AlertDialogTheme)
                            .setTitle(R.string.draw_over_perm)
                            .setPositiveButton("OK", (dialogInterface, i) -> permissionsADevAdmin.HidePermDraw())
                            .setOnCancelListener(dialogInterface -> permissionsADevAdmin.HidePermDraw()).show();
                } else {
                    new AlertDialog.Builder(IntroActivity.this, R.style.AlertDialogTheme)
                            .setTitle(R.string.why_is_perm_req)
                            .setMessage(R.string.draw_over_desc)
                            .setPositiveButton("GRANT", (dialogInterface, i) -> {
                                HiddenCameraUtils.openDrawOverPermissionSetting(IntroActivity.this);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {})
                            .setOnCancelListener(dialogInterface -> {}).show();
                }
            break;
            case 52:
                try {
                    permissionsADevAdmin.SettingsEnd();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            case 1134:
                if (permissionsADevAdmin.devicePolicyManager.isAdminActive(permissionsADevAdmin.demoDeviceAdmin)) {
                    permissionsADevAdmin.HideDevAdmin();
                }
                break;
        }
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            enableLastSlideAlphaExitTransition(true);

            final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());
            getBackButtonTranslationWrapper()
                    .setEnterTranslation((view, percentage) -> view.setTranslationY(px - (percentage * px)));

            coordinatorLayoutPublic = findViewById(agency.tango.materialintroscreen.R.id.coordinator_layout_slide);
            navigationViewPublic = findViewById(agency.tango.materialintroscreen.R.id.navigation_view);
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("important");
        OverScrollViewPager overScrollLayout = findViewById(agency.tango.materialintroscreen.R.id.view_pager_slides);
        overScrollLayout.getOverScrollView().setOffscreenPageLimit(2);

            addSlide(new SlideFragmentBuilder()
                    .backgroundColor(R.color.intro_slide)
                    .buttonsColor(R.color.buttons_intro)
                    .image("ic_launcher_big.png")
                    .title(getString(R.string.letsetapp))
                    .description(getString(R.string.slide_alt))
                    .build());
            addSlide(new SlideFragmentBuilder()
                    .backgroundColor(R.color.locate_slide)
                    .image("one_locate_device.png")
                    .buttonsColor(R.color.buttons_locate)
                    .title(getString(R.string.ld))
                    .description(getString(R.string.locate_slide_desc1))
                    .description(getString(R.string.locate_slide_desc2))
                    .description(getString(R.string.locate_slide_desc3))
                    .build());
            addSlide(new SlideFragmentBuilder()
                    .backgroundColor(R.color.backup_slide)
                    .image("two_backup_data.png")
                    .buttonsColor(R.color.buttons_backup)
                    .title(getString(R.string.backup_data))
                    .description(getString(R.string.backup_slide_desc1))
                    .description(getString(R.string.backup_slide_desc2))
                    .build());
            addSlide(new SlideFragmentBuilder()
                    .backgroundColor(R.color.delete_slide)
                    .image("three_delete_data.png")
                    .buttonsColor(R.color.buttons_delete)
                    .title(getString(R.string.del_data))
                    .description(getString(R.string.delete_slide_desc1))
                    .description(getString(R.string.delete_slide_desc2))
                    .description(getString(R.string.delete_slide_desc3))
                    .build());
            addSlide(new SlideFragmentBuilder()
                    .backgroundColor(R.color.protect_slide)
                    .image("protect_slide.png")
                    .buttonsColor(R.color.buttons_protect)
                    .title(getString(R.string.prot_device))
                    .description(getString(R.string.protect_slide_desc1))
                    .description(getString(R.string.protect_slide_desc2))
                    .description(getString(R.string.protect_slide_desc3))
                    .build());

            //startService(new Intent(this, SimListener.class));

            permissionsADevAdmin = new PermissionsADevAdmin();
            addSlide(permissionsADevAdmin);
            addSlide(new RootAComp());
            addSlide(new RegisterLogin());

            addSlide(new SlideFragmentBuilder()
                    .backgroundColor(R.color.finish_slide)
                    .image("ic_launcher_big.png")
                    .buttonsColor(R.color.buttons_delete)
                    .title("Setup finished")
                    .description("Start protection!")
                    .build());
        if (Helper.sharedPrefs(getApplicationContext()).getBoolean("first", true)) {
            new File(Environment.getExternalStorageDirectory() + "/Rescue/data").mkdirs();
            String urlString = Helper.getServerUrl(getApplicationContext(), Helper.url.BETATESTERS);
            new Helper.CallAPI(urlString, "", out -> {
                try {
                    Integer responseText = Integer.parseInt(out.getString(Config.ResponseJson.TEXT.toString()));
                    if (responseText >= 5) {
                        new AlertDialog.Builder(IntroActivity.this)
                                .setTitle("WHAT?")
                                .setMessage("The beta test program is full, how did you get this app?")
                                .setOnCancelListener(dialog -> System.exit(0)).show();
                    } else {
                        Log.d(TAG, "onCreate: beta testers, all ok, only "+responseText+" testers.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            startActivity(new Intent(this, MainActivity.class));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(this, ProtectorService.class));
            } else {
                startService(new Intent(this, ProtectorService.class));
            }
            finish();
        }

    }

    @Override
    public void onFinish() {
        Log.d("TAG", "onFinish called");
        Helper.sharedPrefs(getApplicationContext()).edit().putBoolean("first", false).apply();
    }

    public void showError(String error) {
        Log.d(TAG, "showError: "+error);
        Snackbar.make(coordinatorLayoutPublic, error, Snackbar.LENGTH_LONG).addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                navigationViewPublic.setTranslationY(0f);
                super.onDismissed(snackbar, event);
            }
        }).show();
    }
}
