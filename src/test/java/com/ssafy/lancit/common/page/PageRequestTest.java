package com.ssafy.lancit.common.page;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;

class PageRequestTest {

    @Test
    @DisplayName("0 이하 페이지와 크기는 기본값으로 보정된다")
    void invalidPageAndSize_useDefaultsConsistently() {
        PageRequest request = new PageRequest();
        request.setPage(0);
        request.setSize(0);

        PageResponse<String> response = PageResponse.of(List.of(), 25, request);

        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getSize()).isEqualTo(10);
        assertThat(request.getOffset()).isZero();
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.isHasPrev()).isFalse();
    }

    @Test
    @DisplayName("페이지 크기는 최대 100으로 제한된다")
    void oversizedPage_isCapped() {
        PageRequest request = new PageRequest();
        request.setPage(2);
        request.setSize(1_000);

        assertThat(request.getSize()).isEqualTo(100);
        assertThat(request.getOffset()).isEqualTo(100);
    }

    @Test
    @DisplayName("매우 큰 페이지의 offset은 음수로 오버플로되지 않는다")
    void hugePage_offsetDoesNotOverflow() {
        PageRequest request = new PageRequest();
        request.setPage(Integer.MAX_VALUE);
        request.setSize(100);

        assertThat(request.getOffset()).isEqualTo(Integer.MAX_VALUE);
    }
}
