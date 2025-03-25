package com.curioussong.alsongdalsong.game.util;

public class SongAnswerValidator {

    // 채팅의 모든 공백 제거 및 대문자 치환
    public static String extractAnswer(String answer) {
        return answer.replaceAll("\\s+", "")
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("[^가-힣a-zA-Z0-9]", "")
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

    public static boolean isCorrectAnswer(String userAnswer, String koreanTitle, String englishTitle) {
        String processedUserAnswer = extractAnswer(userAnswer);
        String processedKoreanAnswer = extractAnswer(koreanTitle);
        String koreanAnswerWithNumber = convertNumbersToKorean(processedKoreanAnswer);

        // 영어 제목이 있는 경우 한글/영어 둘 다 정답 처리
        if (englishTitle != null) {
            String processedEnglishAnswer = extractAnswer(englishTitle);
            return processedUserAnswer.equals(processedKoreanAnswer) ||
                    processedUserAnswer.equals(koreanAnswerWithNumber) ||
                    processedUserAnswer.equals(processedEnglishAnswer);
        }

        return processedUserAnswer.equals(processedKoreanAnswer) ||
                processedUserAnswer.equals(koreanAnswerWithNumber);
    }

}
