package com.nhnacademy.book2onandon_order_payment_service.order.order.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.Refund;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    @DisplayName("Order 빌더 및 Getter 동작 확인 (Happy Path)")
    void orderBuilderAndGetterTest() {
        String orderNo = "ORD20260108001";
        LocalDate wantDate = LocalDate.now().plusDays(2);

        Order order = Order.builder()
                .userId(1L)
                .orderNumber(orderNo)
                .orderStatus(OrderStatus.COMPLETED)
                .orderTitle("테스트 도서")
                .totalAmount(20000)
                .totalDiscountAmount(2000)
                .totalItemAmount(18000)
                .deliveryFee(3000)
                .wrappingFee(1000)
                .couponDiscount(1000)
                .pointDiscount(1000)
                .wantDeliveryDate(wantDate)
                .build();

        assertThat(order.getUserId()).isEqualTo(1L);
        assertThat(order.getOrderNumber()).isEqualTo(orderNo);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getWantDeliveryDate()).isEqualTo(wantDate);
        assertThat(order.getOrderDateTime()).isNotNull();
    }

    @Test
    @DisplayName("주문 상태 업데이트 확인 (Happy Path)")
    void updateStatus_Success() {
        Order order = Order.builder()
                .orderStatus(OrderStatus.PENDING)
                .build();

        order.updateStatus(OrderStatus.SHIPPING);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SHIPPING);
    }

    @Test
    @DisplayName("주문 항목 추가 및 양방향 연관관계 설정 확인 (Happy Path)")
    void addOrderItem_Success() {
        Order order = Order.builder()
                .orderItems(new ArrayList<>())
                .build();
        OrderItem item1 = OrderItem.builder().bookId(10L).build();
        OrderItem item2 = OrderItem.builder().bookId(11L).build();

        order.addOrderItem(List.of(item1, item2));

        assertThat(order.getOrderItems()).hasSize(2);
        assertThat(item1.getOrder()).isEqualTo(order);
        assertThat(item2.getOrder()).isEqualTo(order);
    }

    @Test
    @DisplayName("배송지 추가 및 양방향 연관관계 설정 확인 (Happy Path)")
    void addDeliveryAddress_Success() {
        Order order = Order.builder().build();
        DeliveryAddress address = DeliveryAddress.builder().build();

        order.addDeliveryAddress(address);

        assertThat(order.getDeliveryAddress()).isEqualTo(address);
        assertThat(address.getOrder()).isEqualTo(order);
    }

    @Test
    @DisplayName("반품 내역 추가 및 양방향 연관관계 설정 확인 (Happy Path)")
    void addRefund_Success() {
        Order order = Order.builder()
                .refunds(new ArrayList<>())
                .build();
        Refund refund = new Refund() {};

        order.addRefund(refund);

        assertThat(order.getRefunds()).contains(refund);
        assertThat(refund.getOrder()).isEqualTo(order);
    }

    @Test
    @DisplayName("null 반품 내역 추가 시 아무 동작 안 함 (Fail Path)")
    void addRefund_Null() {
        Order order = Order.builder()
                .refunds(new ArrayList<>())
                .build();

        order.addRefund(null);

        assertThat(order.getRefunds()).isEmpty();
    }

    @Test
    @DisplayName("refunds 리스트가 null인 상태에서 반품 추가 시 초기화 후 추가 확인 (Edge Case)")
    void addRefund_WhenRefundListIsNull() {
        Order order = Order.builder().build();
        order.setRefunds(null);
        Refund refund = new Refund() {};

        order.addRefund(refund);

        assertThat(order.getRefunds()).isNotNull();
        assertThat(order.getRefunds()).hasSize(1);
    }
}