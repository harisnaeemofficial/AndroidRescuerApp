/*
 * Copyright 2017 Keval Patel.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package tuev.konstantin.androidrescuer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.io.File;

/**
 * Created by Keval on 11-Nov-16.
 * This class holds common camera utils.
 *
 * @author {@link 'https://github.com/kevalpatel2106'}
 */

public final class HiddenCameraUtils {

    /**
     * Check if the application has "Draw over other app" permission? This permission is available to all
     * the application below Android M (<API 23). But for the API 23 and above user has to enable it mannually
     * if the permission is not available by opening Settings -> Apps -> Gear icon on top-right corner ->
     * Draw Over other apps.
     *
     * @return true if the permission is available.
     * @see 'http://www.androidpolice.com/2015/09/07/android-m-begins-locking-down-floating-apps-requires-users-to-grant-special-permission-to-draw-on-other-apps/'
     */
    @SuppressLint("NewApi")
    public static boolean canOverDrawOtherApps(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    /**
     * This will open settings screen to allow the "Draw over other apps" permission to the application.
     *
     * @param context instance of caller.
     * @return request code for on activity result or 0
     */
    public static void openDrawOverPermissionSetting(Activity context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
        context.startActivityForResult(intent, 3545);
    }

    /**
     * Get the cache directory.
     *
     * @param context instance of the caller
     * @return cache directory file.
     */
    @NonNull
    static File getCacheDir(Context context) {
        return context.getExternalCacheDir() == null ? context.getCacheDir() : context.getExternalCacheDir();
    }

    /**
     * Check if the device has front camera or not?
     *
     * @param context context
     * @return true if the device has front camera.
     */
    @SuppressWarnings("deprecation")
    public static boolean isFrontCameraAvailable(@NonNull Context context) {
        int numCameras = Camera.getNumberOfCameras();
        return numCameras > 0 && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }


    /**
     * Rotate the bitmap by 90 degree.
     *
     * @param bitmap original bitmap
     * @return rotated bitmap
     */
    @WorkerThread
    static Bitmap rotateBitmap(@NonNull Bitmap bitmap, int rotation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
