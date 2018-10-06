package tuev.konstantin.adb;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class AdbProtocol {
    static final int ADB_HEADER_LENGTH = 24;
    private static final int AUTH_TYPE_TOKEN = 1;
    static final int CMD_AUTH = 1213486401;
    static final int CMD_CLSE = 1163086915;
    static final int CMD_CNXN = 1314410051;
    static final int CMD_OKAY = 1497451343;
    private static final int CMD_OPEN = 1313165391;
    static final int CMD_WRTE = 1163154007;
    private static final int CONNECT_MAXDATA = 4096;
    private static byte[] CONNECT_PAYLOAD = null;
    private static final int CONNECT_VERSION = 16777216;

    static final class AdbMessage {
        int arg0;
        int arg1;
        int checksum;
        public int command;
        int magic;
        byte[] payload;
        int payloadLength;

        AdbMessage() {
        }

        static AdbMessage parseAdbMessage(InputStream in) throws IOException {
            AdbMessage msg = new AdbMessage();
            ByteBuffer packet = ByteBuffer.allocate(AdbProtocol.ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            int dataRead = 0;
            do {
                int bytesRead = in.read(packet.array(), dataRead, 24 - dataRead);
                if (bytesRead < 0) {
                    throw new IOException("Stream closed");
                }
                dataRead += bytesRead;
            } while (dataRead < AdbProtocol.ADB_HEADER_LENGTH);
            msg.command = packet.getInt();
            msg.arg0 = packet.getInt();
            msg.arg1 = packet.getInt();
            msg.payloadLength = packet.getInt();
            msg.checksum = packet.getInt();
            msg.magic = packet.getInt();
            if (msg.payloadLength != 0) {
                msg.payload = new byte[msg.payloadLength];
                dataRead = 0;
                do {
                    int bytesRead = in.read(msg.payload, dataRead, msg.payloadLength - dataRead);
                    if (bytesRead < 0) {
                        throw new IOException("Stream closed");
                    }
                    dataRead += bytesRead;
                } while (dataRead < msg.payloadLength);
            }
            return msg;
        }
    }

    static {
        try {
            CONNECT_PAYLOAD = "host::\u0000".getBytes("UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
    }

    private static int getPayloadChecksum(byte[] payload) {
        int checksum = 0;
        for (byte b : payload) {
            if (b >= (byte) 0) {
                checksum += b;
            } else {
                checksum += b + AdbCrypto.KEY_LENGTH_BYTES;
            }
        }
        return checksum;
    }

    static boolean validateMessage(AdbMessage msg) {
        return msg.command == (~msg.magic) && (msg.payloadLength == 0 || getPayloadChecksum(msg.payload) == msg.checksum);
    }

    private static byte[] generateMessage(int cmd, int arg0, int arg1, byte[] payload) {
        ByteBuffer message;
        if (payload != null) {
            message = ByteBuffer.allocate(payload.length + ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        } else {
            message = ByteBuffer.allocate(ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        }
        message.putInt(cmd);
        message.putInt(arg0);
        message.putInt(arg1);
        if (payload != null) {
            message.putInt(payload.length);
            message.putInt(getPayloadChecksum(payload));
        } else {
            message.putInt(0);
            message.putInt(0);
        }
        message.putInt(~cmd);
        if (payload != null) {
            message.put(payload);
        }
        return message.array();
    }

    static byte[] generateConnect() {
        return generateMessage(CMD_CNXN, CONNECT_VERSION, CONNECT_MAXDATA, CONNECT_PAYLOAD);
    }

    static byte[] generateAuth(int type, byte[] data) {
        return generateMessage(CMD_AUTH, type, 0, data);
    }

    static byte[] generateOpen(int localId, String dest) throws UnsupportedEncodingException {
        ByteBuffer bbuf = ByteBuffer.allocate(dest.length() + AUTH_TYPE_TOKEN);
        bbuf.put(dest.getBytes("UTF-8"));
        bbuf.put((byte) 0);
        return generateMessage(CMD_OPEN, localId, 0, bbuf.array());
    }

    static byte[] generateWrite(int localId, int remoteId, byte[] data) {
        return generateMessage(CMD_WRTE, localId, remoteId, data);
    }

    static byte[] generateClose(int localId, int remoteId) {
        return generateMessage(CMD_CLSE, localId, remoteId, null);
    }

    static byte[] generateReady(int localId, int remoteId) {
        return generateMessage(CMD_OKAY, localId, remoteId, null);
    }
}
