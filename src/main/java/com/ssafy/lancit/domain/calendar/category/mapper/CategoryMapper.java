package com.ssafy.lancit.domain.calendar.category.mapper;

import com.ssafy.lancit.domain.calendar.category.dto.CategoryDTO;
import com.ssafy.lancit.global.enums.OwnerType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {
    List<CategoryDTO> findByOwner(@Param("email") String email, @Param("ownerType") OwnerType ownerType);

    CategoryDTO findByIdAndOwner(@Param("categoryId") int categoryId,
                                 @Param("email") String email,
                                 @Param("ownerType") OwnerType ownerType);

    int insert(CategoryDTO category);

    int update(CategoryDTO category);

    int deleteByIdAndOwner(@Param("categoryId") int categoryId,
                           @Param("email") String email,
                           @Param("ownerType") OwnerType ownerType);

    int countTasksByCategoryIdAndOwner(@Param("categoryId") int categoryId,
                                       @Param("email") String email,
                                       @Param("ownerType") OwnerType ownerType);

    void deleteByOwner(@Param("email") String email, @Param("ownerType") OwnerType ownerType);

    // ★ OwnerCheckAspect 에서 사용
    String findOwnerEmailById(@Param("categoryId") int categoryId);
}
