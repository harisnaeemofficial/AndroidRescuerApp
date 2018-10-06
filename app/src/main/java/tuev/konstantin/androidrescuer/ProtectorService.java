package tuev.konstantin.androidrescuer;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static tuev.konstantin.androidrescuer.MainActivity.TAG;

@SuppressWarnings("unused")
public class ProtectorService extends Service {
    LockscreenProtector lockscreenProtector = new LockscreenProtector();
    private NotificationManager mNotificationManager;
    ScanOpenWifi scanOpenWifi = new ScanOpenWifi();
    SimListener simListener = new SimListener();
    LocationTracker locationTracker = new LocationTracker();
    StatusbarProtector statusbarProtector = new StatusbarProtector();
    BackgroundCamera backgroundCamera = new BackgroundCamera();
    IHandleControlMessage handleControlMessage;

    @SuppressWarnings("ResultOfMethodCallIgnored")
	public class BackgroundCamera implements SurfaceHolder.Callback {
		private WindowManager windowManager;
		private SurfaceView surfaceView;
		private Camera camera = null;
		private int cameraId;


		private int findFrontFacingCamera() {
			int cameraId = -1;
			// Search for the front facing camera
			int numberOfCameras = Camera.getNumberOfCameras();
			for (int i = 0; i < numberOfCameras; i++) {
				Camera.CameraInfo info = new Camera.CameraInfo();
				Camera.getCameraInfo(i, info);
				if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
					cameraId = i;
					break;
				}
			}
			return cameraId;
		}

		public void onCreate() {
			windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
			surfaceView = new SurfaceView(ProtectorService.this);
            WindowManager.LayoutParams layoutParams;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                layoutParams = new WindowManager.LayoutParams(
                        1, 1,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                        PixelFormat.TRANSLUCENT
                );
            } else {
                layoutParams = new WindowManager.LayoutParams(
                        1, 1,
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                        PixelFormat.TRANSLUCENT
                );
            }
            layoutParams.gravity = Gravity.START | Gravity.TOP;
			windowManager.addView(surfaceView, layoutParams);
			surfaceView.getHolder().addCallback(BackgroundCamera.this);
		}

		@Override
		public void surfaceCreated(SurfaceHolder surfaceHolder) {
			if (getPackageManager()
					.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
				cameraId = findFrontFacingCamera();
				if (cameraId >= 0) {
					camera = Camera.open(cameraId);
				}
			}
			try {
				camera.setPreviewDisplay(surfaceHolder);
			} catch (IOException e) {
				e.printStackTrace();
			}

			Camera.Parameters params = camera.getParameters();
			Camera.Size size = camera.getParameters().getSupportedPictureSizes().get(0);
			params.setPictureSize(size.width, size.height);
			if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			}
			camera.setParameters(params);
			//setCameraDisplayOrientation(cameraId, camera);
			camera.startPreview();
			camera.setOneShotPreviewCallback((data, camera) -> {
                camera.setOneShotPreviewCallback(null);
                takePic();
            });
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

		}

		void onDestroy() {
			camera.release();

			windowManager.removeView(surfaceView);
		}

		void takePic() {
			camera.takePicture(null, null, (data, camera) -> {

				File sourceFile = new File(Environment.getExternalStorageDirectory() + "/rescue");
				if (!sourceFile.exists()) {
					sourceFile = new File(Environment.getExternalStorageDirectory() + "/Rescue");
				}
				if (new File(sourceFile + "/data").listFiles(pathname -> pathname.toString().endsWith(".jpg")).length == 0) {
                    sourceFile = new File(sourceFile + "/data");
                    sourceFile.mkdirs();
                }

				File pictureFileDir = new File(sourceFile + "/Intruder-" + new SimpleDateFormat("dd\\MM\\yy-hh-mm-ss", Locale.GERMANY).format(new Date()) + ".jpg");
				try {
					FileOutputStream fos = new FileOutputStream(pictureFileDir);
					fos.write(data);
					fos.flush();
					fos.close();
					Log.v("New Image saved:" + pictureFileDir);
				} catch (Exception error) {
					Log.v("File " + pictureFileDir.getName() + " not saved: "
							+ error.getMessage());
				}
				onDestroy();
			});
		}


		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {

		}
	}

    public class LocationTracker {
        private FusedLocationProviderClient mFusedLocationClient;
        private LocationCallback mLocationCallback;

        public void onCreate(boolean oneTime) {
            if (ActivityCompat.checkSelfPermission(ProtectorService.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ProtectorService.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.v("The fuck? This is required! The user is retard!?");
                Toast.makeText(ProtectorService.this, "The fuck? This is required!\nConclusion: the user is retard!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Helper.locationEnabled(ProtectorService.this)) {
                Helper.toggleLocation(ProtectorService.this, true);
				new Thread(() -> {
                    int secsWaited = 0;
                    while (!Helper.locationEnabled(ProtectorService.this) && secsWaited <= 20) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        secsWaited += 1;
                    }
                    if (Helper.locationEnabled(ProtectorService.this)) {
                        new Handler(ProtectorService.this.getMainLooper()).post(() -> LocationTracker.this.init(oneTime));
                    }
                }).start();
            } else {
                init(oneTime);
            }

        }

        void onDestroy() {
        	try {
				Helper.sharedPrefs(ProtectorService.this).edit().putBoolean("lockationTracking", false).apply();
				mFusedLocationClient.removeLocationUpdates(mLocationCallback);
			} catch (Exception ex) {
        		ex.printStackTrace();
			}
        }

        @SuppressLint("MissingPermission")
        private void init(boolean oneTime) {
            if (!oneTime) {
				Helper.sharedPrefs(ProtectorService.this).edit().putBoolean("lockationTracking", true).apply();
            }
            Log.v("Location Tracker init()");
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(ProtectorService.this);
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                	Location location = locationResult.getLastLocation();
                	if (oneTime) {
                		onDestroy();
					}
					if (Logger.log(ProtectorService.this)) {
                	    Logger.d("Location: lat: "+location.getLatitude()+ " long: "+location.getLongitude());
                    }
                    JSONObject json = new JSONObject();
                    PassNPhone passNPhone = Helper.getPassNPhone(getApplicationContext());
                    try {
                        json.put("phone", passNPhone.getPhone());
                        json.put("pass", passNPhone.getPass());
                        json.put("lat", location.getLatitude());
                        json.put("long", location.getLongitude());
                        json.put("test", Config.test);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String url = Helper.getServerUrl(getApplicationContext(), Helper.url.NEWUSERLOCATION);
                    android.util.Log.d(TAG, "location: json: "+json+" url: "+url);
                    android.util.Log.d(TAG, "sendRegistrationToServer: url: "+url+" json: "+json.toString());
                    new Helper.CallAPI(url, json.toString(), out -> {});
                }
            };
            LocationRequest mLocationRequest = new LocationRequest();
            if (oneTime) {
				mLocationRequest.setInterval(0);
				mLocationRequest.setFastestInterval(0);
				mLocationRequest.setSmallestDisplacement(0);
			} else {
				mLocationRequest.setInterval(60000);
				mLocationRequest.setFastestInterval(30000);
				mLocationRequest.setSmallestDisplacement(10);
			}
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback,null);
        }
    }

    private KeyguardManager kgMgr;

    @SuppressLint("PrivateApi")
	public class LockscreenProtector {
		static final String ON_BOOT_COMPLETED = "on_boot";
		public static final int mId = 1;

		BroadcastReceiver mReceiver = new LockscreenReceiver();

		public void onCreate() {
			ProtectorService.this.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
			canRun = true;
		}

		boolean canRun = false;

		void onDestroy() {
			try {
				ProtectorService.this.unregisterReceiver(this.mReceiver);
            } catch (Exception e) {
                Log.v("No unregister for our receiver: " + e);
            }
			canRun = false;
		}
	}

    @SuppressLint("PrivateApi")
    public class StatusbarProtector {
        BroadcastReceiver mReceiverOnOff = new PowerOnOffReceiver();
        BroadcastReceiver mReceiverUnlock = new PhoneUnlockedReceiver();
        private customViewGroup view;
        private WindowManager manager;

        public class PowerOnOffReceiver extends BroadcastReceiver {

            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null && action.equals("android.intent.action.SCREEN_ON")) {
                    if (kgMgr != null && kgMgr.inKeyguardRestrictedInputMode()) {
                        Log.v("Screen On");
                        collapseNow();
                        return;
                    }
                    removeHandler();
                } else if (action != null && action.equals("android.intent.action.SCREEN_OFF")) {
                    removeHandler();
                }
            }
        }

        public class PhoneUnlockedReceiver extends BroadcastReceiver {

            @Override
            public void onReceive(Context context, Intent intent) {

                KeyguardManager keyguardManager = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
                if (keyguardManager != null && !keyguardManager.inKeyguardRestrictedInputMode()) {
                    removeHandler();
                }
            }
        }

        public class customViewGroup extends ViewGroup {

            public customViewGroup(Context context) {
                super(context);
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                Log.v("customViewGroup " + " **********Intercepted");
                return true;
            }
        }

        public void onCreate() {
            canRun = true;
            onStartCommand();
        }

        boolean canRun = false;

        void onStartCommand() {
            if (canRun) {
                manager = ((WindowManager) getApplicationContext()
                        .getSystemService(Context.WINDOW_SERVICE));
                Log.v("Watcher onStartCommand");
                Log.v("pull down status bar enabled");
                IntentFilter screenStateFilter = new IntentFilter();
                screenStateFilter.addAction("android.intent.action.SCREEN_ON");
                screenStateFilter.addAction("android.intent.action.SCREEN_OFF");
                ProtectorService.this.registerReceiver(this.mReceiverOnOff, screenStateFilter);
                registerReceiver(mReceiverUnlock, new IntentFilter("android.intent.action.USER_PRESENT"));
            }
        }

        void onDestroy() {
            try {
                ProtectorService.this.unregisterReceiver(this.mReceiverOnOff);
                unregisterReceiver(mReceiverUnlock);
                removeHandler();
            } catch (Exception e) {
                Log.v("No unregister for our receiver: " + e);
            }
            canRun = false;
        }

        private void removeHandler() {
            Log.v("removing handler");
            if (manager != null && view != null) {
                try {
                    manager.removeView(view);
                } catch (Exception ignored) {
                }
            }
        }

        void collapseNow() {
            Log.v("Collapse now");
            WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
            if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                localLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
            }
            localLayoutParams.gravity = Gravity.TOP;
            localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|

                    // this is to enable the notification to recieve touch events
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |

                    // Draws over status bar
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

            localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            localLayoutParams.height = (int) (50 * getResources()
                    .getDisplayMetrics().scaledDensity);
            localLayoutParams.format = PixelFormat.TRANSPARENT;

            view = new customViewGroup(ProtectorService.this);


            manager.addView(view, localLayoutParams);
        }
    }

	public static class Log {
		static void v(String s) {
			android.util.Log.d(ProtectorService.class.getSimpleName(), s);
		}
	}

    public class ScanOpenWifi {
        boolean firstRun = true;
        boolean wasConnected = false;
        Long lastRanCriticalDataBackupMillis = null;

		String TAG = getClass().getSimpleName();
		Runnable checkify = ScanOpenWifi.this::scanWifi;
		private ConnectivityManager connManager;
		boolean connected = false;
		List<String> disabled = new ArrayList<>();
		private Handler handler;
		Map<String, Integer> knownWifis = new HashMap<>();
		WifiManager mainWifi;
		WifiReceiver receiverWifi;
		private final NetworkStateReceiver stateReceiver = new NetworkStateReceiver();

		public class NetworkStateReceiver extends BroadcastReceiver {
			public final String TAG = NetworkStateReceiver.class.getSimpleName();

			@SuppressLint({"StaticFieldLeak"})
			public void onReceive(Context context, Intent intent) {

                android.util.Log.d(this.TAG, "Network connectivity change");
                NetworkInfo wifinet = ScanOpenWifi.this.connManager.getNetworkInfo(1);
                NetworkInfo mobile = ScanOpenWifi.this.connManager.getNetworkInfo(0);
                ScanOpenWifi.this.connected = wifinet.isConnectedOrConnecting() || mobile.isConnectedOrConnecting();
			    if (firstRun) {
                    wasConnected = connected;
			        firstRun = false;
			        return;
                }
				if (!ScanOpenWifi.this.connected) {
			        if (mobile.isAvailable()) {
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    connected = wifinet.isConnectedOrConnecting() || mobile.isConnectedOrConnecting();
                    if (!connected) {
                        if (lastRanCriticalDataBackupMillis == null || !DateUtils.isToday(lastRanCriticalDataBackupMillis)) {
                            handleControlMessage.prepareContacts(mp -> {
                                ScanOpenWifi.this.scanWifi();
                            });
                        } else {
                            ScanOpenWifi.this.scanWifi();
                        }
                    }
				} else if (wifinet.isConnectedOrConnecting() && !wasConnected) {
			        if (!wifinet.isConnected()) {
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
					android.util.Log.d(this.TAG, "onReceive: checking wifi");
					final int networkId = ScanOpenWifi.this.mainWifi.getConnectionInfo().getNetworkId();
					new AsyncTask<Void, Void, Void>() {
						boolean shit = false;

						protected Void doInBackground(Void... voids) {
							try {
								HttpURLConnection urlc = (HttpURLConnection) new URL("http://clients3.google.com/generate_204").openConnection();
								urlc.setRequestProperty("User-Agent", "Android");
								urlc.setRequestProperty("Connection", "close");
								urlc.setConnectTimeout(1500);
								urlc.connect();
								if (!(urlc.getResponseCode() == 204 || urlc.getContentLength() == 0)) {
									this.shit = true;
									android.util.Log.d(NetworkStateReceiver.this.TAG, "doInBackground: wifi: " + networkId + " is shit.");
								}
							} catch (IOException e) {
								android.util.Log.e(NetworkStateReceiver.this.TAG, "Error checking internet connection: " + e);
								this.shit = true;
								android.util.Log.d(NetworkStateReceiver.this.TAG, "doInBackground: wifi: " + networkId + " is shit.");
							}
							return null;
						}

						protected void onPostExecute(Void aVoid) {
							if (this.shit) {
								if (ScanOpenWifi.this.disabled.isEmpty() || !ScanOpenWifi.this.disabled.contains(ScanOpenWifi.this.mainWifi.getConnectionInfo().getBSSID())) {
									ScanOpenWifi.this.disabled.add(ScanOpenWifi.this.mainWifi.getConnectionInfo().getBSSID());
								}
								ScanOpenWifi.this.mainWifi.disconnect();
								if (networkId != -1) {
									ScanOpenWifi.this.mainWifi.disableNetwork(networkId);
									ScanOpenWifi.this.mainWifi.removeNetwork(networkId);
									ScanOpenWifi.this.mainWifi.saveConfiguration();
								}
							} else {
							    if (lastRanCriticalDataBackupMillis == null || !DateUtils.isToday(lastRanCriticalDataBackupMillis)) {
                                    android.util.Log.d(TAG, "onPostExecute: sendControlData");
                                    handleControlMessage = new HandleControlMessage(ProtectorService.this, true);
                                    handleControlMessage.sendCollectedData(true);
                                    lastRanCriticalDataBackupMillis = Calendar.getInstance().getTimeInMillis();
                                } else {
                                    android.util.Log.d(TAG, "onPostExecute: sendControlData doesn't need to run 2 times per day.");
                                }
                            }
						}
					}.execute();
				}
				android.util.Log.d(this.TAG, "Network State receiver onReceive: connected: " + ScanOpenWifi.this.connected);
                wasConnected = connected;
			}
		}

		class WifiReceiver extends BroadcastReceiver {
			WifiReceiver() {
			}

			public void onReceive(Context c, Intent intent) {
				NetworkInfo wifinet = ScanOpenWifi.this.connManager.getNetworkInfo(1);
				NetworkInfo mobile = ScanOpenWifi.this.connManager.getNetworkInfo(0);
				connected = wifinet.isConnectedOrConnecting() || mobile.isConnectedOrConnecting();
                if (mobile.isAvailable()) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                connected = wifinet.isConnectedOrConnecting() || mobile.isConnectedOrConnecting();

				android.util.Log.d(ScanOpenWifi.this.TAG, "Wifi receiver onReceive: connected: " + ScanOpenWifi.this.connected);
				if (!ScanOpenWifi.this.connected) {
					ScanOpenWifi.this.savedWifisUpdate();
					for (String bssid : ScanOpenWifi.this.disabled) {
						android.util.Log.d(ScanOpenWifi.this.TAG, "onReceive: bssid in disabled: " + bssid);
					}
					for (String ssid : ScanOpenWifi.this.knownWifis.keySet()) {
						android.util.Log.d(ScanOpenWifi.this.TAG, "onReceive: knownwifi ssid: " + ssid + " net id: " + ScanOpenWifi.this.knownWifis.get(ssid));
					}
					ScanResult wifi = null;
					for (ScanResult result : ScanOpenWifi.this.mainWifi.getScanResults()) {
						android.util.Log.d(ScanOpenWifi.this.TAG, "onReceive: scanRes: " + result.SSID + " strength: " + result.level);
						if (ScanOpenWifi.this.knownWifis.containsKey(result.SSID)) {
							if (ScanOpenWifi.this.disabled.contains(result.BSSID)) {
								android.util.Log.d(ScanOpenWifi.this.TAG, "onReceive: disabled bssid");
							} else {
								ScanOpenWifi.this.mainWifi.enableNetwork(ScanOpenWifi.this.knownWifis.get(result.SSID), true);
								return;
							}
						}
						if (!(result.capabilities.contains("WEP") || result.capabilities.contains("PSK") || result.capabilities.contains("EAP"))) {
							boolean problem = false;
							for (String disabledWifi : ScanOpenWifi.this.disabled) {
								if (disabledWifi.equals(result.BSSID)) {
									problem = true;
								}
							}
							if (!problem) {
								android.util.Log.d(ScanOpenWifi.this.TAG, "onReceive: free wifi: " + result.SSID + " strength: " + result.level);
								if (wifi != null && wifi.level < result.level) {
									wifi = result;
								} else if (wifi == null) {
									wifi = result;
								}
							}
						}
					}
					if (wifi != null && wifi.level > -85) {
						android.util.Log.d(ScanOpenWifi.this.TAG, "onReceive: ready to add wifi");
						WifiConfiguration wfc = new WifiConfiguration();
						wfc.SSID = "\"".concat(wifi.SSID).concat("\"");
						wfc.status = 1;
						wfc.priority = 40;
						wfc.allowedKeyManagement.set(0);
						wfc.allowedProtocols.set(1);
						wfc.allowedProtocols.set(0);
						wfc.allowedAuthAlgorithms.clear();
						wfc.allowedPairwiseCiphers.set(2);
						wfc.allowedPairwiseCiphers.set(1);
						wfc.allowedGroupCiphers.set(0);
						wfc.allowedGroupCiphers.set(1);
						wfc.allowedGroupCiphers.set(3);
						wfc.allowedGroupCiphers.set(2);
						int networkId = ScanOpenWifi.this.mainWifi.addNetwork(wfc);
						if (networkId != -1) {
							android.util.Log.d(ScanOpenWifi.this.TAG, "onReceive: connecting to: " + networkId);
							ScanOpenWifi.this.mainWifi.enableNetwork(networkId, true);
							android.util.Log.d(ScanOpenWifi.this.TAG, "onReceive: result connected: " + ScanOpenWifi.this.connected);
							return;
						}
						android.util.Log.d(ScanOpenWifi.this.TAG, "onReceive: network id -1");
					}
				}
			}
		}

		public void onCreate() {
			if (VERSION.SDK_INT >= 24 && !Helper.locationEnabled(ProtectorService.this)) {
				Helper.toggleLocation(ProtectorService.this, true);
			}
			this.mainWifi = (WifiManager) ProtectorService.this.getApplicationContext().getSystemService(WIFI_SERVICE);
			this.connManager = (ConnectivityManager) ProtectorService.this.getSystemService(CONNECTIVITY_SERVICE);
			this.receiverWifi = new WifiReceiver();
			ProtectorService.this.registerReceiver(this.receiverWifi, new IntentFilter("android.net.wifi.SCAN_RESULTS"));
			android.util.Log.d(this.TAG, "onCreate: ");
			IntentFilter filter = new IntentFilter();
			filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
			ProtectorService.this.registerReceiver(this.stateReceiver, filter);
			savedWifisUpdate();
			NetworkInfo wifinet = this.connManager.getNetworkInfo(1);
			NetworkInfo mobile = this.connManager.getNetworkInfo(0);
			if (wifinet.isConnectedOrConnecting() || mobile.isConnectedOrConnecting()) {
				this.connected = true;
			} else {
				scanWifi();
			}
			android.util.Log.d(this.TAG, "onReceive: connected: " + this.connected);
		}

		void doInback(boolean removeCheckWifi) {
			if (this.handler != null) {
				this.handler.removeCallbacks(this.checkify);
				this.handler = null;
			}
			if (!removeCheckWifi) {
				this.handler = new Handler();
				this.handler.postDelayed(this.checkify, 60000);
			}
		}

		private void scanWifi() {
			NetworkInfo wifinet = this.connManager.getNetworkInfo(1);
			NetworkInfo mobile = this.connManager.getNetworkInfo(0);
			this.connected = wifinet.isConnectedOrConnecting() || mobile.isConnectedOrConnecting();
			if (!this.connected) {
				if (!this.mainWifi.isWifiEnabled()) {
					this.mainWifi.setWifiEnabled(true);
				}
				this.mainWifi.startScan();
				doInback(false);
				android.util.Log.d(this.TAG, "scanWifi: ");
			} else {
				doInback(true);
			}
		}

		void onDestroy() {
		    try {
                ProtectorService.this.unregisterReceiver(this.stateReceiver);
                ProtectorService.this.unregisterReceiver(this.receiverWifi);
            } catch (Exception ex) {
		        ex.printStackTrace();
            }
		}

		private void savedWifisUpdate() {
			this.knownWifis = new HashMap<>();
			if (this.mainWifi.getConfiguredNetworks() != null) {
				for (WifiConfiguration wifi : this.mainWifi.getConfiguredNetworks()) {
					this.knownWifis.put(wifi.SSID.replace("\"", ""), wifi.networkId);
				}
			}
		}
	}

    public class SimListener {
		private MyPhoneStateListener phoneListener;
		TelephonyManager telephony;

		public class MyPhoneStateListener extends PhoneStateListener {

			public void onServiceStateChanged(ServiceState serviceState) {
				super.onServiceStateChanged(serviceState);

                int simState = telephony.getSimState();
                switch (simState) {
                    case TelephonyManager.SIM_STATE_ABSENT:
                        boolean[] mSelection = new boolean[5];
                        try {
                            mSelection = new Gson().fromJson(Helper.sharedPrefs(ProtectorService.this).getString("sim_action", null), boolean[].class);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        if (Logger.log(ProtectorService.this) || mSelection[1]) {
                            Logger.startLogger(getApplicationContext());
                            switch (serviceState.getState()) {
                                case ServiceState.STATE_OUT_OF_SERVICE:
                                    Logger.d("onServiceStateChanged: STATE_OUT_OF_SERVICE");
                                    return;
                                case ServiceState.STATE_EMERGENCY_ONLY:
                                    Logger.d("onServiceStateChanged: STATE_EMERGENCY_ONLY");
                                    return;
                                case ServiceState.STATE_POWER_OFF:
                                    Logger.d("onServiceStateChanged: STATE_POWER_OFF");
                                    return;
                                default:
                            }
                        }
                        if (mSelection[2]) {
                            try {
                                backgroundCamera.onCreate();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            android.util.Log.d(TAG, "onServiceStateChanged: take pic");
                        }
                        if (mSelection[3]) {
                            handleControlMessage.factoryDataReset(mSelection[4]);
                        } else if (mSelection[4]) {
                            handleControlMessage.deleteInternalStorage();
                        }
                        break;
                    case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                        Logger.d("Sim pin enter");
                        break;
                    case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                        Logger.d("Sim puk enter");
                        break;
                    case TelephonyManager.SIM_STATE_READY:
                        Logger.d("Sim ready");
                        break;
                }
			}
		}

		public void onCreate() {
			this.phoneListener = new MyPhoneStateListener();
			this.telephony = (TelephonyManager) ProtectorService.this.getApplicationContext().getSystemService(TELEPHONY_SERVICE);
			if (this.telephony != null) {
				this.telephony.listen(this.phoneListener, PhoneStateListener.LISTEN_SERVICE_STATE);
			}
		}

		void onDestroy() {
		    try {
                this.telephony.listen(this.phoneListener, PhoneStateListener.LISTEN_NONE);
            } catch (Exception ex) {
		        ex.printStackTrace();
            }
		}
	}

	@Nullable
	public IBinder onBind(Intent intent) {
		return null;
	}









	@SuppressWarnings("ConstantConditions")
	public int onStartCommand(Intent intent, int flags, int startId) {
		createNotification();
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				if (extras.getString("action") != null) {
					boolean state = extras.getBoolean("state", false);
					switch (extras.getString("action")) {
						case "takePic":
                            try {
                                backgroundCamera.onCreate();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
							break;
						case "sendLocation":
							locationTracker.onCreate(true);
							break;
						case "locationTracking":
							if (state) {
								locationTracker.onCreate(false);
							} else {
								locationTracker.onDestroy();
							}
							break;
                        case "statusBar":
                            Helper.sharedPrefs(ProtectorService.this).edit().putBoolean("statusBarDisable", state).apply();
                            if (extras.containsKey("unlocked")) {
                                if (extras.getBoolean("unlocked")) {
                                    statusbarProtector.removeHandler();
                                }
                            } else {
                                if (state) {
                                    statusbarProtector.onCreate();
                                } else {
                                    statusbarProtector.onDestroy();
                                }
                            }
                            break;
                        case "simListener":
                            simListener.onCreate();
                            break;
                        case "lockProtector":
							Helper.sharedPrefs(ProtectorService.this).edit().putBoolean("lockPowermenu", state).apply();
                            if (state) {
                                lockscreenProtector.onCreate();
                            } else {
                                lockscreenProtector.onDestroy();
                            }
                            break;
                        case "wifiScan":
							Helper.sharedPrefs(ProtectorService.this).edit().putBoolean("scanForWifi", state).apply();
                            if (state) {
                                scanOpenWifi.onCreate();
                            } else {
                                scanOpenWifi.onDestroy();
                            }
					}
				}
			}
		}
		return START_STICKY;
	}

	public void onCreate() {
		super.onCreate();
		//this.locationTracker.onCreate();
        kgMgr = (KeyguardManager) ProtectorService.this.getApplicationContext().getSystemService(KEYGUARD_SERVICE);

        handleControlMessage = new HandleControlMessage(ProtectorService.this, false);
		boolean[] mSelection = null;
		try {
			mSelection = new Gson().fromJson(Helper.sharedPrefs(ProtectorService.this).getString("sim_action", null), boolean[].class);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (mSelection != null && !mSelection[0]) {
			simListener.onCreate();
		}
		if (Helper.sharedPrefs(ProtectorService.this).getBoolean("lockPowermenu", false)) {
			lockscreenProtector.onCreate();
		}
        if (Helper.sharedPrefs(ProtectorService.this).getBoolean("statusBarDisable", false)) {
            statusbarProtector.onCreate();
        }
		if (Helper.sharedPrefs(ProtectorService.this).getBoolean("scanForWifi", false)) {
			scanOpenWifi.onCreate();
		}
		if (Helper.sharedPrefs(ProtectorService.this).getBoolean("lockationTracking", false)) {
			locationTracker.onCreate(false);
		}
        Log.v("Protector Service started!");
	}

	public void onDestroy() {
		super.onDestroy();
		stopForeground(true);
		scheduleUpdate();
		locationTracker.onDestroy();
		simListener.onDestroy();
		lockscreenProtector.onDestroy();
		scanOpenWifi.onDestroy();
        Toast.makeText(this, "Protector Service ended!", Toast.LENGTH_SHORT).show();
        Log.v("Protector Service ended!");

    }









	public void onTaskRemoved(Intent rootIntent) {
		scheduleUpdate();
		this.mNotificationManager.cancelAll();
	}

	Notification notification = null;

	private void createNotification() {
		if (notification == null) {
			this.mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.protection_service_notification);
			Builder m_notificationBuilder = new Builder(this)
					.setAutoCancel(true)
					.setOngoing(true)
					.setContentTitle("Protection is on!")
					.setSmallIcon(R.drawable.ic_stat_name);
			if (VERSION.SDK_INT >= 16) {
				m_notificationBuilder.setPriority(Notification.PRIORITY_LOW);
			}
			m_notificationBuilder.setContentIntent(PendingIntent.getActivity(this, (int) System.currentTimeMillis(), new Intent(this, IntroActivity.class), 0));
			if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				m_notificationBuilder.setChannelId("android_rescuer_channel");
				NotificationChannel channel = new NotificationChannel("android_rescuer_channel", "Service", NotificationManager.IMPORTANCE_LOW);
				channel.enableLights(false);
				mNotificationManager.createNotificationChannel(channel);
			}

			if (VERSION.SDK_INT >= 16) {
				notification = m_notificationBuilder.build();
			} else {
				notification = m_notificationBuilder.getNotification();
			}
			this.mNotificationManager.notify(46, notification);
			startForeground(46, notification);
		}
	}

	@SuppressLint("WrongConstant")
	private void scheduleUpdate() {
		Log.v("Android wants to kill the service. Start it again after a minute.");
		Intent ishintent = new Intent(this, ProtectorService.class);
        PendingIntent pintent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pintent = PendingIntent.getForegroundService(this, 0, ishintent, 0);
        } else {
            pintent = PendingIntent.getService(this, 0, ishintent, 0);
        }
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		if (alarm != null) {
			alarm.cancel(pintent);
			alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),60000, pintent);
		}
    }
}
