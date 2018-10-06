package tuev.konstantin.adb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class AdbCrypto {
    private static final int KEY_LENGTH_BITS = 2048;
    static final int KEY_LENGTH_BYTES = 256;
    private static final int KEY_LENGTH_WORDS = 64;
    private static final int[] SIGNATURE_PADDING_AS_INT = new int[]{0, 1, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 0, 48, 33, 48, 9, 6, 5, 43, 14, 3, 2, 26, 5, 0, 4, 20};
    private static byte[] SIGNATURE_PADDING = new byte[SIGNATURE_PADDING_AS_INT.length];
    private AndroidBase64 base64;
    private KeyPair keyPair;

    static {
        for (int i = 0; i < SIGNATURE_PADDING.length; i++) {
            SIGNATURE_PADDING[i] = (byte) SIGNATURE_PADDING_AS_INT[i];
        }
    }

    private static byte[] convertRsaPublicKeyToAdbFormat(RSAPublicKey pubkey) {
        BigInteger r32 = BigInteger.ZERO.setBit(32);
        BigInteger n = pubkey.getModulus();
        BigInteger rr = BigInteger.ZERO.setBit(KEY_LENGTH_BITS).modPow(BigInteger.valueOf(2), n);
        BigInteger n0inv = n.remainder(r32).modInverse(r32);
        int[] myN = new int[KEY_LENGTH_WORDS];
        int[] myRr = new int[KEY_LENGTH_WORDS];
        for (int i = 0; i < KEY_LENGTH_WORDS; i++) {
            BigInteger[] res = rr.divideAndRemainder(r32);
            rr = res[0];
            myRr[i] = res[1].intValue();
            res = n.divideAndRemainder(r32);
            n = res[0];
            myN[i] = res[1].intValue();
        }
        ByteBuffer bbuf = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);
        bbuf.putInt(KEY_LENGTH_WORDS);
        bbuf.putInt(n0inv.negate().intValue());
        for (int i2 : myN) {
            bbuf.putInt(i2);
        }
        for (int i22 : myRr) {
            bbuf.putInt(i22);
        }
        bbuf.putInt(pubkey.getPublicExponent().intValue());
        return bbuf.array();
    }

    static AdbCrypto loadAdbKeyPair(AndroidBase64 base64, File privateKey, File publicKey) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        AdbCrypto crypto = new AdbCrypto();
        byte[] privKeyBytes = new byte[((int) privateKey.length())];
        byte[] pubKeyBytes = new byte[((int) publicKey.length())];
        FileInputStream privIn = new FileInputStream(privateKey);
        FileInputStream pubIn = new FileInputStream(publicKey);
        privIn.read(privKeyBytes);
        pubIn.read(pubKeyBytes);
        privIn.close();
        pubIn.close();
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        crypto.keyPair = new KeyPair(keyFactory.generatePublic(new X509EncodedKeySpec(pubKeyBytes)), keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privKeyBytes)));
        crypto.base64 = base64;
        return crypto;
    }

    static AdbCrypto generateAdbKeyPair(AndroidBase64 base64) throws NoSuchAlgorithmException {
        AdbCrypto crypto = new AdbCrypto();
        KeyPairGenerator rsaKeyPg = KeyPairGenerator.getInstance("RSA");
        rsaKeyPg.initialize(KEY_LENGTH_BITS);
        crypto.keyPair = rsaKeyPg.genKeyPair();
        crypto.base64 = base64;
        return crypto;
    }

    byte[] signAdbTokenPayload(byte[] payload) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
        c.init(1, this.keyPair.getPrivate());
        c.update(SIGNATURE_PADDING);
        return c.doFinal(payload);
    }

    byte[] getAdbPublicKeyPayload() throws IOException {
        byte[] convertedKey = convertRsaPublicKeyToAdbFormat((RSAPublicKey) this.keyPair.getPublic());
        String keyString = this.base64.encodeToString(convertedKey) +
                " unknown@unknown" +
                '\u0000';
        return keyString.getBytes("UTF-8");
    }

    void saveAdbKeyPair(File privateKey, File publicKey) throws IOException {
        FileOutputStream privOut = new FileOutputStream(privateKey);
        FileOutputStream pubOut = new FileOutputStream(publicKey);
        privOut.write(this.keyPair.getPrivate().getEncoded());
        pubOut.write(this.keyPair.getPublic().getEncoded());
        privOut.close();
        pubOut.close();
    }
}
