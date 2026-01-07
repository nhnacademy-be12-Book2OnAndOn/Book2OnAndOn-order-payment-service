package com.nhnacademy.book2onandon_order_payment_service.order.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.service.impl.OrderApiServiceImpl;
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

    @Test
    @DisplayName("도서 구매 여부 확인 성공")
    void existsPurchase_Success() {
        Long userId = 1L;
        Long bookId = 100L;
        given(orderRepository.existsPurchase(eq(userId), eq(bookId), eq(OrderItemStatus.DELIVERED)))
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
}