package com.ssafy.lancit.domain.user.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.lancit.domain.user.dto.UserDTO;
 
@Mapper
public interface UserMapper {
    UserDTO findByEmail(String email);
    void insert(UserDTO dto);
    void update(UserDTO dto);
    void delete(String email);
    boolean existsByEmail(String email);
}
 
