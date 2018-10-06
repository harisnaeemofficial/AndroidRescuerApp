package tuev.konstantin.adb;

import android.os.Looper;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class AdbUtils {
    private static final String PRIVATE_KEY_NAME = "private.key";
    private static final String PUBLIC_KEY_NAME = "public.key";

    public static AdbCrypto readCryptoConfig(File dataDir) {
        File pubKey = new File(dataDir, PUBLIC_KEY_NAME);
        File privKey = new File(dataDir, PRIVATE_KEY_NAME);
        if (!pubKey.exists() || !privKey.exists()) {
            return null;
        }
        try {
            return AdbCrypto.loadAdbKeyPair(new AndroidBase64(), privKey, pubKey);
        } catch (Exception e) {
            return null;
        }
    }

    public static AdbCrypto writeNewCryptoConfig(File dataDir) {
        File pubKey = new File(dataDir, PUBLIC_KEY_NAME);
        File privKey = new File(dataDir, PRIVATE_KEY_NAME);
        try {
            AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(new AndroidBase64());
            crypto.saveAdbKeyPair(privKey, pubKey);
            return crypto;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean safeClose(final Closeable c) {
        if (c == null) {
            return false;
        }
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            try {
                c.close();
            } catch (IOException e) {
                return false;
            }
        }
        new Thread(() -> {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }).start();
        return true;
    }
}
