package com.ssafy.lancit.domain.calendar.category.mapper;

import com.ssafy.lancit.domain.calendar.category.dto.CategoryDTO;
import com.ssafy.lancit.global.enums.OwnerType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {
    List<CategoryDTO> findAll(@Param("email") String email, @Param("ownerType") OwnerType ownerType);
    void insert(CategoryDTO dto);
    void update(@Param("categoryId") int categoryId, @Param("dto") CategoryDTO dto);
    int delete(@Param("categoryId") int categoryId);
    boolean existsByIdAndOwner(@Param("categoryId") int categoryId,
                               @Param("email") String email,
                               @Param("ownerType") OwnerType ownerType);
    int deleteByIdAndOwner(@Param("categoryId") int categoryId,
                           @Param("email") String email,
                           @Param("ownerType") OwnerType ownerType);
    void deleteByOwner(@Param("email") String email, @Param("ownerType") OwnerType ownerType);

    // ★ OwnerCheckAspect 에서 사용
    String findOwnerEmailById(@Param("categoryId") int categoryId);
}
