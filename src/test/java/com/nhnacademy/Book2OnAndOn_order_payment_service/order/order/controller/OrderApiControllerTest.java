package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller.OrderApiController;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderApiService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:"
})
class OrderApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderApiService orderApiService;

    private static final String USER_ID_HEADER = "X-User-Id";

    @Test
    @DisplayName("유저의 도서 구매 여부 확인 성공 (Happy Path)")
    void hasPurchasedBook_Success() throws Exception {
        Long userId = 1L;
        Long bookId = 100L;
        given(orderApiService.existsPurchase(userId, bookId)).willReturn(true);

        mockMvc.perform(get("/order/check-purchase/{bookId}", bookId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("도서 구매 여부 확인 실패 - 헤더 누락 (Fail Path)")
    void hasPurchasedBook_Fail_MissingHeader() throws Exception {
        mockMvc.perform(get("/order/check-purchase/{bookId}", 100L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("베스트셀러 목록 조회 성공 (Happy Path)")
    void getBestSellers_Success() throws Exception {
        String period = "month";
        List<Long> mockIds = List.of(1L, 2L, 3L);
        given(orderApiService.getBestSellers(period)).willReturn(mockIds);

        mockMvc.perform(get("/orders/bestsellers")
                        .param("period", period))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(1L))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("베스트셀러 목록 조회 실패 - 파라미터 누락 (Fail Path)")
    void getBestSellers_Fail_MissingParam() throws Exception {
        mockMvc.perform(get("/orders/bestsellers"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("특정 기간 유저 순수 주문액 조회 성공 (Happy Path)")
    void getNetOrderAmount_Success() throws Exception {
        Long userId = 1L;
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        given(orderApiService.calculateTotalOrderAmountForUserBetweenDates(userId, from, to))
                .willReturn(150000L);

        mockMvc.perform(get("/orders/users/{userId}/net-amount", userId)
                        .param("from", "2025-01-01")
                        .param("to", "2025-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().string("150000"));
    }

    @Test
    @DisplayName("순수 주문액 조회 실패 - 날짜 형식 오류 (Fail Path)")
    void getNetOrderAmount_Fail_InvalidDateFormat() throws Exception {
        mockMvc.perform(get("/orders/users/{userId}/net-amount", 1L)
                        .param("from", "invalid-date")
                        .param("to", "2025-12-31"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(containsString("Failed to convert")));
    }
}