package com.knoban.gml.server;

import com.knoban.gml.requests.*;
import com.knoban.gml.streams.GMLInputStream;
import com.knoban.gml.streams.GMLOutputStream;
import com.knoban.gml.utils.Pair;
import com.knoban.gml.utils.Tools;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Written for EAE-3720 at the University of Utah.
 * @author Alden Bansemer (kNoAPP)
 */
public class GMLConnection {

    private GMLServer server;
    private Socket connection;
    private Thread listener;
    private GMLInputStream in;
    private GMLOutputStream out;
    private volatile Boolean isClosed;

    private Queue<Pair<Byte, RequestFulfillment>> queuedRequests = new LinkedList<Pair<Byte, RequestFulfillment>>();
    private ReadWriteLock queuedRequestsLock = new ReentrantReadWriteLock();

    protected UUID uuid;

    /**
     * Creates a new GMLConnection. This should only be called by GMLServer.
     * @param server - The GMLServer that called this constructor.
     * @param connection - The connection from which data is received and sent.
     */
    protected GMLConnection(GMLServer server, Socket connection) {
        this.server = server;
        this.connection = connection;
        this.uuid = UUID.randomUUID(); // Assign this connection a unique UUID.
    }

    /**
     * @return The unique UUID of this GMLConnection.
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * @return True, if connection is open. False, if connection has been closed, Null, if the connection can be opened.
     */
    public Boolean isClosed() {
        return isClosed;
    }

    /**
     * Connections may only be opened ONCE! Once opened, they process requests from the Socket until the
     * connection is closed by the client or server. This call runs on its own Thread.
     */
    public void open() {
        if(isClosed != null)
            return;

        isClosed = false;
        listener = new Thread(() -> {
            while(!isClosed) {
                try {
                    in = new GMLInputStream(connection.getInputStream());
                    out = new GMLOutputStream(connection.getOutputStream());

                    while(!isClosed) {
                        in.readS8();
                        in.skipPassHeader();
                        if(in.readS16() == RequestCode.HANDSHAKE) {
                            byte request = in.readS8();

                            /*
                             * Enqueue requests here with close attention to race conditions. Use the header's
                             * request code to determine the request type. If data must be passed, use the
                             * RequestFulfillment interface to create a class that can carry specific types
                             * of data to the processing queue.
                             *
                             * It is okay, safe, and encouraged to use null for the RequestFulfillment if no
                             * extra data must be passed to the handler.
                             */
                            switch(request) {
                                case RequestCode.USER_ID:
                                    queuedRequestsLock.writeLock().lock();
                                    queuedRequests.offer(new Pair(request, null));
                                    queuedRequestsLock.writeLock().unlock();
                                    break;
                            }
                        }
                    }
                } catch(SocketException | EOFException e) {
                    // Client issued disconnect.
                    System.out.println("Disconnect [Client]: " + Tools.formatSocket(connection));
                    close(); // Formally close this connection.
                } catch(IOException e) {
                    System.out.println("Unable to read input stream: " + e.getMessage());
                }
            }
        });
        listener.start();
    }

    /**
     * Immediately process the request queue. If called from the server's request processing, this is Thread safe.
     * Can be called elsewhere with caution for race conditions.
     */
    public void processRequests() {
        while(!queuedRequests.isEmpty()) {
            queuedRequestsLock.writeLock().lock();
            Pair<Byte, RequestFulfillment> request = queuedRequests.poll();
            queuedRequestsLock.writeLock().unlock();

            Byte requestCode = request.getKey();
            RequestFulfillment data = request.getValue();

            try {
                switch(requestCode) {
                    case RequestCode.USER_ID:
                        System.out.println(Tools.formatSocket(connection) + ": USER_ID");
                        out.writeS16(RequestCode.HANDSHAKE);
                        out.writeS8(requestCode);
                        out.writeS16((short) (uuid.toString().length() + 1)); // size + null terminator
                        out.writeString(uuid.toString());
                        break;
                }
            } catch(IOException e) {
                System.out.println("Unable to write output stream: " + e.getMessage());
            }
        }
    }

    /**
     * Close the connection. Once closed, this GMLConnection may not be reopened. A new connection must be created
     * from the client-side of things.
     */
    public void close() {
        if(isClosed == null || isClosed == true)
            return;
        isClosed = true;

        System.out.println("Disconnect [Server]: " + Tools.formatSocket(connection));

        server.connections.remove(this);

        try {
            connection.close();
            listener.join();
        } catch(IOException | InterruptedException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }

    /*
     * House keeping to properly identify and compare this class.
     */

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof GMLConnection))
            return false;

        GMLConnection player = (GMLConnection) o;
        return player.uuid.equals(uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
