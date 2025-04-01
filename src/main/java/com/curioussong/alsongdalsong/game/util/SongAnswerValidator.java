package com.curioussong.alsongdalsong.game.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SongAnswerValidator {

    // 채팅의 모든 공백 제거 및 대문자 치환
    public static String extractAnswer(String answer) {
        return answer.replaceAll("(?<=\\S) (?=\\S)", "") // 공백이 한 개인 것들만 제거
                .replaceAll("[^가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9\\p{P}]", "") // 한글, 영어, 숫자, 특수문자만 남기고 제거
                .toUpperCase();
    }

    public static String convertNumbersToKorean(String text) {
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

    public static String convertNumbersToEnglish(String text) {
        return text.replaceAll("0", "zero")
                .replaceAll("1", "one")
                .replaceAll("2", "two")
                .replaceAll("3", "three")
                .replaceAll("4", "four")
                .replaceAll("5", "five")
                .replaceAll("6", "six")
                .replaceAll("7", "seven")
                .replaceAll("8", "eight")
                .replaceAll("9", "nine");
    }

    public static String convertSpecialCharactersToKorean(String text) {
        return text.replaceAll("%", "퍼센트")
                .replaceAll("&", "앤")
                .replaceAll("#", "샾")
                .replaceAll("@", "앳");
    }

    public static String convertSpecialCharactersToEnglish(String text) {
        return text.replaceAll("%", "PERCENT")
                .replaceAll("&", "AND")
                .replaceAll("#", "SHARP")
                .replaceAll("@", "AT");
    }

    public static String removeSomeSpecialCharacters(String text) {
        return text.replaceAll("[.=,'\"-]", "");
    }

    public static boolean isCorrectAnswer(String userAnswer, String koreanTitle, String englishTitle) {
        String processedUserAnswer = extractAnswer(userAnswer);

        List<String> correctAnswers = new ArrayList<>();

        // 원본 제목 추가
        correctAnswers.add(koreanTitle);

        // 공백 제거, 대문자 치환
        String preprocessedKoreanAnswer = extractAnswer(koreanTitle);
        correctAnswers.add(preprocessedKoreanAnswer);

        // 괄호 및 괄호 안의 내용 제거
        String removeParenthesesKoreanTitle = preprocessedKoreanAnswer.replaceAll("\\(.*?\\)", "");
        correctAnswers.add(removeParenthesesKoreanTitle);

        // 숫자를 발음하여 변환
        String substituteNumberKoreanTitle = convertNumbersToKorean(removeParenthesesKoreanTitle);
        correctAnswers.add(substituteNumberKoreanTitle);

        // 특수 문자 발음하여 변환
        String substituteSpecialKoreanTitle = convertSpecialCharactersToKorean(substituteNumberKoreanTitle);
        correctAnswers.add(substituteSpecialKoreanTitle);

        // 온점, 반점, 따옴표, 쌍따옴표 제거
        String removeSomeSpecialCharacterKoreanTitle = removeSomeSpecialCharacters(removeParenthesesKoreanTitle);
        correctAnswers.add(removeSomeSpecialCharacterKoreanTitle);

        if (!englishTitle.isBlank()) {
            correctAnswers.add(englishTitle);
            String preprocessedEnglishAnswer = extractAnswer(englishTitle);
            correctAnswers.add(preprocessedEnglishAnswer);
            String removeParenthesesEnglishTitle = preprocessedEnglishAnswer.replaceAll("\\(.*?\\)", "");
            correctAnswers.add(removeParenthesesEnglishTitle);
            String substituteNumberEnglishTitle = convertNumbersToEnglish(removeParenthesesEnglishTitle);
            correctAnswers.add(substituteNumberEnglishTitle);
            String substituteSpecialEnglishTitle = convertSpecialCharactersToEnglish(substituteNumberEnglishTitle);
            correctAnswers.add(substituteSpecialEnglishTitle);
            String removeSomeSpecialCharacterEnglishTitle = removeSomeSpecialCharacters(removeParenthesesEnglishTitle);
            correctAnswers.add(removeSomeSpecialCharacterEnglishTitle);
        }

        log.debug("사용자가 입력한 정답 : {}", processedUserAnswer);
        log.debug("정답 목록 : {}", correctAnswers);
        return correctAnswers.contains(processedUserAnswer);
    }

}
