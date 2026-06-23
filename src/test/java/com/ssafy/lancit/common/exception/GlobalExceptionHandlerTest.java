package com.ssafy.lancit.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.lancit.common.page.dto.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BindingTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("잘못된 enum 쿼리 파라미터는 400을 반환한다")
    void invalidEnum_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/enum").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("숫자 모델 바인딩 실패는 400을 반환한다")
    void invalidModelAttribute_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/page").param("page", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("잘못된 JSON 본문은 400을 반환한다")
    void malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("필수 쿼리 파라미터 누락은 400을 반환한다")
    void missingParameter_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/enum"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private enum TestStatus {
        OPEN
    }

    private record TestBody(String name) {
    }

    @RestController
    private static class BindingTestController {

        @GetMapping("/test/enum")
        void enumParameter(@RequestParam TestStatus status) {
        }

        @GetMapping("/test/page")
        void pageParameter(@ModelAttribute PageRequest pageRequest) {
        }

        @PostMapping("/test/body")
        void body(@RequestBody TestBody body) {
        }
    }
}
