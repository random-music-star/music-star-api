package com.curioussong.alsongdalsong.common.util;

import java.util.List;
import java.util.regex.Pattern;

public class BadWordFilter {

    private static final List<Pattern> BAD_WORD_PATTERNS = List.of(
            // 대표적인 욕설들 (한글, 초성 조합, 알파벳 유사표기)
            Pattern.compile("씨발", Pattern.CASE_INSENSITIVE),
            Pattern.compile("씨부럴", Pattern.CASE_INSENSITIVE),
            Pattern.compile("시부럴", Pattern.CASE_INSENSITIVE),
            Pattern.compile("시벌", Pattern.CASE_INSENSITIVE),
            Pattern.compile("씨벌", Pattern.CASE_INSENSITIVE),
            Pattern.compile("쒸벌", Pattern.CASE_INSENSITIVE),
            Pattern.compile("씨불", Pattern.CASE_INSENSITIVE),
            Pattern.compile("씨[ㅂb][ㅏa]?[ㄹr]?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ㅅ[ㅂb]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("개(새|색|새끼|색히|쉐이|년|놈|자식|쉑|씹|씨발|시발)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("병[ㅅ신쉰][ㅣi]?[ㄴn]?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("븅[ㅅ신][ㅣi]?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("미친[놈년새]?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ㅁㅊ", Pattern.CASE_INSENSITIVE),
            Pattern.compile("좆|ㅈ같|ㅈㄹ|ㅈ도|ㅈ문", Pattern.CASE_INSENSITIVE),
            Pattern.compile("보지|자지|씹|질싸|딸딸이", Pattern.CASE_INSENSITIVE),
            Pattern.compile("지랄|ㅈㄹ|염병|열받|개같", Pattern.CASE_INSENSITIVE),
            Pattern.compile("썅|꺼져|닥쳐|죽어|뒤져|닥치", Pattern.CASE_INSENSITIVE),
            Pattern.compile("후장|뒷구멍|항문", Pattern.CASE_INSENSITIVE),
            Pattern.compile("빠구리|쌍놈|쌍년", Pattern.CASE_INSENSITIVE),
            Pattern.compile("애미|애비|느금|느그엄마|느금마|느개비|느금빠|느검마|느갭|느검", Pattern.CASE_INSENSITIVE),
            Pattern.compile("fuck|shit|bitch|asshole|dick|pussy", Pattern.CASE_INSENSITIVE),
            Pattern.compile("fuxx|sh1t|bi7ch|d1ck", Pattern.CASE_INSENSITIVE),
            Pattern.compile("tlqkf|rlqhs|qkfrp|sibal|ssibal|qudtls|Tlqkf|rotoRl|roTlqkf", Pattern.CASE_INSENSITIVE) // 한글을 영어로 친 욕설
    );

    public static String filter(String input) {
        String result = input;
        for (Pattern pattern : BAD_WORD_PATTERNS) {
            result = pattern.matcher(result).replaceAll("****");
        }
        return result;
    }

    public static boolean containsBadWord(String input) {
        for (Pattern pattern : BAD_WORD_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                return true;
            }
        }
        return false;
    }
}
