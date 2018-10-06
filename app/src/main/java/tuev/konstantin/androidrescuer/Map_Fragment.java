package tuev.konstantin.androidrescuer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.android.ex.chips.recipientchip.DrawableRecipientChip;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

import static tuev.konstantin.androidrescuer.MainActivity.TAG;

public class Map_Fragment extends BaseFragment implements OnMapReadyCallback {
    public View search;
    public RelativeLayout bottomContainer;
    MainActivity mainActivity;
    private View fragment;
    private ImageView openIn;
    private ImageView directions;
    private LinearLayout directionsContainer;
    private ObservableBoolean mapReady = new ObservableBoolean();
    private Marker marker;
    private GoogleMap map;
    private AppCompatCheckBox autoRefresh;
    private LatLng lastLatLong = null;
    private Marker clickedMarker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setRetainInstance(false);
        mainActivity = (MainActivity) getActivity();
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragment = inflater.inflate(R.layout.map_fragment, container, false);


        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);


        bottomContainer = fragment.findViewById(R.id.bottom_container);
        search = fragment.findViewById(R.id.search);

        if (landscape) {
            search.setVisibility(View.GONE);
        } else {
            search.setOnClickListener(view -> {
                if (mainActivity.navigation.getVisibility() == View.GONE) {
                    mainActivity.checkedChange(true);
                }
                search();
            });
        }
        if (mainActivity.navigation.getVisibility() == View.VISIBLE) {
            bottomContainer.setTranslationY(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 58, getResources().getDisplayMetrics()) * -1);
            if (mainActivity.top.getVisibility() != View.VISIBLE) {
                mainActivity.checkedChange(true);
            }
        }
        autoRefresh = fragment.findViewById(R.id.autoRefresh);
        return fragment;
    }

    @Override
    public void onKeyboardShow() {
        mainActivity.toggleNavBar(true);
    }

    @Override
    public void onKeyboardHide() {
        mainActivity.toggleNavBar(false);
    }
    
    @SuppressLint({"MissingPermission", "ResourceType"})
    @Override
    public void onMapReady(final GoogleMap map1) {
        map = map1;
        mapReady.setValue(true);
        try {
            map.setMyLocationEnabled(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.90270080000001,12.496235200000001), 5));
        final ViewGroup logoNTrademark = ((ViewGroup)((ViewGroup)(fragment.findViewById(0x2).getParent()).getParent()).getChildAt(1));
        ((ImageView)logoNTrademark.getChildAt(0)).setImageResource(R.mipmap.logo);
        directionsContainer = fragment.findViewById(0x4);
        directions = (ImageView) directionsContainer.getChildAt(0);
        openIn = (ImageView) directionsContainer.getChildAt(1);
        openIn.setImageResource(R.drawable.ic_cloud_blue_a400_24dp);
        openIn.setOnClickListener(view -> {
            Log.d(TAG, "onClick: openIN");
            Helper.showAddressInfo(mainActivity, clickedMarker.getPosition());
        });
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {
            @Override
            public boolean onMarkerClick(Marker arg0) {
                clickedMarker = arg0;
                if (!landscape) {
                    search.animate()
                            .translationY(search.getHeight() + 10)
                            .alpha(0.0f)
                            .setDuration(getResources().getInteger(R.integer.defAnimationDuration))
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    search.setVisibility(View.GONE);
                                }
                            });
                }
                if (mainActivity.navigation.getVisibility() == View.VISIBLE) {
                    mainActivity.checkedChange(false);
                }
                return false;
            }
        });
        (new Handler()).postDelayed(() -> logoNTrademark.getChildAt(1).setVisibility(View.GONE), 5200);
        map.setOnMapClickListener(latLng -> {
            if (mainActivity.navigation.getVisibility() == View.VISIBLE) {
                mainActivity.checkedChange(false);
            } else {
                if (directionsContainer != null && directionsContainer.getVisibility() != View.VISIBLE) {
                    mainActivity.checkedChange(true);
                } else {
                    if (!landscape) {
                        search.setVisibility(View.VISIBLE);
                        search.setAlpha(0.0f);
                        search.animate()
                                .translationY(0)
                                .alpha(1.0f)
                                .setDuration(getResources().getInteger(R.integer.defAnimationDuration))
                                .setListener(null);
                    }
                }
            }
        });
        mainActivity.phoneRetv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                canRefresh = false;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mainActivity.appPass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                canRefresh = false;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (canRefreshCheck && canRefresh && !mainActivity.phoneRetv.getText().toString().replace(" ", "").isEmpty() && !mainActivity.appPass.getText().toString().replace(" ", "").isEmpty()) {
                    mainActivity.runOnUiThread(() -> search());
                }
            }
        }, 0, 10000);
        autoRefresh.setOnCheckedChangeListener((buttonView, isChecked) -> canRefreshCheck = isChecked);
    }

    boolean canRefresh = false;
    boolean canRefreshCheck = false;

    public void realSearch() {
        doingSearch = true;
        DrawableRecipientChip[] chips = mainActivity.phoneRetv.getSortedRecipients();
        String appPass = mainActivity.appPass.getText().toString();
        if (chips.length != 0 && chips[0] != null && !appPass.isEmpty()) {
            String phone = chips[0].getEntry().getDestination().replaceAll("[^0-9+]", "");
            JSONObject json = new JSONObject();
            try {
                json.put("phone", phone);
                json.put("pass", appPass);
                json.put("myphone", mainActivity.prefs.getString("phone", ""));
                json.put("test", Config.test);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            new Helper.CallAPI(Helper.getServerUrl(getContext(), Helper.url.JUSTUSERLOCATION), json, out -> {
                try {
                    String responseText = out.getString(Config.ResponseJson.TEXT.toString());
                    Boolean responseError = out.getBoolean(Config.ResponseJson.ERROR.toString());
                    if (responseError) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.error)
                                .setMessage(responseText)
                                .setPositiveButton("OK", (dialog, which) -> {

                                }).show();
                        doingSearch = false;
                        canRefresh = false;
                    } else {
                        LatLng latLng = new LatLng(Double.parseDouble(out.optString("lat", "0.0")), Double.parseDouble(out.optString("long", "0.0")));
                        if (lastLatLong == null || lastLatLong.latitude != latLng.latitude || lastLatLong.longitude != latLng.longitude) {
                            lastLatLong = latLng;
                            boolean doIt = false;
                            if (marker == null) {
                                doIt = true;
                                marker = map.addMarker(new MarkerOptions()
                                        .title("Phone location")
                                        .snippet("Phone number: " + phone)
                                        .position(latLng));
                            } else {
                                marker.setSnippet("Phone number: " + phone);
                                marker.setPosition(latLng);
                            }
                            boolean finalDoIt = doIt;
                            map.animateCamera(CameraUpdateFactory.newLatLng(latLng), new GoogleMap.CancelableCallback() {
                                @Override
                                public void onFinish() {
                                    if (finalDoIt) {
                                        (new Handler(Looper.getMainLooper())).postDelayed(() -> map.animateCamera(CameraUpdateFactory.zoomTo(16)), 600);
                                    }
                                }

                                @Override
                                public void onCancel() {

                                }
                            });
                        }
                        doingSearch = false;
                        canRefresh = true;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    boolean doingSearch = false;

    Observer observer;
    public void search() {
        if (!doingSearch) {
            if (!mapReady.getValue()) {
                observer = (o, arg) -> {
                    if (mapReady.getValue()) {
                        mapReady.deleteObserver(observer);
                        realSearch();
                    }
                };
                mapReady.addObserver(observer);
            } else {
                realSearch();
            }
        }
    }
}
