package com.ssafy.lancit.common.util;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import com.ssafy.lancit.global.enums.Weekday;

// VARCHAR(100) 컬럼 <-> List<Weekday> 자바 객체 간 변환
// 저장 형식: "MON,TUE,WED,THU,FRI" (콤마 구분 문자열)
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class WeekdayListTypeHandler extends BaseTypeHandler<List<Weekday>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Weekday> parameter, JdbcType jdbcType) throws SQLException {
        String joined = parameter.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
        ps.setString(i, joined);
    }

    @Override
    public List<Weekday> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<Weekday> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<Weekday> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<Weekday> parse(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Weekday::valueOf)
                .collect(Collectors.toList());
    }
}