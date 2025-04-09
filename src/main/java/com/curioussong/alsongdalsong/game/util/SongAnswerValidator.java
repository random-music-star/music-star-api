package com.curioussong.alsongdalsong.game.util;

import com.curioussong.alsongdalsong.game.domain.GameMode;
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

    private static boolean isCorrectAnswer(String processedUserAnswer, String koreanTitle, String englishTitle) {
        List<String> correctAnswers = getAllValidAnswers(koreanTitle, englishTitle);

        log.debug("사용자가 입력한 정답 : {}", processedUserAnswer);
        log.debug("정답 목록 : {}", correctAnswers);

        return correctAnswers.contains(processedUserAnswer);
    }

    public static boolean isCorrectAnswer(String userAnswer,
                                          GameMode gameMode,
                                          String korTitleFirst, String engTitleFirst,
                                          String korTitleSecond, String engTitleSecond) {
        String processedUserAnswer = extractAnswer(userAnswer);

        if (gameMode == GameMode.DUAL) {
            List<String> validAnswers1 = getAllValidAnswers(korTitleFirst, engTitleFirst);
            List<String> validAnswers2 = getAllValidAnswers(korTitleSecond, engTitleSecond);

            for (String ans1 : validAnswers1) {
                for (String ans2 : validAnswers2) {
                    if (processedUserAnswer.equals(ans1 + ans2) || processedUserAnswer.equals(ans2 + ans1)) {
                        return true;
                    }
                }
            }
            return false;
        }

        return isCorrectAnswer(processedUserAnswer, korTitleFirst, engTitleFirst);
    }

    private static String removeParenthesesContent(String input) {
        while (input.contains("(")) {
            input = input.replaceAll("\\([^()]*\\)", ""); // 가장 안쪽 괄호부터 제거
        }
        return input.trim();
    }

    private static List<String> getAllValidAnswers(String koreanTitle, String englishTitle) {
        List<String> validAnswers = new ArrayList<>();

        if (!koreanTitle.isBlank()) {
            // 원본 제목 추가
            validAnswers.add(koreanTitle);

            // 공백 제거, 대문자 치환
            String preprocessedKoreanAnswer = extractAnswer(koreanTitle);
            validAnswers.add(preprocessedKoreanAnswer);

            // 괄호 및 괄호 안의 내용 제거
            String removeParenthesesKoreanTitle = preprocessedKoreanAnswer.replaceAll("\\(.*?\\)", "");
            validAnswers.add(removeParenthesesKoreanTitle);

            // 숫자를 발음하여 변환
            String substituteNumberKoreanTitle = convertNumbersToKorean(removeParenthesesKoreanTitle);
            validAnswers.add(substituteNumberKoreanTitle);

            // 특수 문자 발음하여 변환
            String substituteSpecialKoreanTitle = convertSpecialCharactersToKorean(substituteNumberKoreanTitle);
            validAnswers.add(substituteSpecialKoreanTitle);

            // 온점, 반점, 따옴표, 쌍따옴표 제거
            String removeSomeSpecialCharacterKoreanTitle = removeSomeSpecialCharacters(removeParenthesesKoreanTitle);
            validAnswers.add(removeSomeSpecialCharacterKoreanTitle);
        }

        if (!englishTitle.isBlank()) {
            validAnswers.add(englishTitle);
            String preprocessedEnglishAnswer = extractAnswer(englishTitle);
            validAnswers.add(preprocessedEnglishAnswer);
            String removeParenthesesEnglishTitle = removeParenthesesContent(preprocessedEnglishAnswer);
            validAnswers.add(removeParenthesesEnglishTitle);
            String substituteNumberEnglishTitle = convertNumbersToEnglish(removeParenthesesEnglishTitle);
            validAnswers.add(substituteNumberEnglishTitle);
            String substituteSpecialEnglishTitle = convertSpecialCharactersToEnglish(substituteNumberEnglishTitle);
            validAnswers.add(substituteSpecialEnglishTitle);
            String removeSomeSpecialCharacterEnglishTitle = removeSomeSpecialCharacters(removeParenthesesEnglishTitle);
            validAnswers.add(removeSomeSpecialCharacterEnglishTitle);
        }

        return validAnswers.stream()
                .distinct()
                .toList();
    }
}
