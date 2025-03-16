package com.curioussong.alsongdalsong.util;

public class KoreanConsonantExtractor {
    private static final char[] CONSONANT =
            {'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'};

    public static String extractConsonants(String word) {
        StringBuilder consonants = new StringBuilder();

        for (char c : word.toCharArray()) {
            if (c >= '가' && c <= '힣') { // 한글 범위
                int index = (c - 0xAC00) / 588; // 초성 인덱스 계산
                consonants.append(CONSONANT[index]);
            } else {
                consonants.append(c); // 한글이 아니면 그대로 추가
            }
        }
        return consonants.toString();
    }
}
