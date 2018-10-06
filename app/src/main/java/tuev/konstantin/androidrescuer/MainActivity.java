package tuev.konstantin.androidrescuer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ToggleButton;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

public class MainActivity extends AppCompatActivity {


    Menu menu;
    public SharedPreferences prefs;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    if (!(current instanceof Main_Fragment)) {
                        current = new Main_Fragment();


                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                        transaction.replace(R.id.fragment_container, current);

                        transaction.commit();

                        current.landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

                        tb.setTextOff(getString(R.string.show_header));
                        tb.setTextOn(getString(R.string.hide_header));
                        tb.setChecked(true);
                        toggleNavBar(true);
                        toggleBack.setVisibility(View.VISIBLE);
                        toolbar.setVisibility(View.VISIBLE);

                        if (current.landscape) {
                            menu.getItem(0).setVisible(true);
                            menu.getItem(1).setVisible(false);
                            (new Handler()).postDelayed(() -> {
                                wasChecked = tb.isChecked();
                                onShowMethod();
                                if (!wasChecked) {
                                    checkedChange(true);
                                    tb.setChecked(true);
                                }
                            }, 100);
                        }
                    }
                    return true;
                case R.id.navigation_map:
                    if (!(current instanceof Map_Fragment)) {
                        current = new Map_Fragment();
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                        transaction.replace(R.id.fragment_container, current);

                        transaction.commit();

                        current.landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

                        toggleNavBar(false);
                        toggleBack.setVisibility(View.GONE);
                        if (current.landscape) {
                            toolbar.setVisibility(View.VISIBLE);
                            menu.getItem(0).setVisible(false);
                            menu.getItem(1).setVisible(true);
                            (new Handler()).postDelayed(() -> {
                                wasChecked = tb.isChecked();
                                if (!wasChecked) {
                                    checkedChange(false);
                                    tb.setChecked(false);
                                }
                                toggleBack.setVisibility(View.GONE);
                            }, 100);
                        } else {
                            toolbar.setVisibility(View.GONE);
                        }
                    }
                    return true;
                case R.id.navigation_control_panel:
                    if (!(current instanceof ControlPanel_Fragment)) {
                        current = new ControlPanel_Fragment();
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                        transaction.replace(R.id.fragment_container, current);

                        transaction.commit();

                        toggleNavBar(true);
                    }
                    return true;
            }
            return false;
        }
    };

    private float finalRadiusTop;
    public View top;
    private int cxtop, cytop;
    public static String TAG = "tuev.ko.";
    public BaseFragment current;
    boolean shown = false;
    public ToggleButton tb;
    private boolean wasChecked;
    private View toggleBack;
    public BottomNavigationView navigation;
    private int cxnav, cynav;
    float finalRadiusNav;
    public ViewGroup root;
    public boolean canRUN = true;
    private Toolbar toolbar;
    public RecipientEditTextView phoneRetv;
    private boolean shouldLandMap = false;

    protected void onHideKeyboard() {
        if (shown) {
            shown = false;
            if (!current.landscape) {
                onHideMethod();
            }
            current.onKeyboardHide();
        }
    }
    public void onHideMethod() {
        if (!(current instanceof Map_Fragment)) {
            toggleBack.setVisibility(View.VISIBLE);
            toggleBack.setAlpha(0.0f);
            toggleBack.animate()
                    .translationX(0)
                    .alpha(1.0f)
                    .setDuration(getResources().getInteger(R.integer.defAnimationDuration))
                    .setListener(null);
        } else {
            showBottomNavBar();
        }
    }
    public void showBottomNavBar() {
        canRUN = false;
        if (current instanceof Map_Fragment && ((Map_Fragment)current).bottomContainer != null) {
            ((Map_Fragment) current).bottomContainer.animate()
                    .translationY(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 58, getResources().getDisplayMetrics()) * -1)
                    .setDuration(getResources().getInteger(R.integer.defAnimationDuration))
                    .setListener(null);
        }
        startColorAnimation(navigation, getResources().getColor(R.color.colorPrimaryLight), getResources().getColor(android.R.color.white));
        navigation.setVisibility(View.VISIBLE);
        navigation.setAlpha(0.0f);
        navigation.animate()
                .translationY(0)
                .alpha(1.0f)
                .setDuration(getResources().getInteger(R.integer.defAnimationDuration))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        canRUN = true;
                    }
                });
    }
    public void onShowMethod() {
        if (!(current instanceof Map_Fragment)) {
            toggleBack.animate()
                    .translationX(toggleBack.getHeight() + 10)
                    .alpha(0.0f)
                    .setDuration(getResources().getInteger(R.integer.defAnimationDuration))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            toggleBack.setVisibility(View.GONE);
                        }
                    });
        } else {
            hideBottomNav();
        }
    }

    public void hideBottomNav() {
        canRUN = false;
        startColorAnimation(navigation, getResources().getColor(android.R.color.white), getResources().getColor(R.color.colorPrimaryLight));
        if (current instanceof Map_Fragment && ((Map_Fragment)current).bottomContainer != null) {
            ((Map_Fragment) current).bottomContainer.animate()
                    .translationY(0)
                    .setDuration(getResources().getInteger(R.integer.defAnimationDuration))
                    .setListener(null);
        }
        navigation.animate()
                .translationY(navigation.getHeight() + 10)
                .alpha(0.0f)
                .setDuration(getResources().getInteger(R.integer.defAnimationDuration))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        navigation.setVisibility(View.GONE);
                        canRUN = true;
                    }
                });
    }

    protected void onShowKeyboard() {
        if (!shown) {
            shown = true;
            if (!current.landscape) {
                onShowMethod();
            }
            current.onKeyboardShow();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (!current.landscape) {
            outState.putBoolean("toggle", tb.isChecked());
        } else {
            outState.putBoolean("toggle", wasChecked);
        }
        String fragment = null;
        if (current instanceof Map_Fragment) {
            fragment = "map";
        } else if (current instanceof Main_Fragment) {
            fragment = "main";
        } else if (current instanceof ControlPanel_Fragment) {
            fragment = "control";
        }
        outState.putString("fragments", fragment);
        super.onSaveInstanceState(outState);
    }

    public EditText appPass;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        top = findViewById(R.id.topPanel);
        tb = findViewById(R.id.showHideTop);
        FragmentManager fm = getSupportFragmentManager();

        navigation = findViewById(R.id.navigation);
        toggleBack = findViewById(R.id.back_view_animate);

        current = (BaseFragment) fm.findFragmentById(R.id.fragment_container);
        if (current == null) {
            Log.d(TAG, "onCreate: current: null");
            if (savedInstanceState != null) {
                Log.d(TAG, "onCreate: savedINS: not null");
                FragmentTransaction transaction;
                switch (savedInstanceState.getString("fragments", "main")) {
                    case "main":
                        Log.d(TAG, "onCreate: main");
                        current = new Main_Fragment();

                        transaction = getSupportFragmentManager().beginTransaction();

                        transaction.replace(R.id.fragment_container, current);

                        transaction.commit();
                        break;
                    case "map":
                        current = new Map_Fragment();
                        transaction = getSupportFragmentManager().beginTransaction();

                        transaction.replace(R.id.fragment_container, current);

                        transaction.commit();
                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                            toolbar.setVisibility(View.GONE);
                        } else {
                            toolbar.setVisibility(View.VISIBLE);
                            shouldLandMap = true;
                        }
                        toggleBack.setVisibility(View.GONE);
                        toggleNavBar(false);
                        break;
                    case "control":
                        current = new ControlPanel_Fragment();
                        transaction = getSupportFragmentManager().beginTransaction();

                        transaction.replace(R.id.fragment_container, current);

                        transaction.commit();
                        if (navigation.getVisibility() == View.VISIBLE) {
                            checkedChange(false);
                        }
                        break;
                }
            } else {
                Log.d(TAG, "onCreate: savedINS: null");
                current = new Main_Fragment();
                fm.beginTransaction().add(R.id.fragment_container, current).commit();
            }
        } else {
            Log.d(TAG, "onCreate: current: not null");
            if (current instanceof Map_Fragment) {
                Log.d(TAG, "onCreate: maps");
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    toolbar.setVisibility(View.GONE);
                } else {
                    toolbar.setVisibility(View.VISIBLE);
                    shouldLandMap = true;
                }
                toggleBack.setVisibility(View.GONE);
                toggleNavBar(false);
            }
            if (current instanceof ControlPanel_Fragment) {
                if (navigation.getVisibility() == View.VISIBLE) {
                    checkedChange(false);
                }
            }
        }
        current.landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            top.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    //Remove it here unless you want to get this callback for EVERY
                    //layout pass, which can get you into infinite loops if you ever
                    //modify the layout from within this method.
                    top.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        cxtop = top.getWidth() / 2;
                        cytop = top.getHeight() / 2;
                        finalRadiusTop = (float) Math.hypot(cxtop, cytop);
                            cxnav = navigation.getWidth() / 2;
                            cynav = navigation.getHeight() / 2;
                            finalRadiusNav = (float) Math.hypot(cxnav, cynav);
                    }
                    if (current instanceof Map_Fragment) {
                        toggleNavBar(false);
                    }

                        if (savedInstanceState != null && savedInstanceState.getBoolean("toggle", false)) {
                            tb.setChecked(true);
                            top.setVisibility(View.VISIBLE);
                            if (current instanceof Map_Fragment) {
                                navigation.setVisibility(View.VISIBLE);
                            }
                        } else {
                            tb.setChecked(false);
                            top.setVisibility(View.GONE);
                            if (current instanceof Map_Fragment) {
                                navigation.setVisibility(View.GONE);
                            }
                        }
                        if (current.landscape) {
                            (new Handler()).postDelayed(() -> {
                                wasChecked = tb.isChecked();
                                if (!wasChecked) {
                                    if (current instanceof Main_Fragment) {
                                        onShowMethod();
                                        checkedChange(true);
                                        tb.setChecked(true);
                                    }
                                } else {
                                    if (current instanceof Main_Fragment) {
                                        onShowMethod();
                                    }
                                }
                            }, 100);
                        }
                    //Now you can get the width and height from content
                }
            });

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        phoneRetv = findViewById(R.id.phone_get);
        phoneRetv.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        BaseRecipientAdapter baseRecipientAdapter = new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, this);

        baseRecipientAdapter.setShowMobileOnly(false);

        phoneRetv.setAdapter(baseRecipientAdapter);

        appPass = findViewById(R.id.appPassword);

        root = findViewById(R.id.rootLayout);

        tb.setOnCheckedChangeListener((compoundButton, b) -> checkedChange(b));

            KeyboardVisibilityEvent.setEventListener(
                    this,
                    isOpen -> {
                        if (isOpen) {
                            onShowKeyboard();
                        } else {
                            onHideKeyboard();
                        }
                    });
            if (getIntent() != null) {
                Bundle extras = getIntent().getExtras();
                if (extras != null && extras.containsKey("dialogTitle")) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(extras.getString("dialogTitle", ""))
                            .setMessage(extras.getString("dialogMsg", ""))
                            .setPositiveButton("OK", (dialog, which) -> {}).show();
                }
            }
    }

    public void toggleNavBar(boolean show) {

        int newUiOptions = 0;

        if (!show) {
            // Navigation bar hiding:  Backwards compatible to ICS.
            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

            // Status bar hiding: Backwards compatible to Jellybean
            if (Build.VERSION.SDK_INT >= 16) {
                newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
            }

            if (Build.VERSION.SDK_INT >= 19) {
                newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
        } else {
            newUiOptions ^= View.SYSTEM_UI_FLAG_VISIBLE;
        }

        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
        //END_INCLUDE (set_ui_flags)
    }

    @SuppressLint("NewApi")
    public void checkedChange(boolean show) {
        if (ViewCompat.isAttachedToWindow(top) && ViewCompat.isAttachedToWindow(navigation)) {
            if (!show) {
                Animator anim =
                        ViewAnimationUtils.createCircularReveal(top, cxtop, cytop, finalRadiusTop, 0);
                anim.setInterpolator(new AccelerateDecelerateInterpolator());

                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        top.setVisibility(View.GONE);
                        if (current instanceof Map_Fragment && navigation.getVisibility() == View.VISIBLE) {
                            hideBottomNav();
                        }
                    }
                });

// start the animation
                anim.setDuration(getResources().getInteger(R.integer.defAnimationDuration));
                anim.start();
            } else {
                Animator anim =
                        ViewAnimationUtils.createCircularReveal(top, cxtop, cytop, 0, finalRadiusTop);
                anim.setInterpolator(new AccelerateDecelerateInterpolator());

                // make the view visible and start the animation
                anim.setDuration(getResources().getInteger(R.integer.defAnimationDuration));
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (current instanceof Map_Fragment && navigation.getVisibility() != View.VISIBLE) {
                            showBottomNavBar();
                        }
                    }
                });
                top.setVisibility(View.VISIBLE);
                anim.start();
            }
        }
    }

    static void startColorAnimation(final View view, final int startColor, final int endColor) {
        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(startColor, endColor);
        anim.setEvaluator(new ArgbEvaluator());
        anim.addUpdateListener(valueAnimator -> view.setBackgroundColor((Integer) valueAnimator.getAnimatedValue()));
        anim.setDuration(view.getResources().getInteger(R.integer.defAnimationDuration));
        anim.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu;
        if (current.landscape) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            if (shouldLandMap) {
                menu.getItem(0).setVisible(false);
                menu.getItem(1).setVisible(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.send_menu:
                if (current instanceof Main_Fragment) {
                    ((Main_Fragment) current).send();
                }
                return true;
            case R.id.search_menu:
                if (current instanceof Map_Fragment) {
                    ((Map_Fragment) current).search();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
