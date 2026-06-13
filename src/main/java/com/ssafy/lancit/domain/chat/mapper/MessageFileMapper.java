package com.ssafy.lancit.domain.chat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MessageFileMapper {

	// 채팅으로 파일을 발송함
    int insert(
            @Param("messageId") Integer messageId,
            @Param("fileId") Integer fileId
    );

    // 메세지 아이디로 파일 아이디들 가져오기
    List<Integer> findFileIdsByMessageId(
            Integer messageId
    );

    // 메세지 아이디로 삭제하기
    int deleteByMessageId(
            Integer messageId
    );
}