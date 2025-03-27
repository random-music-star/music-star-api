package com.curioussong.alsongdalsong.game.event;

import java.util.List;

public record YearSelectionEvent(String roomId, List<Integer> selectedYears) {
}
