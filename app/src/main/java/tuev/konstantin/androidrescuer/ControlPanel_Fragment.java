package tuev.konstantin.androidrescuer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;

import java.util.Observable;
import java.util.Observer;

import eu.chainfire.libsuperuser.Shell;

public class ControlPanel_Fragment extends BaseFragment {
    private View fragment;
    private SwitchCompat realTimeLoc, locOnWrongPass, photoOnWrongPass, lockPowermenu, lockAirplane, logging, wifiScan, lockStatusbar;
    private BroadcastReceiver receiver;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragment = inflater.inflate(R.layout.control_panel, container, false);
        HandleControlMessage handleControlMessage = new HandleControlMessage(getContext(), true);

        realTimeLoc = fragment.findViewById(R.id.rtLoc);
        locOnWrongPass = fragment.findViewById(R.id.locWP);
        photoOnWrongPass = fragment.findViewById(R.id.photoWP);
        lockPowermenu = fragment.findViewById(R.id.powermenu_lock);
        lockAirplane = fragment.findViewById(R.id.airplane_lock);
        Helper.hasRoot(getContext(), hasRoot -> lockAirplane.setEnabled(hasRoot));
        lockStatusbar = fragment.findViewById(R.id.statusbar_lock);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lockStatusbar.setEnabled(false);
        }
        wifiScan = fragment.findViewById(R.id.wifi_scan);
        getContext().registerReceiver(receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateSwitches();
            }
        }, new IntentFilter("tuev.konstantin.androidrescuer.UpdateSwitches"));

        realTimeLoc.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent startService = new Intent(getContext(), ProtectorService.class);
            startService.putExtra("action", "locationTracking");
            startService.putExtra("state", isChecked);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(startService);
            } else {
                getContext().startService(startService);
            }
        });

        locOnWrongPass.setOnCheckedChangeListener((buttonView, isChecked) -> Helper.sharedPrefs(getContext()).edit().putBoolean("wrongPassLocation", isChecked).apply());

        photoOnWrongPass.setOnCheckedChangeListener((buttonView, isChecked) -> Helper.sharedPrefs(getContext()).edit().putBoolean("wrongPassPic", isChecked).apply());

        View backupContacts = fragment.findViewById(R.id.back_cont);
        backupContacts.setOnClickListener(v -> handleControlMessage.backupContacts());

        View backupFolder = fragment.findViewById(R.id.back_folder);
        backupFolder.setOnClickListener(v -> handleControlMessage.backupRescueFolder());

        View activateWithComp = fragment.findViewById(R.id.comp_activation);
        Helper.hasRoot(getContext(), hasRoot -> {
            if (hasRoot) {
                activateWithComp.setEnabled(false);
            }
        });
        activateWithComp.setOnClickListener(v -> Helper.showPrepareForPCActivation(getContext()));

        lockPowermenu.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent startService = new Intent(getContext(), ProtectorService.class);
            startService.putExtra("action", "lockProtector");
            startService.putExtra("state", isChecked);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(startService);
            } else {
                getContext().startService(startService);
            }
        });

        lockAirplane.setOnCheckedChangeListener((buttonView, isChecked) -> Helper.sharedPrefs(getContext()).edit().putBoolean("airplane_lock", isChecked).apply());

        lockStatusbar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent startService = new Intent(getContext(), ProtectorService.class);
            startService.putExtra("action", "statusBar");
            startService.putExtra("state", isChecked);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(startService);
            } else {
                getContext().startService(startService);
            }
        });

        logging = fragment.findViewById(R.id.logging);
        boolean log = Logger.log(getContext());
        if (log) {
            Logger.startLogger(getContext());
        }
        logging.setChecked(log);
        logging.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Logger.startLogger(getContext());
            } else {
                Logger.endLog();
            }
        });

        wifiScan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent startService = new Intent(getContext(), ProtectorService.class);
            startService.putExtra("action", "wifiScan");
            startService.putExtra("state", isChecked);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(startService);
            } else {
                getContext().startService(startService);
            }
        });

        updateSwitches();
        return fragment;
    }

    @Override
    public void onDestroyView() {
        getContext().unregisterReceiver(receiver);
        super.onDestroyView();
    }

    void updateSwitches() {
        realTimeLoc.setChecked(Helper.sharedPrefs(getContext()).getBoolean("lockationTracking", false));
        locOnWrongPass.setChecked(Helper.sharedPrefs(getContext()).getBoolean("wrongPassLocation", false));
        photoOnWrongPass.setChecked(Helper.sharedPrefs(getContext()).getBoolean("wrongPassPic", false));
        lockPowermenu.setChecked(Helper.sharedPrefs(getContext()).getBoolean("lockPowermenu", false));
        lockAirplane.setChecked(Helper.sharedPrefs(getContext()).getBoolean("airplane_lock", false));
        wifiScan.setChecked(Helper.sharedPrefs(getContext()).getBoolean("scanForWifi", false));
        lockStatusbar.setChecked(Helper.sharedPrefs(getContext()).getBoolean("statusBarDisable", false));
    }
}
