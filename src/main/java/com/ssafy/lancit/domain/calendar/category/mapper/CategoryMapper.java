package com.ssafy.lancit.domain.calendar.category.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.calendar.category.dto.CategoryDTO;
import com.ssafy.lancit.global.enums.OwnerType;
 
@Mapper
public interface CategoryMapper {
    List<CategoryDTO> findAll(@Param("email") String email, @Param("ownerType") OwnerType ownerType);
    void insert(CategoryDTO dto);
    void update(@Param("categoryId") int categoryId, @Param("dto") CategoryDTO dto);
    void delete(int categoryId);
    void deleteByOwner(@Param("email") String email, @Param("ownerType") OwnerType ownerType);
}
 