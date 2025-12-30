package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.RefundPointInternalRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.Refund;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundReason;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.listener.RefundPointProcessor;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.refund.RefundRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundPointProcessorTest {

    @Mock private UserServiceClient userServiceClient;
    @Mock private RefundRepository refundRepository;
    @Mock private DeliveryRepository deliveryRepository;

    @InjectMocks private RefundPointProcessor refundPointProcessor;

    @Test
    @DisplayName("정상적인 환불 상황에서 포인트 환불 API를 호출한다")
    void refundAsPoint_Success() {
        Order order = mock(Order.class);
        Refund refund = mock(Refund.class);
        OrderItem orderItem = mock(OrderItem.class);
        RefundItem refundItem = mock(RefundItem.class);
        Delivery delivery = mock(Delivery.class);

        given(refund.getOrder()).willReturn(order);
        given(order.getUserId()).willReturn(1L);
        given(order.getOrderId()).willReturn(100L);
        given(refund.getRefundId()).willReturn(1L);
        given(order.getOrderItems()).willReturn(List.of(orderItem));
        given(refund.getRefundItems()).willReturn(List.of(refundItem));
        
        given(orderItem.getUnitPrice()).willReturn(10000);
        given(orderItem.getOrderItemQuantity()).willReturn(1);
        given(refundItem.getOrderItem()).willReturn(orderItem);
        given(refundItem.getRefundQuantity()).willReturn(1);
        
        given(refundRepository.findByOrderOrderId(anyLong())).willReturn(List.of(refund));
        given(deliveryRepository.findByOrder_OrderId(anyLong())).willReturn(Optional.of(delivery));
        given(order.getOrderDateTime()).willReturn(LocalDateTime.now());
        given(refund.getRefundReason()).willReturn(RefundReason.PRODUCT_DEFECT);

        refundPointProcessor.refundAsPoint(refund);

        verify(userServiceClient, times(1)).refundPoint(eq(1L), any(RefundPointInternalRequestDto.class));
    }

    @Test
    @DisplayName("비회원 주문인 경우 포인트 환불 처리를 생략한다 (Fail Path)")
    void refundAsPoint_NonMember_Skip() {
        Order order = mock(Order.class);
        Refund refund = mock(Refund.class);
        given(refund.getOrder()).willReturn(order);
        given(order.getUserId()).willReturn(null);

        refundPointProcessor.refundAsPoint(refund);

        verifyNoInteractions(userServiceClient);
    }

    @Test
    @DisplayName("단순 변심 환불 시 배송비를 차감하여 계산한다")
    void calculateShippingDeduction_ChangeOfMind() {
        Order order = mock(Order.class);
        Refund refund = mock(Refund.class);
        OrderItem orderItem = mock(OrderItem.class);
        RefundItem refundItem = mock(RefundItem.class);
        Delivery delivery = mock(Delivery.class);

        given(refund.getOrder()).willReturn(order);
        given(order.getUserId()).willReturn(1L);
        given(order.getOrderItems()).willReturn(List.of(orderItem));
        given(refund.getRefundItems()).willReturn(List.of(refundItem));
        given(refundItem.getOrderItem()).willReturn(orderItem);
        given(orderItem.getUnitPrice()).willReturn(10000);
        given(refundItem.getRefundQuantity()).willReturn(1);
        given(orderItem.getOrderItemQuantity()).willReturn(1);

        given(refund.getRefundReason()).willReturn(RefundReason.CHANGE_OF_MIND);
        given(order.getDeliveryFee()).willReturn(3000);
        given(deliveryRepository.findByOrder_OrderId(any())).willReturn(Optional.of(delivery));
        given(order.getOrderDateTime()).willReturn(LocalDateTime.now());
        
        given(refundRepository.existsCompletedRefundWithShippingDeduction(any(), any(), any())).willReturn(false);

        refundPointProcessor.refundAsPoint(refund);

        verify(userServiceClient).refundPoint(any(), any(RefundPointInternalRequestDto.class));
    }

    @Test
    @DisplayName("주문 상품이나 환불 상품이 비어있으면 계산 결과는 0원이다")
    void calculate_EmptyItems_ReturnsZero() {
        Order order = mock(Order.class);
        Refund refund = mock(Refund.class);
        given(refund.getOrder()).willReturn(order);
        given(order.getUserId()).willReturn(1L);
        given(order.getOrderItems()).willReturn(List.of());

        refundPointProcessor.refundAsPoint(refund);

        verify(userServiceClient).refundPoint(any(), any(RefundPointInternalRequestDto.class));
    }

    @Test
    @DisplayName("배송 정보가 없으면 IllegalStateException이 발생한다 (Fail Path)")
    void getDaysAfterShipment_NoDelivery_ThrowsException() {
        Order order = mock(Order.class);
        Refund refund = mock(Refund.class);
        OrderItem orderItem = mock(OrderItem.class);
        RefundItem refundItem = mock(RefundItem.class);

        given(refund.getOrder()).willReturn(order);
        given(order.getUserId()).willReturn(1L);
        given(order.getOrderItems()).willReturn(List.of(orderItem));
        given(refund.getRefundItems()).willReturn(List.of(refundItem));
        given(orderItem.getUnitPrice()).willReturn(1000);
        given(orderItem.getOrderItemQuantity()).willReturn(1);
        given(refundItem.getOrderItem()).willReturn(orderItem);
        given(refundItem.getRefundQuantity()).willReturn(1);

        given(deliveryRepository.findByOrder_OrderId(any())).willReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> refundPointProcessor.refundAsPoint(refund))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("배송 정보가 존재하지 않습니다");
    }
}