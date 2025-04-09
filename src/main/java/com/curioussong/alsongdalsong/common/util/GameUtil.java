package com.curioussong.alsongdalsong.common.util;

public class GameUtil {

    private GameUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
