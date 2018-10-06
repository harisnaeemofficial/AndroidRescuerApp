package tuev.konstantin.androidrescuer;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.widgets.OverScrollViewPager;

import static tuev.konstantin.androidrescuer.Helper.sendLocalConnectionCommandAndEnd;

public class ComputerActivity extends MaterialIntroActivity {

    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OverScrollViewPager overScrollLayout = findViewById(agency.tango.materialintroscreen.R.id.view_pager_slides);
        overScrollLayout.getOverScrollView().setOffscreenPageLimit(2);
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.intro_slide)
                .buttonsColor(R.color.buttons_intro)
                .image("c1.jpg")
                .description("")
                .title("Open your device's Settings app and click about device.")
                .build());
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.intro_slide)
                .buttonsColor(R.color.buttons_intro)
                .image("c2.jpg")
                .description("")
                .title("Go somewhere near the bottom of this screen and find Build number...")
                .build());
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.intro_slide)
                .buttonsColor(R.color.buttons_intro)
                .image("c3.jpg")
                .description("")
                .title("When all ready, go back and find Developer options...")
                .build());
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.intro_slide)
                .buttonsColor(R.color.buttons_intro)
                .image("c4.jpg")
                .description("")
                .title("In Developer options screen find Debugging submenu.")
                .build());
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.intro_slide)
                .buttonsColor(R.color.buttons_intro)
                .image("c5.jpg")
                .description("")
                .title("Follow the instructions.")
                .build());
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.intro_slide)
                .buttonsColor(R.color.buttons_intro)
                .image("c6.jpg")
                .description("GRANT ANY DIALOG SHOWN ON THE DEVICE!!!")
                .title("Open the desktop program, ensure that you have internet and click on the 3-dot menu...")
                .build());
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean writeSecure = intent.getBooleanExtra("writeSecure", false);
                boolean adbLan = intent.getBooleanExtra("adbLan", false);
                checkStates(writeSecure, adbLan);
            }
        };
        registerReceiver(receiver, new IntentFilter("tuev.konstantin.androidrescuer.ADBStatusChange"));
    }

    private void checkStates(boolean writeSecure, boolean adbLan) {
        canFinish = true;
        Runnable adbLanAction = () -> new AlertDialog.Builder(ComputerActivity.this)
                .setTitle("Grant the next dialog!")
                .setMessage("If no dialog appeared something has went wrong!!!")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> checkAdb())
                .show();

        if (writeSecure) {
            if (Helper.canWriteSystemSettings(ComputerActivity.this)) {
                new AlertDialog.Builder(ComputerActivity.this)
                        .setTitle("Success!")
                        .setMessage("The app can now change system settings!")
                        .setCancelable(false)
                        .setPositiveButton("OK", (dialog14, which14) -> {if (adbLan) {adbLanAction.run();}}).show();
            } else {
                new AlertDialog.Builder(ComputerActivity.this)
                        .setTitle("Something happened, try again later!!!")
                        .setMessage("The app still can't change system settings.")
                        .setCancelable(false)
                        .setPositiveButton("OK", (dialog, which) -> {if (adbLan) {adbLanAction.run();}}).show();
            }
        } else {
            if (adbLan) {
                adbLanAction.run();
            }
        }
    }

    private void checkAdb() {
        ProgressDialog pd = new ProgressDialog(this, R.style.AlertDialogTheme);
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.setTitle("Waiting results...");
        pd.show();
        sendLocalConnectionCommandAndEnd("logcat", ComputerActivity.this, (fail) -> runOnUiThread(() -> {
            pd.cancel();
            if (!fail) {
                new AlertDialog.Builder(ComputerActivity.this)
                        .setTitle("All with remote mobile data is OK UNTIL RESTART.")
                        .setPositiveButton("OK", (dialog12, which12) -> {

                        }).show();
            } else {
                new AlertDialog.Builder(ComputerActivity.this)
                        .setTitle("Something might be wrong")
                        .setNegativeButton("OK", (dialog1, which1) -> {

                        })
                        .setPositiveButton("Check Again", (dialog, which) -> checkAdb()).show();
            }
        }));
    }

    boolean canFinish = false;
    @Override
    public void finish() {
        if (canFinish) {
            unregisterReceiver(receiver);
            super.finish();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Nothing has happened.")
                    .setMessage("It seems like no actions were performed.")
                    .setPositiveButton("Close", (dialog, which) -> {

                    })
                    .setNegativeButton("Check anyway", (dialog, which) -> checkStates(true, true))
                    .setNeutralButton("Exit", (dialog, which) -> {
                        canFinish = true;
                        finish();
                    }).show();
        }
    }
}
