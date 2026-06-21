package com.ssafy.lancit.domain.holiday.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HolidayDTO {
    private Long id;
    private LocalDate date;       // 공휴일 날짜
    private String name;          // 공휴일 명칭 (신정, 설날 등)
    private int year;             // 연도
    private boolean isHoliday;    // true = 공휴일, false = 대체공휴일

    @JsonGetter("isHoliday")
    public boolean isHoliday() {
        return isHoliday;
    }

    @JsonSetter("isHoliday")
    @JsonAlias("holiday")
    public void setHoliday(boolean isHoliday) {
        this.isHoliday = isHoliday;
    }
}
