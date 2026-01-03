package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderTransactionService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderTransactionServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderViewAssembler orderViewAssembler;

    @InjectMocks private OrderTransactionService orderTransactionService;

    @Test
    @DisplayName("유저 ID와 주문 번호로 주문 존재 여부를 검증한다")
    void validateOrderExistence_Success() {
        Order order = mock(Order.class);
        given(orderRepository.findByUserIdAndOrderNumber(1L, "ORD-1")).willReturn(Optional.of(order));

        Order result = orderTransactionService.validateOrderExistence(1L, "ORD-1");

        assertThat(result).isEqualTo(order);
    }

    @Test
    @DisplayName("주문이 존재하지 않으면 OrderNotFoundException이 발생한다")
    void validateOrderExistence_NotFound() {
        given(orderRepository.findByUserIdAndOrderNumber(1L, "ORD-1")).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderTransactionService.validateOrderExistence(1L, "ORD-1"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("결제 요청 금액과 주문 저장 금액이 일치하면 주문 객체를 반환한다")
    void validateOrderAmount_Success() {
        CommonConfirmRequest req = new CommonConfirmRequest("ORD-1", "payment-key-123",10000);
        Order order = mock(Order.class);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(order));
        given(order.getTotalAmount()).willReturn(10000);

        Order result = orderTransactionService.validateOrderAmount(req);

        assertThat(result).isEqualTo(order);
    }

    @Test
    @DisplayName("결제 금액이 일치하지 않으면 OrderVerificationException이 발생한다")
    void validateOrderAmount_Mismatch() {
        CommonConfirmRequest req = new CommonConfirmRequest("ORD-1", "payment-key-123",10000);
        Order order = mock(Order.class);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(order));
        given(order.getTotalAmount()).willReturn(9000);

        assertThatThrownBy(() -> orderTransactionService.validateOrderAmount(req))
                .isInstanceOf(OrderVerificationException.class);
    }

    @Test
    @DisplayName("임시 주문 데이터를 생성하고 저장한다")
    void createPendingOrder_Success() {
        OrderVerificationResult result = mock(OrderVerificationResult.class);
        given(result.orderNumber()).willReturn("ORD-1");
        given(result.orderItemList()).willReturn(List.of(mock(OrderItem.class)));
        given(result.deliveryAddress()).willReturn(mock(DeliveryAddress.class));
        
        Order savedOrder = mock(Order.class);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
        given(orderViewAssembler.toOrderCreateView(savedOrder)).willReturn(mock(OrderCreateResponseDto.class));

        OrderCreateResponseDto response = orderTransactionService.createPendingOrder(1L, result);

        assertThat(response).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("결제 성공(flag=true) 시 주문 및 상품 상태를 완료로 변경한다")
    void changeStatusOrder_Completed() {
        Order order = mock(Order.class);
        OrderItem item = mock(OrderItem.class);
        given(order.getOrderItems()).willReturn(List.of(item));

        orderTransactionService.changeStatusOrder(order, true);

        verify(order).updateStatus(OrderStatus.COMPLETED);
        verify(item).updateStatus(OrderItemStatus.ORDER_COMPLETE);
    }

    @Test
    @DisplayName("결제 취소(flag=false) 시 주문 및 상품 상태를 취소로 변경한다")
    void changeStatusOrder_Canceled() {
        Order order = mock(Order.class);
        OrderItem item = mock(OrderItem.class);
        given(order.getOrderItems()).willReturn(List.of(item));

        orderTransactionService.changeStatusOrder(order, false);

        verify(order).updateStatus(OrderStatus.CANCELED);
        verify(item).updateStatus(OrderItemStatus.ORDER_CANCELED);
    }

    @Test
    @DisplayName("주문 번호로 엔티티를 조회한다")
    void getOrderEntity_Success() {
        Order order = mock(Order.class);
        given(orderRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(order));

        Order result = orderTransactionService.getOrderEntity("ORD-1");

        assertThat(result).isEqualTo(order);
    }
}