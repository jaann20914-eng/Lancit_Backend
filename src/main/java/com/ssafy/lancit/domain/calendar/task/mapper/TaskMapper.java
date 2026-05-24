package com.ssafy.lancit.domain.calendar.task.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.global.enums.OwnerType;
 
@Mapper
public interface TaskMapper {
    List<TaskDTO> findMonthly(@Param("email") String email, @Param("year") int year, @Param("month") int month, @Param("categoryId") Integer categoryId);
    TaskDTO findById(int taskId);
    String findOwnerEmail(int taskId);
    void insert(TaskDTO dto);
    void update(@Param("taskId") int taskId, @Param("dto") TaskDTO dto);
    void delete(int taskId);
    void deleteByOwner(@Param("email") String email, @Param("ownerType") OwnerType ownerType);
    int countByCategory(int categoryId);
    List<TaskDTO> findDeadlineTasks(int daysUntilDeadline);
}
