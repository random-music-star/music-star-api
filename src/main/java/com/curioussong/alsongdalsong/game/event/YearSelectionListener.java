package com.curioussong.alsongdalsong.game.event;

import com.curioussong.alsongdalsong.game.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class YearSelectionListener {

    private final GameService gameService;

    @EventListener
    public void handleYearSelectionEvent(YearSelectionEvent event) {
        log.info("Year selection event: {}", event.selectedYears());
        gameService.updateSongYears(event.roomId(), event.selectedYears());
    }
}