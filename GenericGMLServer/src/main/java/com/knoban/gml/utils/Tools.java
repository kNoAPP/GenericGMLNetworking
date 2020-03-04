package com.knoban.gml.utils;

import java.net.Socket;
import java.util.Random;

/**
 * Written for EAE-3720 at the University of Utah.
 * @author Alden Bansemer (kNoAPP)
 */
public class Tools {

    /**
     * Generate a random int.
     * @param min - Minimum inclusive integer
     * @param max - Maximum inclusive integer
     * @return The random number
     */
    public static int randomNumber(int min, int max) {
        Random rand = new Random();
        int val = rand.nextInt(max - min + 1) + min;
        return val;
    }

    /**
     * Output a Socket's IP and port.
     * @param socket - The Socket to output from
     * @return - A formatted String with the Socket's IP and port.
     */
    public static String formatSocket(Socket socket) {
        return socket.getInetAddress() + ":" + socket.getPort();
    }
}
