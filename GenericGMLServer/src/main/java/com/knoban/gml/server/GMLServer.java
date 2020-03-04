package com.knoban.gml.server;

import com.knoban.gml.utils.Tools;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Written for EAE-3720 at the University of Utah.
 * @author Alden Bansemer (kNoAPP)
 */
public class GMLServer {

    protected HashSet<GMLConnection> connections = new HashSet<GMLConnection>();
    private ReadWriteLock rwLockConnections = new ReentrantReadWriteLock();

    private ServerSocket serverSocket;
    private int port;
    private Thread connectionListener;
    private volatile boolean isListening;

    private Thread requestProcessor;
    private volatile boolean isProcessingRequests;

    /**
     * Create a new GMLServer instance running on a specified port.
     * @param port - The port to run on.
     * @throws IOException - If the server socket cannot be created.
     */
    public GMLServer(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
    }

    /**
     * @return The port this server runs on.
     */
    public int getPort() {
        return port;
    }

    /**
     * Begin listening for new connections to the server. Each connection is
     * given its own thread to run on for incoming data.
     */
    public void open() {
        if(isListening)
            return;

        isListening = true;
        connectionListener = new Thread(() -> {
            while(isListening) {
                try {
                    Socket connection = serverSocket.accept();
                    System.out.println("Connected [CLIENT/SERVER]: " + Tools.formatSocket(connection));
                    GMLConnection conn = new GMLConnection(GMLServer.this, connection);
                    rwLockConnections.writeLock().lock();
                    connections.add(conn);
                    rwLockConnections.writeLock().unlock();
                    conn.open();
                } catch(IOException e) {
                    System.out.println("Failed to accept connection: " + e.getMessage());
                }
            }
        });
        connectionListener.start();
    }

    /**
     * Begin processing requests from all connections. All pending requests are processed on each connection
     * before moving on to the next. Thus, data handled from processed requests is Thread-safe between connections.
     */
    public void startProcessingRequests() {
        isProcessingRequests = true;
        requestProcessor = new Thread(() -> {
            while(isProcessingRequests) {
                rwLockConnections.readLock().lock();
                List<GMLConnection> connections = new ArrayList<GMLConnection>(this.connections); // Avoids concurrent modification
                rwLockConnections.readLock().unlock();
                for(GMLConnection player : connections) {
                    player.processRequests();
                }
            }
        });
        requestProcessor.start();
    }

    /**
     * Stop processing requests. This does not empty the request queue nor does it block additional requests. It only
     * stops the server's processing and response to them. Restarting processing requests will handle all received
     * requests during the time processing was stopped.
     */
    public void stopProcessingRequests() {
        isProcessingRequests = false;
        try {
            requestProcessor.join();
        } catch(InterruptedException e) {
            System.out.println("Got interrupted while stopping request processing: " + e.getMessage());
        }
    }

    /**
     * Close the server. All current connections will be closed and any pending requests will not be handled. The
     * server may be reopened with open() after calling this. New connections will then be accepted.
     */
    public void close() {
        isListening = false;

        stopProcessingRequests();

        new ArrayList<GMLConnection>(connections).stream().forEach((p) -> p.close()); // Avoids concurrent modification

        try {
            serverSocket.close();
            connectionListener.join();
            connectionListener = null;
        } catch(InterruptedException | IOException e) {
            System.out.println("Got interrupted while stopping incoming connections: " + e.getMessage());
        }
    }
}
