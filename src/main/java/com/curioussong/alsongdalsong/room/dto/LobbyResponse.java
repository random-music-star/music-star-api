package com.curioussong.alsongdalsong.room.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class LobbyResponse {
    private List<RoomDTO> rooms;
    private int totalPages;
    private long totalElements;
    private int currentPage;
}
