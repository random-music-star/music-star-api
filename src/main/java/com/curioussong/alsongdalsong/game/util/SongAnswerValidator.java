package com.curioussong.alsongdalsong.game.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SongAnswerValidator {

    // 채팅의 모든 공백 제거 및 대문자 치환
    public String extractAnswer(String answer) {
        return answer.replaceAll("\\s+", "")
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("[^가-힣ㄱ-ㅎa-zA-Z0-9!@#$%^&*()]", "")
                .toUpperCase();
    }

    public String convertNumbersToKorean(String text) {
        return text.replaceAll("0", "영")
                .replaceAll("1", "일")
                .replaceAll("2", "이")
                .replaceAll("3", "삼")
                .replaceAll("4", "사")
                .replaceAll("5", "오")
                .replaceAll("6", "육")
                .replaceAll("7", "칠")
                .replaceAll("8", "팔")
                .replaceAll("9", "구");
    }

    public String convertSpecialCharactersToKorean(String text) {
        return text.replaceAll("%", "퍼센트")
                .replaceAll("&", "앤");
    }

    public boolean isCorrectAnswer(String userAnswer, String koreanTitle, String englishTitle) {
        String processedUserAnswer = extractAnswer(userAnswer);

        String processedKoreanAnswer = extractAnswer(koreanTitle);
        String processedEnglishAnswer = null;
        if (!englishTitle.isBlank()) {
            processedEnglishAnswer = extractAnswer(englishTitle);
        }
        String koreanAnswerWithNumber = convertNumbersToKorean(processedKoreanAnswer);
        String koreanAnswerWithSpecialCharacters = convertSpecialCharactersToKorean(processedKoreanAnswer);
        String koreanAnswerWithNumberAndSpecialCharacters = convertSpecialCharactersToKorean(koreanAnswerWithNumber);
        String koreanAnswerWithSpecialCharactersAndNumber = convertNumbersToKorean(koreanAnswerWithSpecialCharacters);

        List<String> correctAnswers = new ArrayList<>();
        correctAnswers.add(processedKoreanAnswer);
        if (processedEnglishAnswer != null) {
            correctAnswers.add(processedEnglishAnswer);
        }
        correctAnswers.add(koreanAnswerWithNumber);
        correctAnswers.add(koreanAnswerWithSpecialCharacters);
        correctAnswers.add(koreanAnswerWithNumberAndSpecialCharacters);
        correctAnswers.add(koreanAnswerWithSpecialCharactersAndNumber);

        log.info("user's Answer: " + processedUserAnswer);
        log.info("correct answers: " + correctAnswers);
        return correctAnswers.contains(processedUserAnswer);
    }

}
