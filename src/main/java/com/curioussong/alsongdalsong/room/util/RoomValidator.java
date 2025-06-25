package com.curioussong.alsongdalsong.room.util;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.room.domain.Room;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

public class RoomValidator {

    private static final List<Integer> VALID_YEARS = List.of(1970, 1980, 1990, 2000, 2010, 2020, 2021, 2022, 2023, 2024);

    public static void validateYears(List<Integer> years) {
        if (years == null || years.isEmpty()) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "선택된 연도가 없습니다."
            );
        }

        for (Integer year : years) {
            if (year == null || !VALID_YEARS.contains(year)) {
                throw new HttpClientErrorException(
                        HttpStatus.BAD_REQUEST,
                        "지원하지 않는 연도가 포함되어 있습니다"
                );
            }
        }
    }

    public static void validateRoomSettings(String format, Integer maxPlayer, Integer maxGameRound, String password, String title){
        if("BOARD".equals(format)){
            if(maxPlayer>6 || maxPlayer<2) {
                throw new HttpClientErrorException(
                        HttpStatus.BAD_REQUEST,
                        "보드판 맵의 최대 인원은 2~6명입니다."
                );
            }
        } else if ("GENERAL".equals(format)) {
            if(maxPlayer>60 || maxPlayer<2){
                throw new HttpClientErrorException(
                        HttpStatus.BAD_REQUEST,
                        "점수판 맵의 최대 인원은 2~60명입니다. "
                );
            }
        }

        if(!(maxGameRound == 5 || maxGameRound == 10 || maxGameRound == 20 || maxGameRound == 30)){
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "게임 라운드는 5, 10, 20, 30 중 하나여야 합니다."
            );
        }

        if(password.length()>30){
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "방의 비밀번호는 30자 이하여야 합니다."
            );
        }

        if(title.length() > 15){
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "방의 제목은 15 이내여야 합니다."
            );
        }
    }

    public static void validateRoomHost(Room room, Member member) {
        if (!room.isHost(member)) {
            throw new HttpClientErrorException(
                    HttpStatus.UNAUTHORIZED,
                    "방 설정은 방장만 변경 가능합니다."
            );
        }
    }
}
