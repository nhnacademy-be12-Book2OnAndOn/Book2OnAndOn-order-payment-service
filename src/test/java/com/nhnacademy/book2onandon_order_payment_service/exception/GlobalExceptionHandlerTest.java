package com.nhnacademy.book2onandon_order_payment_service.exception;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nhnacademy.book2onandon_order_payment_service.payment.exception.AmountMismatchException;
import feign.FeignException;
import feign.Request;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("NotFoundException 발생 시 404 응답을 반환한다")
    void handleNotFoundExceptionTest() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Not found error"));
    }

    @Test
    @DisplayName("DuplicateException 발생 시 409 응답을 반환한다")
    void handleDuplicateExceptionTest() throws Exception {
        mockMvc.perform(get("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Duplicate error"));
    }

    @Test
    @DisplayName("AmountMismatchException 발생 시 400 응답을 반환한다")
    void handleAmountMismatchExceptionTest() throws Exception {
        mockMvc.perform(get("/test/amount-mismatch"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Amount mismatch"));
    }

    @Test
    @DisplayName("FeignException 발생 시 외부 API 상태 코드를 유지하여 응답한다")
    void handleFeignExceptionTest() throws Exception {
        mockMvc.perform(get("/test/feign-error"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("외부 API 호출 중 오류")));
    }

    @Test
    @DisplayName("정의되지 않은 일반 Exception 발생 시 500 응답을 반환한다")
    void handleGeneralExceptionTest() throws Exception {
        mockMvc.perform(get("/test/unhandled"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/not-found")
        public void throwNotFound() { throw new NotFoundException("Not found error"); }

        @GetMapping("/test/duplicate")
        public void throwDuplicate() { throw new DuplicateException("Duplicate error"); }

        @GetMapping("/test/amount-mismatch")
        public void throwAmountMismatch() { throw new AmountMismatchException("Amount mismatch"); }

        @GetMapping("/test/feign-error")
        public void throwFeign() {
            throw new FeignException.Unauthorized("Unauthorized", 
                Request.create(Request.HttpMethod.GET, "/url", Map.of(), null, null, null), 
                null, null);
        }

        @GetMapping("/test/unhandled")
        public void throwUnhandled() { throw new RuntimeException("Unexpected"); }
    }
}