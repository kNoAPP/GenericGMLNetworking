package com.knoban.gml.utils;

import java.util.Arrays;
import java.util.Random;

/**
 * Written for EAE-3720 at the University of Utah.
 * @author Alden Bansemer (kNoAPP)
 */
public class RoomCode {

    private char[] code;

    /**
     * Generates a random 4-letter room code. This code is letters only and has 456,976 different possibilities.
     * Warning! Room code does not check for vulgarities. It is entirely possible to get a room code like FUCK,
     * ASSS, BTCH, or NIGG.
     */
    public RoomCode() {
        code = new char[4];

        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());
        for(int i=0; i<4; i++) {
            code[i] = (char) (rand.nextInt(26) + 65);
        }
    }

    /**
     * Create a room code from a String. The String must be 4 characters in length. No checks are made on if the
     * passed String is letters-only.
     * @param s - The String to build a RoomCode from.
     */
    public RoomCode(String s) {
        if(s.length() != 4)
            throw new IllegalArgumentException("Constructed room code does not have 4 characters!");

        s = s.toUpperCase();
        code = new char[4];
        for(int i=0; i<4; i++) {
            code[i] = s.charAt(i);
        }
    }

    /**
     * House keeping for value comparisons.
     */

    @Override
    public String toString() {
        return String.valueOf(code);
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof RoomCode))
            return false;

        RoomCode rc = (RoomCode) o;
        for(int i=0; i<4; i++) {
            if(rc.code[i] != code[i])
                return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(code);
    }
}
