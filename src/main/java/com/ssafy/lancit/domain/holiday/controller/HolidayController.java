package com.ssafy.lancit.domain.holiday.controller;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.holiday.dto.HolidayDTO;
import com.ssafy.lancit.domain.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    /**
     * 특정 연도 공휴일 목록 조회
     * 캘린더 프론트에서 빨간 날 표시 시 호출
     * GET /api/holidays?year=2026
     *
     * - Redis @Cacheable 캐싱 (365일 TTL)
     * - 인증 없이 조회 가능 (SecurityConfig permitAll 처리)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HolidayDTO>>> getHolidays(
            @RequestParam int year) {
        List<HolidayDTO> holidays = holidayService.getByYear(year);
        return ResponseEntity.ok(ApiResponse.ok(holidays));
    }
}