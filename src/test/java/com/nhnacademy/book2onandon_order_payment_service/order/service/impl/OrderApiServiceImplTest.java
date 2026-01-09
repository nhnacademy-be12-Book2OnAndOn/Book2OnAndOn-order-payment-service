package com.nhnacademy.book2onandon_order_payment_service.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderTransactionService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OrderApiServiceImplTest {

    @InjectMocks
    private OrderApiServiceImpl orderApiService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderTransactionService orderTransactionService;

    @Mock
    private OrderResourceManager orderResourceManager;

    @Test
    @DisplayName("도서 구매 여부 확인 성공")
    void existsPurchase_Success() {
        Long userId = 1L;
        Long bookId = 100L;
        given(orderRepository.existsPurchase((userId), (bookId), (OrderItemStatus.DELIVERED)))
                .willReturn(true);

        Boolean result = orderApiService.existsPurchase(userId, bookId);

        assertThat(result).isTrue();
        verify(orderRepository).existsPurchase(userId, bookId, OrderItemStatus.DELIVERED);
    }

    @Test
    @DisplayName("일간 베스트셀러 조회 성공")
    void getBestSellers_Daily_Success() {
        String period = "DAILY";
        List<Long> mockIds = List.of(1L, 2L);
        given(orderRepository.findTopBestSellerBookIds(any(LocalDateTime.class), any(LocalDateTime.class),
                eq(OrderStatus.DELIVERED), eq(OrderItemStatus.DELIVERED), any(Pageable.class)))
                .willReturn(mockIds);

        List<Long> result = orderApiService.getBestSellers(period);

        assertThat(result).hasSize(2).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("주간 베스트셀러 조회 성공")
    void getBestSellers_Weekly_Success() {
        String period = "WEEKLY";
        List<Long> mockIds = List.of(3L, 4L);
        given(orderRepository.findTopBestSellerBookIds(any(LocalDateTime.class), any(LocalDateTime.class),
                eq(OrderStatus.DELIVERED), eq(OrderItemStatus.DELIVERED), any(Pageable.class)))
                .willReturn(mockIds);

        List<Long> result = orderApiService.getBestSellers(period);

        assertThat(result).hasSize(2).containsExactly(3L, 4L);
    }

    @Test
    @DisplayName("지원하지 않는 기간 베스트셀러 조회 시 빈 리스트 반환")
    void getBestSellers_Fail_InvalidPeriod() {
        String period = "MONTHLY";

        List<Long> result = orderApiService.getBestSellers(period);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("유저의 특정 기간 총 주문액 계산 성공")
    void calculateTotalOrderAmount_Success() {
        Long userId = 1L;
        LocalDate from = LocalDate.now().minusMonths(1);
        LocalDate to = LocalDate.now();
        given(orderRepository.sumTotalItemAmountByUserIdAndOrderDateTimeBetween(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(Optional.of(50000L));

        Long result = orderApiService.calculateTotalOrderAmountForUserBetweenDates(userId, from, to);

        assertThat(result).isEqualTo(50000L);
    }

    @Test
    @DisplayName("주문 내역이 없는 경우 0원 반환")
    void calculateTotalOrderAmount_ReturnZero_WhenEmpty() {
        Long userId = 1L;
        LocalDate from = LocalDate.now().minusMonths(1);
        LocalDate to = LocalDate.now();
        given(orderRepository.sumTotalItemAmountByUserIdAndOrderDateTimeBetween(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(Optional.empty());

        Long result = orderApiService.calculateTotalOrderAmountForUserBetweenDates(userId, from, to);

        assertThat(result).isZero();
    }

    @Test
    @DisplayName("주문 롤백 - 정상 PENDING 주문 처리")
    void rollback_ShouldReleaseResourcesAndChangeStatus_WhenOrderIsPending() {
        // given
        String orderNumber = "B2-000000000001";
        Order order = Order.builder()
                .orderId(1L)
                .orderNumber(orderNumber)
                .userId(1001L)
                .orderStatus(OrderStatus.PENDING)
                .pointDiscount(500)
                .build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        // when
        orderApiService.rollback(orderNumber);

        // then
        verify(orderResourceManager, times(1))
                .releaseResources(orderNumber, order.getUserId(), order.getPointDiscount(), order.getOrderId());
        verify(orderTransactionService, times(1)).changeStatusOrder(order, false);
    }

    @Test
    @DisplayName("주문 롤백 - PENDING이 아닌 주문은 resource release 호출 안 함")
    void rollback_ShouldNotReleaseResources_WhenOrderIsNotPending() {
        // given
        String orderNumber = "B2-000000000002";
        Order order = Order.builder()
                .orderId(2L)
                .orderNumber(orderNumber)
                .userId(1002L)
                .orderStatus(OrderStatus.COMPLETED)
                .pointDiscount(300)
                .build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        // when
        orderApiService.rollback(orderNumber);

        // then
        verify(orderResourceManager, never()).releaseResources(anyString(), anyLong(), anyInt(), anyLong());
        verify(orderTransactionService, times(1)).changeStatusOrder(order, false);
    }

    @Test
    @DisplayName("주문 롤백 - 주문 번호 없으면 OrderNotFoundException 발생")
    void rollback_ShouldThrowOrderNotFoundException_WhenOrderNotExist() {
        // given
        String orderNumber = "B2-000000000003";
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());

        // when & then
        OrderNotFoundException ex = assertThrows(OrderNotFoundException.class, () ->
                orderApiService.rollback(orderNumber)
        );
        assertTrue(ex.getMessage().contains(orderNumber));
    }

}