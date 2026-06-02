package com.ssafy.lancit.domain.holiday.mapper;

import com.ssafy.lancit.domain.holiday.dto.HolidayDTO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HolidayMapper {

    // HolidayMapper.xml → SELECT * FROM holiday WHERE year = #{year} ORDER BY date ASC
    List<HolidayDTO> findByYear(int year);

    // HolidayMapper.xml → SELECT COUNT(*) > 0 FROM holiday WHERE date = #{date}
    boolean existsByDate(LocalDate date);

    // 중복 수집 방지 (배치에서 사용)
    boolean existsByYear(int year);

    // 재수집 시 기존 데이터 삭제 (배치에서 사용)
    void deleteByYear(int year);
}