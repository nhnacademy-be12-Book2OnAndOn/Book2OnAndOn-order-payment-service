package com.nhnacademy.book2onandon_order_payment_service.order.refund.domain.entity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.Refund;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundItemTest {

    @Test
    @DisplayName("create: 정상 생성 시 필드가 올바르게 세팅된다")
    void create_success() {
        Refund refund = mock(Refund.class);
        OrderItem orderItem = mock(OrderItem.class);
        given(orderItem.getOrderItemStatus()).willReturn(OrderItemStatus.ORDER_COMPLETE);

        RefundItem ri = RefundItem.create(refund, orderItem, 2);

        assertThat(ri.getRefund()).isSameAs(refund);
        assertThat(ri.getOrderItem()).isSameAs(orderItem);
        assertThat(ri.getRefundQuantity()).isEqualTo(2);

        // else 분기( fromCode 호출 ) 커버
        assertThat(ri.getOriginalStatus()).isEqualTo(OrderItemStatus.ORDER_COMPLETE);
    }

    @Test
    @DisplayName("create: refund가 null이면 예외")
    void create_refundNull_throw() {
        OrderItem orderItem = mock(OrderItem.class);

        assertThatThrownBy(() -> RefundItem.create(null, orderItem, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refund는 필수");
    }

    @Test
    @DisplayName("create: orderItem이 null이면 예외")
    void create_orderItemNull_throw() {
        Refund refund = mock(Refund.class);

        assertThatThrownBy(() -> RefundItem.create(refund, null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderItem은 필수");
    }

    @Test
    @DisplayName("create: quantity가 0 이하이면 예외")
    void create_quantityInvalid_throw() {
        Refund refund = mock(Refund.class);
        OrderItem orderItem = mock(OrderItem.class);

        assertThatThrownBy(() -> RefundItem.create(refund, orderItem, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity는 1 이상");
    }

    @Test
    @DisplayName("create: orderItemStatus가 null이면 IllegalStateException")
    void create_orderItemStatusNull_throw() {
        Refund refund = mock(Refund.class);
        OrderItem orderItem = mock(OrderItem.class);
        given(orderItem.getOrderItemStatus()).willReturn(null);

        assertThatThrownBy(() -> RefundItem.create(refund, orderItem, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("orderItemStatus가 null");
    }

    @Test
    @DisplayName("getOriginalStatus: originalOrderItemStatus가 null이면 null 반환")
    void getOriginalStatus_null_returnsNull() {
        // mock 쓰면 메서드 본문이 실행되지 않아서 커버가 안 됨 -> 실객체 사용
        RefundItem ri = mock(RefundItem.class);
        ri.setOriginalOrderItemStatus(null);

        assertThat(ri.getOriginalStatus()).isNull();
    }

    @Test
    @DisplayName("getOriginalStatus: originalOrderItemStatus가 있으면 fromCode 결과 반환")
    void getOriginalStatus_nonNull_returnsEnum() {
        Refund refund = mock(Refund.class);
        OrderItem orderItem = mock(OrderItem.class);
        given(orderItem.getOrderItemStatus()).willReturn(OrderItemStatus.ORDER_COMPLETE);

        RefundItem ri = RefundItem.create(refund, orderItem, 1);

        assertThat(ri.getOriginalStatus()).isEqualTo(OrderItemStatus.ORDER_COMPLETE);
    }
}
