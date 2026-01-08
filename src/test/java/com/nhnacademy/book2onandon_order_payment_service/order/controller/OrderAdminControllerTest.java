package com.nhnacademy.book2onandon_order_payment_service.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.*;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.orderitem.OrderItemStatusUpdateDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class OrderAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderAdminController orderAdminController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl = "/admin/orders";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderAdminController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("전체 주문 목록 조회 성공 (Happy Path)")
    void findAllOrderList_Success() throws Exception {
        OrderSimpleDto simpleDto = new OrderSimpleDto(1L, "ORD-001", OrderStatus.COMPLETED, null, 10000, "제목");
        PageImpl<OrderSimpleDto> page = new PageImpl<>(List.of(simpleDto), PageRequest.of(0, 10), 1);

        given(orderService.getOrderListWithAdmin(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get(baseUrl)
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("주문 상세 조회 성공 (Happy Path)")
    void findAdminOrderDetails_Success() throws Exception {
        OrderDetailResponseDto response = new OrderDetailResponseDto();
        given(orderService.getOrderDetailWithAdmin("ORD-001")).willReturn(response);

        mockMvc.perform(get(baseUrl + "/ORD-001"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("주문 상태 변경 성공 (Happy Path)")
    void updateOrderStatusByAdmin_Success() throws Exception {
        OrderStatusUpdateDto req = new OrderStatusUpdateDto(OrderStatus.SHIPPING);

        doNothing().when(orderService).setOrderStatus(eq("ORD-001"), any(OrderStatusUpdateDto.class));

        mockMvc.perform(patch(baseUrl + "/ORD-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("주문 항목 상태 변경 성공 (Happy Path)")
    void updateOrderItemStatusByAdmin_Success() throws Exception {
        OrderItemStatusUpdateDto req = new OrderItemStatusUpdateDto(100L, OrderItemStatus.ORDER_COMPLETE);

        doNothing().when(orderService).setOrderItemStatus(eq("ORD-001"), any(OrderItemStatusUpdateDto.class));

        mockMvc.perform(patch(baseUrl + "/ORD-001/order-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자 주문 취소 성공 (Happy Path)")
    void cancelOrderByAdmin_Success() throws Exception {
        doNothing().when(orderService).cancelOrderByAdmin("ORD-001");

        mockMvc.perform(patch(baseUrl + "/ORD-001/cancel"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지 않는 주문 번호로 상세 조회 시 실패 (Fail Path)")
    void findAdminOrderDetails_NotFound() {
        // given
        given(orderService.getOrderDetailWithAdmin("NOT-FOUND"))
                .willThrow(new RuntimeException("Order Not Found"));

        // [수정] MockMvc가 던지는 ServletException 내부의 원인 예외를 검증합니다.
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        mockMvc.perform(get(baseUrl + "/NOT-FOUND")))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasStackTraceContaining("Order Not Found");
    }

    @Test
    @DisplayName("상태 변경 시 서비스 레이어 예외 발생 (Fail Path)")
    void updateOrderStatusByAdmin_Fail_ServiceException() throws Exception {
        // given
        OrderStatusUpdateDto req = new OrderStatusUpdateDto(OrderStatus.CANCELED);
        String jsonRequest = objectMapper.writeValueAsString(req);

        doThrow(new RuntimeException("Update Error"))
                .when(orderService).setOrderStatus(eq("ORD-001"), any(OrderStatusUpdateDto.class));

        // [수정] MockMvc가 던지는 예외를 직접 검증합니다.
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        mockMvc.perform(patch(baseUrl + "/ORD-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasStackTraceContaining("Update Error");
    }
}