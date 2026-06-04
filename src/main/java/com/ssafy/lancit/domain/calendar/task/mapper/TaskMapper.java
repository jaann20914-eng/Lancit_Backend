package com.ssafy.lancit.domain.calendar.task.mapper;

import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.global.enums.OwnerType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskMapper {

    List<TaskDTO> findByOwner(@Param("email") String email,
                              @Param("ownerType") OwnerType ownerType);

    List<TaskDTO> findByOwnerAndCategory(@Param("email") String email,
                                         @Param("ownerType") OwnerType ownerType,
                                         @Param("categoryId") int categoryId);

    // 월간 일정 조회 - email + ownerType 으로 내 일정만 필터, categoryId 있으면 카테고리 필터
    List<TaskDTO> findMonthly(@Param("email") String email,
                              @Param("ownerType") OwnerType ownerType,
                              @Param("year") int year,
                              @Param("month") int month,
                              @Param("categoryId") Integer categoryId);

    // 일정 상세 조회
    TaskDTO findByIdAndOwner(@Param("taskId") int taskId,
                             @Param("email") String email,
                             @Param("ownerType") OwnerType ownerType);

    // OwnerCheckAspect 에서 소유자 검증용
    String findOwnerEmailById(@Param("taskId") int taskId);

    // 일정 등록
    int insert(TaskDTO task);

    // 일정 수정
    int update(TaskDTO task);

    // 일정 삭제
    int deleteByIdAndOwner(@Param("taskId") int taskId,
                           @Param("email") String email,
                           @Param("ownerType") OwnerType ownerType);

    // 회원 탈퇴 시 소유자 기준 전체 삭제
    void deleteByOwner(@Param("email") String email, @Param("ownerType") OwnerType ownerType);

    // 카테고리 삭제 전 연결된 일정을 같은 소유자의 다른 카테고리로 이동
    int updateCategoryIdByCategoryIdAndOwner(@Param("categoryId") int categoryId,
                                             @Param("moveToCategoryId") int moveToCategoryId,
                                             @Param("email") String email,
                                             @Param("ownerType") OwnerType ownerType);

    // TaskScheduler 에서 납기일 D-7/3/1/0 알림 대상 조회
    List<TaskDTO> findDeadlineTasks(@Param("daysUntilDeadline") int daysUntilDeadline);
}
