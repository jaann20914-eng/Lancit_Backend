package com.ssafy.lancit.domain.holiday.service;

import com.ssafy.lancit.domain.holiday.dto.HolidayDTO;
import com.ssafy.lancit.domain.holiday.mapper.HolidayMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

// 공휴일 조회 - Redis @Cacheable 365일 TTL (연 1회 배치 수집 데이터)
@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayMapper holidayMapper;

    // 연도별 공휴일 목록 조회
    // ★ Redis "holiday:{year}" 캐싱 TTL 365일
    // 최초 조회 시 DB → Redis 저장, 이후 Redis 에서 즉시 반환
    @Cacheable(value = "holiday", key = "#year")
    public List<HolidayDTO> getByYear(int year) {
        return holidayMapper.findByYear(year);
    }

    // 특정 날짜 공휴일 여부 확인
    // ★ @Cacheable 제거 - holiday 캐시는 List<HolidayDTO> 타입이라 boolean 저장 시 타입 충돌
    //   대신 getByYear() 로 해당 연도 목록 조회 후 contains 로 확인하는 방식 권장
    //   TaskScheduler 에서 납기일이 공휴일이면 알림 제외 처리 시 사용
    public boolean isHoliday(LocalDate date) {
        return holidayMapper.existsByDate(date);
    }
}