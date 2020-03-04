package com.knoban.gml;

import com.knoban.gml.server.GMLServer;

import java.io.IOException;

/**
 * Written for EAE-3720 at the University of Utah.
 * @author Alden Bansemer (kNoAPP)
 */
public class Main {

    public static void main(String args[]) {
        try {
            GMLServer server = new GMLServer(25500);
            server.open();
            server.startProcessingRequests();
        } catch(IOException e) {
            System.out.println("Unable to create the GMLServer: " + e.getMessage());
        }
    }
}
