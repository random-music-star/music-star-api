package com.curioussong.alsongdalsong.game.event;

import java.util.List;

public record YearSelectionEvent(Long roomId, List<Integer> selectedYears) {
}
