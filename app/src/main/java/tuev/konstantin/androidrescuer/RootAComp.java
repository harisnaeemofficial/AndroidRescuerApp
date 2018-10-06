package tuev.konstantin.androidrescuer;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import agency.tango.materialintroscreen.SlideFragment;
import eu.chainfire.libsuperuser.Shell;

public class RootAComp extends SlideFragment {
    private boolean goodTogo = false;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.root_xposed, container, false);
        AppCompatButton root = view.findViewById(R.id.check_root);
        Glide.with(this).load(Helper.getServerImageUrl(getContext(), "comp_root.png")).into((ImageView) view.findViewById(R.id.comp_root));
        root.setOnClickListener(view12 -> Helper.hasRoot(getActivity(), hasRoot -> {
            if (hasRoot) {
                new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                        .setTitle(R.string.root_waw)
                        .setMessage(R.string.root_waw_desc)
                        .setPositiveButton("OK", (dialogInterface, i) -> {

                        }).show();
            } else {
                new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                        .setTitle(R.string.no_root)
                        .setMessage(R.string.no_root_desc)
                        .setPositiveButton("OK", (dialogInterface, i) -> {

                        }).show();
            }
            goodTogo = true;
        }));
        AppCompatButton comp = view.findViewById(R.id.comp_instr);
        comp.setOnClickListener(v -> {
            Helper.showPrepareForPCActivation(getContext());
          goodTogo = true;
        });
        return view;
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        goodTogo = savedInstanceState != null && savedInstanceState.getBoolean("goodToGo", false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("goodToGo", goodTogo);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean hasNeededPermissionsToGrant() {
        return false;
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
        return getString(R.string.check_root_computer_more);
    }
}
