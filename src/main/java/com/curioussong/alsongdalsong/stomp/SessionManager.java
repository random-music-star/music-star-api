package com.curioussong.alsongdalsong.stomp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
@Setter
public class SessionManager {
    private Map<String, Map<String, Pair<Long, String>>> sessionRoomMap = new HashMap<>(); // {세션 ID : {사용자 이름 : {채널 ID, 방 ID}}}
    private final Map<Long, Set<String>> channelUserMap = new ConcurrentHashMap<>();

    public void addSessionId(String sessionId, Long channelId, String roomId, String username) {
        sessionRoomMap.putIfAbsent(sessionId, new HashMap<>());
        sessionRoomMap.get(sessionId).put(username, Pair.of(channelId, roomId));
    }

    public void removeSessionId(String sessionId) {
        sessionRoomMap.remove(sessionId);
    }

    public void userEnterChannel(Long channelId, String username) {
        channelUserMap.computeIfAbsent(channelId, k -> ConcurrentHashMap.newKeySet()).add(username);
    }

    public void userLeaveChannel(Long channelId, String username) {
        Set<String> users = channelUserMap.get(channelId);
        if (users != null) {
            users.remove(username);
        }
    }

    public int getChannelUserCount(Long channelId) {
        Set<String> users = channelUserMap.get(channelId);
        return users.size();
    }
}
