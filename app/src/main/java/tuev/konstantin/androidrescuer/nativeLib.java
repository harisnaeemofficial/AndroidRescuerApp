package tuev.konstantin.androidrescuer;

import android.content.Context;

import java.util.ArrayList;

public class nativeLib {
    static {
        System.loadLibrary("native-lib");
    }

    public native String urlPath(boolean home, Helper.url url);
    public static nativeLib getInstance() {
        return INSTANCE;
    }

    private static final nativeLib INSTANCE = new nativeLib();

    public native String urlPathImage(boolean home, String whichImage);
}
