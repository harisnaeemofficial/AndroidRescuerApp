package tuev.konstantin.adb;

import java.io.Closeable;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdbStream implements Closeable {
    private AdbConnection adbConn;
    private boolean isClosed;
    private int localId;
    private int maxQueuedBuffers;
    private final Queue<byte[]> readQueue;
    private int remoteId;
    private AtomicBoolean writeReady;

    AdbStream(AdbConnection adbConn, int localId) {
        this(adbConn, localId, 5);
    }

    private AdbStream(AdbConnection adbConn, int localId, int maxQueuedBuffers) {
        this.adbConn = adbConn;
        this.localId = localId;
        this.readQueue = new ConcurrentLinkedQueue<>();
        this.writeReady = new AtomicBoolean(false);
        this.isClosed = false;
        this.maxQueuedBuffers = maxQueuedBuffers;
    }

    boolean addPayload(byte[] payload) {
        boolean z;
        synchronized (this.readQueue) {
            this.readQueue.add(payload);
            this.readQueue.notifyAll();
            z = this.readQueue.size() <= this.maxQueuedBuffers;
        }
        return z;
    }

    void sendReady() throws IOException {
        this.adbConn.outputStream.write(AdbProtocol.generateReady(this.localId, this.remoteId));
        this.adbConn.outputStream.flush();
    }

    void updateRemoteId(int remoteId) {
        this.remoteId = remoteId;
    }

    void readyForWrite() {
        this.writeReady.set(true);
    }

    void notifyClose() {
        this.isClosed = true;
        synchronized (this) {
            notifyAll();
        }
        synchronized (this.readQueue) {
            this.readQueue.notifyAll();
        }
    }

    public void write(String payload) throws IOException, InterruptedException {
        write(payload.getBytes("UTF-8"), false);
        write(new byte[]{(byte) 0}, true);
    }

    public void write(byte[] payload) throws IOException, InterruptedException {
        write(payload, true);
    }

    public void write(byte[] payload, boolean flush) throws IOException, InterruptedException {
        synchronized (this) {
            while (!this.isClosed && !this.writeReady.compareAndSet(true, false)) {
                wait();
            }
            if (this.isClosed) {
                throw new IOException("Stream closed");
            }
        }
        this.adbConn.outputStream.write(AdbProtocol.generateWrite(this.localId, this.remoteId, payload));
        if (flush) {
            this.adbConn.outputStream.flush();
        }
    }

    public void close() throws IOException {
        synchronized (this) {
            if (this.isClosed) {
                return;
            }
            notifyClose();
            this.adbConn.outputStream.write(AdbProtocol.generateClose(this.localId, this.remoteId));
            this.adbConn.outputStream.flush();
        }
    }

    boolean isClosed() {
        return this.isClosed;
    }
}
