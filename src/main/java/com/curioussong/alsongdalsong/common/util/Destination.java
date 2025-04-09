package com.curioussong.alsongdalsong.common.util;

public class Destination {

    private Destination() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String channel(long channelId) {
        return String.format("/topic/channel/%d", channelId);
    }

    public static String room(long channelId, String roomId) {
        return String.format("/topic/channel/%d/room/%s", channelId, roomId);
    }
}
