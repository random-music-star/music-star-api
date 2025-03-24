package com.curioussong.alsongdalsong.stomp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Getter
@Setter
public class SessionManager {
    private Map<String, Map<String, Pair<Long, Long>>> sessionRoomMap = new HashMap<>(); // {세션 ID : {사용자 이름 : {채널 ID, 방 ID}}}

    public void addSessionId(String sessionId, Long channelId, Long roomId, String username) {
        sessionRoomMap.putIfAbsent(sessionId, new HashMap<>());
        sessionRoomMap.get(sessionId).put(username, Pair.of(channelId, roomId));
    }

    public void removeSessionId(String sessionId) {
        sessionRoomMap.remove(sessionId);
    }
}
