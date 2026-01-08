package com.nhnacademy.book2onandon_order_payment_service.order.refund.domain.entity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.Refund;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundReason;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefundTest {

    @Test
    @DisplayName("create: order가 null이면 IllegalArgumentException")
    void create_orderNull_throws() {
        assertThatThrownBy(() -> Refund.create(null, RefundReason.CHANGE_OF_MIND, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("order는 필수");
    }

    @Test
    @DisplayName("create: reason이 null이면 IllegalArgumentException")
    void create_reasonNull_throws() {
        Order order = mock(Order.class); // 필드가 비어도 create 검증에는 충분
        assertThatThrownBy(() -> Refund.create(order, null, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refundReason은 필수");
    }

    @Test
    @DisplayName("create: 기본값 세팅 - REQUESTED, createdAt, shippingDeductionAmount 기본값 유지")
    void create_setsDefaults() {
        Order order = mock(Order.class);

        Refund refund = Refund.create(order, RefundReason.OTHER, "detail");

        assertThat(refund.getOrder()).isSameAs(order);
        assertThat(refund.getRefundReason()).isEqualTo(RefundReason.OTHER);
        assertThat(refund.getRefundReasonDetail()).isEqualTo("detail");

        assertThat(refund.getRefundStatus()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(refund.getRefundCreatedAt()).isNotNull();

        // 엔티티 필드 기본값
        assertThat(refund.getShippingDeductionAmount()).isNotNull();
        assertThat(refund.getShippingDeductionAmount()).isZero();
        assertThat(refund.getRefundItems()).isNotNull();
    }

    @Test
    @DisplayName("addRefundItem: null 입력이면 아무것도 하지 않는다")
    void addRefundItem_null_noop() {
        Refund refund = Refund.create(mock(Order.class), RefundReason.OTHER, null);
        int before = refund.getRefundItems().size();

        refund.addRefundItem(null);

        assertThat(refund.getRefundItems()).hasSize(before);
    }

    @Test
    @DisplayName("addRefundItem: refundItems에 추가되고, refundItem.setRefund(refund)가 호출된다")
    void addRefundItem_setsBothSides_byInteraction() {
        Refund refund = Refund.create(mock(Order.class), RefundReason.OTHER, null);
        RefundItem item = mock(RefundItem.class);

        refund.addRefundItem(item);

        assertThat(refund.getRefundItems()).contains(item);
        verify(item).setRefund(refund); // 핵심: 상호작용 검증으로 바꿈
    }

    @Test
    @DisplayName("addRefundItem: refundItems가 null이면 내부에서 새 ArrayList로 초기화한 뒤 추가한다 (null-guard branch)")
    void addRefundItem_whenListNull_initializesAndAdds() {
        Refund refund = Refund.create(mock(Order.class), RefundReason.OTHER, null);

        // 핵심: branch 강제 진입
        refund.setRefundItems(null);

        RefundItem item = mock(RefundItem.class);

        refund.addRefundItem(item);

        assertThat(refund.getRefundItems()).isNotNull();
        assertThat(refund.getRefundItems()).hasSize(1);
        assertThat(refund.getRefundItems()).contains(item);

        verify(item).setRefund(refund);
    }

    @Test
    @DisplayName("getOriginalOrderStatusEnum: originalOrderStatus가 null이면 null 반환")
    void getOriginalOrderStatusEnum_null_returnsNull() {
        Refund refund = Refund.create(mock(Order.class), RefundReason.OTHER, null);
        refund.setOriginalOrderStatus(null);

        assertThat(refund.getOriginalOrderStatusEnum()).isNull();
    }

    @Test
    @DisplayName("getOriginalOrderStatusEnum: code를 OrderStatus로 변환한다")
    void getOriginalOrderStatusEnum_converts() {
        Refund refund = Refund.create(mock(Order.class), RefundReason.OTHER, null);

        refund.setOriginalOrderStatus(1);

        OrderStatus status = refund.getOriginalOrderStatusEnum();
        assertThat(status).isNotNull();
    }
}
