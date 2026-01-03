package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.assembler.OrderViewAssembler;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderVerificationResult;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.provider.GuestTokenProvider;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderItemRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderTransactionService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class OrderTransactionServiceTest {

    @InjectMocks
    private OrderTransactionService orderTransactionService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderViewAssembler orderViewAssembler;

    @Mock
    private GuestTokenProvider guestTokenProvider;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Test
    @DisplayName("회원: 본인의 주문 접근 검증 성공")
    void validateOrderExistence_Member_Success() {
        // given
        Long userId = 1L;
        Order order = mock(Order.class);
        given(order.getUserId()).willReturn(userId);

        assertThatCode(() -> orderTransactionService.validateOrderExistence(order, userId, null))
                .doesNotThrowAnyException();

        verify(guestTokenProvider, never()).validateTokenAndGetOrderId(any());
    }

    @Test
    @DisplayName("회원: 타인의 주문 접근 시 예외 발생")
    void validateOrderExistence_Member_Fail_Mismatch() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Order order = mock(Order.class);
        given(order.getUserId()).willReturn(otherUserId);

        // when & then
        assertThatThrownBy(() -> orderTransactionService.validateOrderExistence(order, userId, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("본인의 주문 내역만 조회할 수 있습니다.");
    }

    @Test
    @DisplayName("비회원: 유효한 토큰으로 주문 접근 검증 성공")
    void validateOrderExistence_Guest_Success() {
        // given
        String guestToken = "valid-token";
        Long orderId = 100L;

        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(orderId);

        // 토큰 검증 성공 시 해당 주문 ID 반환
        given(guestTokenProvider.validateTokenAndGetOrderId(guestToken)).willReturn(orderId);

        // when & then
        assertThatCode(() -> orderTransactionService.validateOrderExistence(order, null, guestToken))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비회원: 토큰의 주문 ID와 실제 주문 ID 불일치 시 예외 발생")
    void validateOrderExistence_Guest_Fail_Mismatch() {
        // given
        String guestToken = "valid-token";
        Long orderId = 100L;
        Long tokenOrderId = 999L;

        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(orderId);

        given(guestTokenProvider.validateTokenAndGetOrderId(guestToken)).willReturn(tokenOrderId);

        // when & then
        assertThatThrownBy(() -> orderTransactionService.validateOrderExistence(order, null, guestToken))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("접근 권한이 없는 주문입니다");
    }

    @Test
    @DisplayName("권한 없음: 유저 ID와 게스트 토큰 모두 없는 경우")
    void validateOrderExistence_Fail_NoAuth() {
        // given
        Order order = mock(Order.class);

        // when & then
        assertThatThrownBy(() -> orderTransactionService.validateOrderExistence(order, null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("로그인이 필요하거나");
    }

    @Test
    @DisplayName("주문 금액 검증 성공")
    void validateOrderAmount_Success() {
        // given
        String orderId = "ORD-001";
        Integer amount = 10000;
        CommonConfirmRequest req = new CommonConfirmRequest(orderId,  "key", amount);

        Order order = mock(Order.class);
        given(order.getTotalAmount()).willReturn(10000); // int vs Long 주의 (여기선 int 가정)

        given(orderRepository.findByOrderNumber(orderId)).willReturn(Optional.of(order));

        // when
        Order result = orderTransactionService.validateOrderAmount(req);

        // then
        assertThat(result).isEqualTo(order);
    }

    @Test
    @DisplayName("주문 금액 검증 실패 - 주문 없음")
    void validateOrderAmount_Fail_NotFound() {
        // given
        String orderId = "ORD-NONE";
        CommonConfirmRequest req = new CommonConfirmRequest(orderId,  "key" ,10000);

        given(orderRepository.findByOrderNumber(orderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderTransactionService.validateOrderAmount(req))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("주문 금액 검증 실패 - 금액 불일치")
    void validateOrderAmount_Fail_AmountMismatch() {
        // given
        String orderId = "ORD-001";
        Integer requestAmount = 20000;
        CommonConfirmRequest req = new CommonConfirmRequest(orderId, "key",requestAmount);

        Order order = mock(Order.class);
        given(order.getTotalAmount()).willReturn(10000); // DB에는 10000원

        given(orderRepository.findByOrderNumber(orderId)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderTransactionService.validateOrderAmount(req))
                .isInstanceOf(OrderVerificationException.class)
                .hasMessageContaining("금액 불일치");
    }

    @Test
    @DisplayName("임시 주문 생성 성공")
    void createPendingOrder_Success() {
        // given
        Long userId = 1L;
        DeliveryAddress mockAddress = mock(DeliveryAddress.class);

        OrderVerificationResult result = new OrderVerificationResult(
                "ORD-NEW", "Title", 10000, 0, 10000, 0, 0, 0, 0, LocalDate.now(),
                new ArrayList<>(), mockAddress
        );

        Order savedOrder = mock(Order.class);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
        given(orderViewAssembler.toOrderCreateView(savedOrder)).willReturn(new OrderCreateResponseDto());

        // when
        OrderCreateResponseDto response = orderTransactionService.createPendingOrder(userId, result);

        // then
        assertThat(response).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 상태 변경 - 결제 성공 (COMPLETED)")
    void changeStatusOrder_Success() {
        // given
        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(1L);

        // order.getOrderItems()가 호출되므로 빈 리스트 반환 설정
        given(order.getOrderItems()).willReturn(new ArrayList<>());

        // [중요] NPE 방지를 위해 OrderItemRepository Mocking 필수
        OrderItem mockItem = mock(OrderItem.class);
        given(orderItemRepository.findByOrder_OrderId(1L)).willReturn(List.of(mockItem));

        // when
        orderTransactionService.changeStatusOrder(order, true);

        // then
        verify(order).updateStatus(OrderStatus.COMPLETED);
        verify(mockItem).updateStatus(OrderItemStatus.ORDER_COMPLETE);
    }

    @Test
    @DisplayName("주문 상태 변경 - 결제 취소 (CANCELED)")
    void changeStatusOrder_Cancel() {
        // given
        Order order = mock(Order.class);
        OrderItem mockItem = mock(OrderItem.class);

        // 결제 취소 로직은 order.getOrderItems()를 순회함
        given(order.getOrderItems()).willReturn(List.of(mockItem));

        // when
        orderTransactionService.changeStatusOrder(order, false);

        // then
        verify(order).updateStatus(OrderStatus.CANCELED);
        verify(mockItem).updateStatus(OrderItemStatus.ORDER_CANCELED);

        // 결제 취소 시에는 Repository 조회가 없으므로 호출되지 않았는지 검증
        verify(orderItemRepository, never()).findByOrder_OrderId(any());
    }

    @Test
    @DisplayName("주문 번호로 엔티티 조회 성공")
    void getOrderEntity_Success() {
        // given
        String orderNumber = "ORD-001";
        Order order = mock(Order.class);
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(order));

        // when
        Order result = orderTransactionService.getOrderEntity(orderNumber);

        // then
        assertThat(result).isEqualTo(order);
    }

    @Test
    @DisplayName("주문 번호로 엔티티 조회 실패")
    void getOrderEntity_Fail_NotFound() {
        // given
        String orderNumber = "ORD-NONE";
        given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderTransactionService.getOrderEntity(orderNumber))
                .isInstanceOf(OrderNotFoundException.class);
    }
}