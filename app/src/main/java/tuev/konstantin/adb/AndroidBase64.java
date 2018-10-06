package tuev.konstantin.adb;

import android.util.Base64;

class AndroidBase64 {
    String encodeToString(byte[] data) {
        return Base64.encodeToString(data, 2);
    }
}
