package tuev.konstantin.adb;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.HashMap;

public class AdbConnection implements Closeable {
    private boolean connectAttempted;
    public boolean connected;
    private Thread connectionThread = createConnectionThread();
    private AdbCrypto crypto;
    private InputStream inputStream;
    private int lastLocalId = 0;
    private int maxData;
    private HashMap<Integer, AdbStream> openStreams = new HashMap<>();
    OutputStream outputStream;
    private boolean sentSignature;
    private Socket socket;

    private AdbConnection() {
    }

    public static AdbConnection create(Socket socket, AdbCrypto crypto) throws IOException {
        AdbConnection newConn = new AdbConnection();
        newConn.crypto = crypto;
        newConn.socket = socket;
        newConn.inputStream = socket.getInputStream();
        newConn.outputStream = socket.getOutputStream();
        socket.setTcpNoDelay(true);
        return newConn;
    }

    private Thread createConnectionThread() {
        final AdbConnection conn = this;
        return new Thread(() -> {
            while (!AdbConnection.this.connectionThread.isInterrupted()) {
                AdbProtocol.AdbMessage msg = null;
                try {
                    msg = AdbProtocol.AdbMessage.parseAdbMessage(AdbConnection.this.inputStream);
                if (AdbProtocol.validateMessage(msg)) {
                    switch (msg.command) {
                        case AdbProtocol.CMD_CLSE /*1163086915*/:
                        case AdbProtocol.CMD_WRTE /*1163154007*/:
                        case AdbProtocol.CMD_OKAY /*1497451343*/:
                            if (conn.connected) {
                                AdbStream waitingStream = (AdbStream) AdbConnection.this.openStreams.get(msg.arg1);
                                if (waitingStream != null) {
                                    synchronized (waitingStream) {
                                        if (msg.command == AdbProtocol.CMD_OKAY) {
                                            waitingStream.updateRemoteId(msg.arg0);
                                            waitingStream.readyForWrite();
                                            waitingStream.notify();
                                        } else if (msg.command == AdbProtocol.CMD_WRTE) {
                                            if (waitingStream.addPayload(msg.payload)) {
                                                waitingStream.sendReady();
                                            }
                                        } else if (msg.command == AdbProtocol.CMD_CLSE) {
                                            conn.openStreams.remove(msg.arg1);
                                            waitingStream.notifyClose();
                                        }
                                    }
                                    break;
                                }
                                continue;
                            } else {
                                continue;
                            }
                        case AdbProtocol.CMD_AUTH /*1213486401*/:
                            try {
                                if (msg.arg0 == 1) {
                                    byte[] packet;
                                    if (conn.sentSignature) {
                                        packet = AdbProtocol.generateAuth(3, conn.crypto.getAdbPublicKeyPayload());
                                    } else {
                                        packet = AdbProtocol.generateAuth(2, conn.crypto.signAdbTokenPayload(msg.payload));
                                        conn.sentSignature = true;
                                    }
                                    conn.outputStream.write(packet);
                                    conn.outputStream.flush();
                                    break;
                                }
                                break;
                            } catch (Exception e) {
                                break;
                            }
                        case AdbProtocol.CMD_CNXN /*1314410051*/:
                            synchronized (conn) {
                                conn.maxData = msg.arg1;
                                conn.connected = true;
                                conn.notifyAll();
                            }
                            break;
                        default:
                            break;
                    }
                }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            synchronized (conn) {
                AdbConnection.this.cleanupStreams();
                conn.notifyAll();
                conn.connectAttempted = false;
            }
        });
    }

    public void connect() throws IOException, InterruptedException {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        this.outputStream.write(AdbProtocol.generateConnect());
        this.outputStream.flush();
        this.connectAttempted = true;
        this.connectionThread.start();
        synchronized (this) {
            if (!this.connected) {
                wait();
            }
            if (!this.connected) {
                throw new IOException("Connection failed");
            }
        }
    }

    public AdbStream open(String destination) throws IOException, InterruptedException {
        int localId = this.lastLocalId + 1;
        this.lastLocalId = localId;
        if (this.connectAttempted) {
            synchronized (this) {
                if (!this.connected) {
                    wait();
                }
                if (!this.connected) {
                    throw new IOException("Connection failed");
                }
            }
            AdbStream stream = new AdbStream(this, localId);
            this.openStreams.put(localId, stream);
            this.outputStream.write(AdbProtocol.generateOpen(localId, destination));
            this.outputStream.flush();
            synchronized (stream) {
                stream.wait();
            }
            if (!stream.isClosed()) {
                return stream;
            }
            throw new ConnectException("Stream open actively rejected by remote peer");
        }
        throw new IllegalStateException("connect() must be called first");
    }

    private void cleanupStreams() {
        for (AdbStream s : this.openStreams.values()) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
        this.openStreams.clear();
    }

    public void close() throws IOException {
        if (this.connectionThread != null) {
            this.socket.close();
            this.connectionThread.interrupt();
            try {
                this.connectionThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
