package com.ssafy.lancit.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileDeleteEvent {

    private final String uploadPath;
}

//그냥 데이터 담는 객체.