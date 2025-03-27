package com.curioussong.alsongdalsong.game.dto.quiz;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class QuizResponse {
    private String songUrl;
    private Long timestamp;
}
