package com.ssafy.lancit.domain.calendar.task.mapper;

import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.global.enums.OwnerType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskMapper {

    // 월간 일정 조회 - email + ownerType 으로 내 일정만 필터, categoryId 있으면 카테고리 필터
    List<TaskDTO> findMonthly(@Param("email") String email,
                              @Param("ownerType") OwnerType ownerType, // ★ ownerType 추가
                              @Param("year") int year,
                              @Param("month") int month,
                              @Param("categoryId") Integer categoryId);

    // 일정 상세 조회
    TaskDTO findById(int taskId);

    // OwnerCheckAspect 에서 소유자 검증용
    String findOwnerEmailById(int taskId);

    // 일정 등록
    void insert(TaskDTO dto);

    // 일정 수정 - null 필드 UPDATE 제외 (XML <if test> 처리)
    void update(@Param("taskId") int taskId, @Param("dto") TaskDTO dto);

    // 일정 삭제
    void delete(int taskId);

    // 회원 탈퇴 시 소유자 기준 전체 삭제
    void deleteByOwner(@Param("email") String email, @Param("ownerType") OwnerType ownerType);

    // 카테고리 삭제 전 Task 존재 여부 확인용 (CategoryService.delete() 에서 사용)
    int countByCategory(int categoryId);

    // 카테고리 삭제 시 연관 일정의 categoryId 일괄 이동
    int updateCategoryByCategory(@Param("categoryId") int categoryId,
                                 @Param("moveToCategoryId") int moveToCategoryId,
                                 @Param("email") String email,
                                 @Param("ownerType") OwnerType ownerType);

    // TaskScheduler 에서 납기일 D-7/3/1/0 알림 대상 조회
    List<TaskDTO> findDeadlineTasks(int daysUntilDeadline);
}
