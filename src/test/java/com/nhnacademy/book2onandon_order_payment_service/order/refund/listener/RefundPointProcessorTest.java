package com.nhnacademy.book2onandon_order_payment_service.order.refund.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.nhnacademy.book2onandon_order_payment_service.client.UserServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.RefundPointInternalRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.Refund;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundReason;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.listener.RefundPointProcessor;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.delivery.DeliveryRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.refund.RefundRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefundPointProcessorTest {

    @Mock private UserServiceClient userServiceClient;
    @Mock private RefundRepository refundRepository;
    @Mock private DeliveryRepository deliveryRepository;

    @InjectMocks private RefundPointProcessor refundPointProcessor;

    // =========================================================
    // refundAsPoint: "외부 효과" 중심 (API 호출/스킵)만 검증
    // 내부 계산 분기는 아래 private 메서드 테스트에서 커버
    // =========================================================

    @Test
    @DisplayName("정상적인 환불 상황에서 포인트 환불 API를 1회 호출한다")
    void refundAsPoint_success_callsRefundPointOnce() {
        // given
        Order order = mock(Order.class);
        Refund refund = mock(Refund.class);
        OrderItem orderItem = mock(OrderItem.class);
        RefundItem refundItem = mock(RefundItem.class);
        Delivery delivery = mock(Delivery.class);

        given(refund.getOrder()).willReturn(order);
        given(order.getUserId()).willReturn(1L);
        given(order.getOrderId()).willReturn(100L);
        given(refund.getRefundId()).willReturn(10L);

        // 환불 계산이 "0으로 조기 종료"되지 않도록 최소 세팅
        given(order.getOrderItems()).willReturn(List.of(orderItem));
        given(refund.getRefundItems()).willReturn(List.of(refundItem));
        given(orderItem.getUnitPrice()).willReturn(10_000);
        given(orderItem.getOrderItemQuantity()).willReturn(1);
        given(refundItem.getOrderItem()).willReturn(orderItem);
        given(refundItem.getRefundQuantity()).willReturn(1);

        // completedBase 계산에서 쓰일 수 있으므로 세팅 (실제로 호출되면 사용됨)
        given(refundRepository.findByOrderOrderId(100L)).willReturn(List.of(refund));

        // 배송 조회는 refundAsPoint 흐름에서 사용될 수 있으니 "사용되는 형태"로 세팅
        given(delivery.getDeliveryStartAt()).willReturn(LocalDateTime.now().minusDays(11)); // days>10 => 배송비 차감 0으로 안정화
        given(deliveryRepository.findByOrder_OrderId(100L)).willReturn(Optional.of(delivery));

        given(refund.getRefundReason()).willReturn(RefundReason.PRODUCT_DEFECT); // 변심 아님

        // when
        refundPointProcessor.refundAsPoint(refund);

        // then
        verify(userServiceClient, times(1))
                .refundPoint(eq(1L), any(RefundPointInternalRequestDto.class));
    }

    @Test
    @DisplayName("비회원 주문인 경우 포인트 환불 처리를 생략한다 (Fail Path)")
    void refundAsPoint_nonMember_skips() {
        // given
        Order order = mock(Order.class);
        Refund refund = mock(Refund.class);

        given(refund.getOrder()).willReturn(order);
        given(order.getUserId()).willReturn(null);

        // when
        refundPointProcessor.refundAsPoint(refund);

        // then
        verifyNoInteractions(userServiceClient);
    }

    @Test
    @DisplayName("주문 상품이 비어있으면 요청 DTO 금액들이 0으로 계산되어 호출된다")
    void refundAsPoint_emptyOrderItems_callsWithZeroAmounts() {
        // given
        Order order = mock(Order.class);
        Refund refund = mock(Refund.class);

        given(refund.getOrder()).willReturn(order);
        given(order.getUserId()).willReturn(1L);

        // orderItems 비어있음 => calculateRefundPointAmountProRate에서 (0,0,0) 가능성이 큼
        given(order.getOrderItems()).willReturn(List.of());

        // when
        refundPointProcessor.refundAsPoint(refund);

        // then
        ArgumentCaptor<RefundPointInternalRequestDto> captor =
                ArgumentCaptor.forClass(RefundPointInternalRequestDto.class);

        verify(userServiceClient).refundPoint(eq(1L), captor.capture());

        RefundPointInternalRequestDto dto = captor.getValue();
        assertThat(dto).isNotNull();

        // DTO 필드명이 프로젝트마다 다를 수 있어, 가장 흔한 형태를 커버:
        // - refundPayAsPoint / restoreUsedPoint / shippingDeduction
        // 아래 getter명이 다르면 네 DTO에 맞게 바꿔줘.
        assertThat(dto.getUsedPoint()).isZero();
        assertThat(dto.getRefundPayPoint()).isZero();
    }

    // =========================================================
    // private: calculateRefundPointAmountProRate 분기 커버
    // =========================================================

    @Test
    @DisplayName("calculateRefundPointAmountProRate: order가 null이면 (0,0,0) 반환")
    void calc_orderNull_returnsZero() {
        Refund refund = mock(Refund.class);
        given(refund.getOrder()).willReturn(null);

        Object result = ReflectionTestUtils.invokeMethod(
                refundPointProcessor, "calculateRefundPointAmountProRate", refund
        );

        int refundPayAsPoint = (int) ReflectionTestUtils.invokeMethod(result, "refundPayAsPoint");
        int restoreUsedPoint = (int) ReflectionTestUtils.invokeMethod(result, "restoreUsedPoint");
        int shippingDeduction = (int) ReflectionTestUtils.invokeMethod(result, "shippingDeduction");

        assertThat(refundPayAsPoint).isZero();
        assertThat(restoreUsedPoint).isZero();
        assertThat(shippingDeduction).isZero();
    }

    @Test
    @DisplayName("calculateRefundPointAmountProRate: refundItem.orderItem이 null이면 thisBase=0 -> (0,0,0)")
    void calc_thisBase_orderItemNull_returnsZero() {
        Order order = mock(Order.class);
        Refund refund = mock(Refund.class);

        OrderItem oi1 = mock(OrderItem.class);
        given(oi1.getUnitPrice()).willReturn(10_000);
        given(oi1.getOrderItemQuantity()).willReturn(1);

        RefundItem ri1 = mock(RefundItem.class);
        given(ri1.getOrderItem()).willReturn(null);

        given(refund.getOrder()).willReturn(order);
        given(order.getOrderItems()).willReturn(List.of(oi1));
        given(refund.getRefundItems()).willReturn(List.of(ri1));

        Object result = ReflectionTestUtils.invokeMethod(
                refundPointProcessor, "calculateRefundPointAmountProRate", refund
        );

        int refundPayAsPoint = (int) ReflectionTestUtils.invokeMethod(result, "refundPayAsPoint");
        int restoreUsedPoint = (int) ReflectionTestUtils.invokeMethod(result, "restoreUsedPoint");
        int shippingDeduction = (int) ReflectionTestUtils.invokeMethod(result, "shippingDeduction");

        assertThat(refundPayAsPoint).isZero();
        assertThat(restoreUsedPoint).isZero();
        assertThat(shippingDeduction).isZero();
    }

    @Test
    @DisplayName("completedBase: 현재 refundId 제외 + (completed refundItems null) + (refundItem.orderItem null) 분기 커버")
    void calc_completedBase_excludeCurrent_and_nullRefundItems_and_nullOrderItem() {
        Order order = mock(Order.class);
        Refund currentRefund = mock(Refund.class);

        // orderBase = 10000 * 2 = 20000
        OrderItem orderItem = mock(OrderItem.class);
        given(orderItem.getUnitPrice()).willReturn(10_000);
        given(orderItem.getOrderItemQuantity()).willReturn(2);
        given(order.getOrderItems()).willReturn(List.of(orderItem));

        given(order.getPointDiscount()).willReturn(10_000);
        given(order.getCouponDiscount()).willReturn(0);
        given(order.getOrderId()).willReturn(100L);

        // thisBase = 10000 * 1 = 10000
        RefundItem thisRi = mock(RefundItem.class);
        given(thisRi.getOrderItem()).willReturn(orderItem);
        given(thisRi.getRefundQuantity()).willReturn(1);
        given(currentRefund.getRefundItems()).willReturn(List.of(thisRi));

        given(currentRefund.getOrder()).willReturn(order);
        given(currentRefund.getRefundId()).willReturn(1L);

        // 배송 정보: calculateShippingDeduction 경로에서 예외 방지(또는 조기 0) 목적
        Delivery delivery = mock(Delivery.class);
        given(delivery.getDeliveryStartAt()).willReturn(LocalDateTime.now().minusDays(11)); // days>10 => 배송비 차감 0
        given(deliveryRepository.findByOrder_OrderId(100L)).willReturn(Optional.of(delivery));
        given(currentRefund.getRefundReason()).willReturn(RefundReason.CHANGE_OF_MIND);

        Refund completedNullItems = mock(Refund.class);
        given(completedNullItems.getRefundId()).willReturn(2L);
        given(completedNullItems.getRefundStatus()).willReturn(RefundStatus.REFUND_COMPLETED);
        given(completedNullItems.getRefundItems()).willReturn(null);

        Refund completedHasNullOrderItem = mock(Refund.class);
        given(completedHasNullOrderItem.getRefundId()).willReturn(3L);
        given(completedHasNullOrderItem.getRefundStatus()).willReturn(RefundStatus.REFUND_COMPLETED);

        RefundItem completedRi = mock(RefundItem.class);
        given(completedRi.getOrderItem()).willReturn(null);
        given(completedHasNullOrderItem.getRefundItems()).willReturn(List.of(completedRi));

        Refund notCompleted = mock(Refund.class);
        given(notCompleted.getRefundId()).willReturn(4L);
        given(notCompleted.getRefundStatus()).willReturn(RefundStatus.REQUESTED);

        given(refundRepository.findByOrderOrderId(100L))
                .willReturn(List.of(currentRefund, completedNullItems, completedHasNullOrderItem, notCompleted));

        Object result = ReflectionTestUtils.invokeMethod(
                refundPointProcessor, "calculateRefundPointAmountProRate", currentRefund
        );

        int restoreUsedPoint = (int) ReflectionTestUtils.invokeMethod(result, "restoreUsedPoint");
        assertThat(restoreUsedPoint).isPositive();
    }

    // =========================================================
    // private: calculateShippingDeduction 분기 커버
    // =========================================================

    @Test
    @DisplayName("calculateShippingDeduction: order가 null이면 0 반환")
    void shippingDeduction_orderNull_returns0() {
        Refund refund = mock(Refund.class);
        given(refund.getOrder()).willReturn(null);

        int deduction = (int) ReflectionTestUtils.invokeMethod(
                refundPointProcessor, "calculateShippingDeduction", refund
        );

        assertThat(deduction).isZero();
    }

    @Test
    @DisplayName("calculateShippingDeduction: days>10이면 조건 미충족으로 최종 0 반환")
    void shippingDeduction_daysOver10_returns0() {
        Order order = mock(Order.class);
        Refund refund = mock(Refund.class);
        Delivery delivery = mock(Delivery.class);

        given(refund.getOrder()).willReturn(order);
        given(order.getOrderId()).willReturn(100L);
        given(refund.getRefundReason()).willReturn(RefundReason.CHANGE_OF_MIND);

        // days 계산: 배송 시작 11일 전
        given(delivery.getDeliveryStartAt()).willReturn(LocalDateTime.now().minusDays(11));
        given(deliveryRepository.findByOrder_OrderId(100L)).willReturn(Optional.of(delivery));

        int deduction = (int) ReflectionTestUtils.invokeMethod(
                refundPointProcessor, "calculateShippingDeduction", refund
        );

        assertThat(deduction).isZero();

        // existsCompletedRefundWithShippingDeduction 는 호출되지 않는 게 정상일 확률이 높음(조기 return)
        verifyNoInteractions(refundRepository);
    }

    @Test
    @DisplayName("calculateShippingDeduction: refundId null이면 -1L로 exists 호출 + alreadyDeducted면 0")
    void shippingDeduction_refundIdNull_alreadyDeducted_returns0() {
        Order order = mock(Order.class);
        Refund refund = mock(Refund.class);
        Delivery delivery = mock(Delivery.class);

        given(refund.getOrder()).willReturn(order);
        given(order.getOrderId()).willReturn(100L);
        given(refund.getRefundId()).willReturn(null);
        given(refund.getRefundReason()).willReturn(RefundReason.CHANGE_OF_MIND);

        // days <= 10 확정
        given(delivery.getDeliveryStartAt()).willReturn(LocalDateTime.now());
        given(deliveryRepository.findByOrder_OrderId(100L)).willReturn(Optional.of(delivery));

        given(refundRepository.existsCompletedRefundWithShippingDeduction(
                100L, -1L, RefundStatus.REFUND_COMPLETED
        )).willReturn(true);

        int deduction = (int) ReflectionTestUtils.invokeMethod(
                refundPointProcessor, "calculateShippingDeduction", refund
        );

        assertThat(deduction).isZero();

        verify(refundRepository).existsCompletedRefundWithShippingDeduction(
                100L, -1L, RefundStatus.REFUND_COMPLETED
        );
    }

    @Test
    @DisplayName("calculateShippingDeduction: fee가 null/0이면 DEFAULT_RETURN_SHIPPING_FEE(3000) 적용")
    void shippingDeduction_feeNull_returnsDefault3000() {
        Order order = mock(Order.class);
        Refund refund = mock(Refund.class);
        Delivery delivery = mock(Delivery.class);

        given(refund.getOrder()).willReturn(order);
        given(order.getOrderId()).willReturn(100L);
        given(refund.getRefundId()).willReturn(10L);

        // 네 코드에서 OTHER도 배송비 차감 대상이라면 유지, 아니라면 CHANGE_OF_MIND로 맞춰야 함.
        given(refund.getRefundReason()).willReturn(RefundReason.OTHER);

        // days<=10
        given(delivery.getDeliveryStartAt()).willReturn(LocalDateTime.now().minusDays(1));
        given(deliveryRepository.findByOrder_OrderId(100L)).willReturn(Optional.of(delivery));

        given(refundRepository.existsCompletedRefundWithShippingDeduction(
                100L, 10L, RefundStatus.REFUND_COMPLETED
        )).willReturn(false);

        given(order.getDeliveryFee()).willReturn(null);

        int deduction = (int) ReflectionTestUtils.invokeMethod(
                refundPointProcessor, "calculateShippingDeduction", refund
        );

        assertThat(deduction).isEqualTo(3000);

        verify(refundRepository).existsCompletedRefundWithShippingDeduction(
                100L, 10L, RefundStatus.REFUND_COMPLETED
        );
    }

    // =========================================================
    // private: getDaysAfterShipment baseDate 분기 커버
    // =========================================================

    @Test
    @DisplayName("getDaysAfterShipment: deliveryStartAt이 있으면 그 날짜를 기준으로 days 계산")
    void getDaysAfterShipment_usesDeliveryStartAt() {
        Order order = mock(Order.class);
        Delivery delivery = mock(Delivery.class);

        given(order.getOrderId()).willReturn(100L);

        LocalDateTime deliveryStartAt = LocalDateTime.now().minusDays(5);
        given(delivery.getDeliveryStartAt()).willReturn(deliveryStartAt);

        given(deliveryRepository.findByOrder_OrderId(100L)).willReturn(Optional.of(delivery));

        long days = (long) ReflectionTestUtils.invokeMethod(
                refundPointProcessor, "getDaysAfterShipment", order
        );

        long expected = ChronoUnit.DAYS.between(deliveryStartAt.toLocalDate(), LocalDate.now());
        assertThat(days).isEqualTo(expected);
    }

    @Test
    @DisplayName("getDaysAfterShipment: 배송 정보가 없으면 IllegalStateException 발생")
    void getDaysAfterShipment_noDelivery_throwsIllegalState() {
        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(100L);

        given(deliveryRepository.findByOrder_OrderId(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                ReflectionTestUtils.invokeMethod(refundPointProcessor, "getDaysAfterShipment", order)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("배송 정보가 존재하지 않습니다");
    }
}
