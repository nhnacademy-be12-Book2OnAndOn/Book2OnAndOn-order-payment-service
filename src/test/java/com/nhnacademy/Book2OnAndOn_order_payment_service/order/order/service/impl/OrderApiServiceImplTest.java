package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderRollbackDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderTransactionService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.impl.OrderApiServiceImpl;
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
    @DisplayName("도서 구매 여부 확인 - 성공")
    void existsPurchase_Success() {
        Long userId = 1L;
        Long bookId = 100L;
        given(orderRepository.existsPurchase(eq(userId), eq(bookId), any(OrderItemStatus.class)))
                .willReturn(true);

        Boolean result = orderApiService.existsPurchase(userId, bookId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("베스트셀러 조회 - DAILY")
    void getBestSellers_Daily() {
        String period = "DAILY";
        given(orderRepository.findTopBestSellerBookIds(
                any(LocalDateTime.class), any(LocalDateTime.class),
                any(OrderStatus.class), any(OrderItemStatus.class), any(Pageable.class)))
                .willReturn(List.of(1L, 2L));

        List<Long> result = orderApiService.getBestSellers(period);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("베스트셀러 조회 - WEEKLY")
    void getBestSellers_Weekly() {
        String period = "WEEKLY";
        given(orderRepository.findTopBestSellerBookIds(
                any(LocalDateTime.class), any(LocalDateTime.class),
                any(OrderStatus.class), any(OrderItemStatus.class), any(Pageable.class)))
                .willReturn(List.of(1L));

        List<Long> result = orderApiService.getBestSellers(period);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("베스트셀러 조회 - 기타 기간 (빈 리스트 반환)")
    void getBestSellers_Other() {
        String period = "MONTHLY";
        List<Long> result = orderApiService.getBestSellers(period);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특정 기간 유저 주문 총액 조회 - 성공")
    void calculateTotalOrderAmount_Success() {
        Long userId = 1L;
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        given(orderRepository.sumTotalItemAmountByUserIdAndOrderDateTimeBetween(eq(userId), any(), any()))
                .willReturn(Optional.of(50000L));

        Long result = orderApiService.calculateTotalOrderAmountForUserBetweenDates(userId, from, to);

        assertThat(result).isEqualTo(50000L);
    }

    @Test
    @DisplayName("특정 기간 유저 주문 총액 조회 - 데이터 없음 (0 반환)")
    void calculateTotalOrderAmount_Empty() {
        Long userId = 1L;
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        given(orderRepository.sumTotalItemAmountByUserIdAndOrderDateTimeBetween(eq(userId), any(), any()))
                .willReturn(Optional.empty());

        Long result = orderApiService.calculateTotalOrderAmountForUserBetweenDates(userId, from, to);

        assertThat(result).isZero();
    }

    @Test
    @DisplayName("롤백 - 비회원 (userId == null)")
    void rollback_Guest() {
        OrderRollbackDto req = new OrderRollbackDto("ORD-001");
        // 비회원 로직은 현재 TODO 상태이므로 메서드 호출 시 에러가 안 나는지만 확인
        orderApiService.rollback(null, req);
    }

    @Test
    @DisplayName("롤백 - 회원 (userId != null)")
    void rollback_Member() {
        Long userId = 1L;
        OrderRollbackDto req = new OrderRollbackDto("ORD-001");
        orderApiService.rollback(userId, req);
    }
}