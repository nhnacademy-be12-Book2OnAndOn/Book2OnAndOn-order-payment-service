package com.nhnacademy.book2onandon_order_payment_service.order.listener;

import com.nhnacademy.book2onandon_order_payment_service.client.UserServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.RefundPointInternalRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.Refund;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundReason;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.delivery.DeliveryRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.refund.RefundRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundPointProcessor {

    private static final int DEFAULT_RETURN_SHIPPING_FEE = 3000;

    private final UserServiceClient userServiceClient;
    private final RefundRepository refundRepository;
    private final DeliveryRepository deliveryRepository;

    public void refundAsPoint(Refund refund) {
        Order order = refund.getOrder();
        if (order == null) return;
        if (order.getUserId() == null) return;

        RefundCalcResult r = calculateRefundPointAmountProRate(refund);

        userServiceClient.refundPoint(
                order.getUserId(),
                new RefundPointInternalRequestDto(
                        order.getOrderId(),
                        refund.getRefundId(), // 멱등키로 쓰기 좋음
                        r.restoreUsedPoint(),
                        r.refundPayAsPoint()
                )
        );
    }

    // =========================
    // 내부 계산 로직(RefundServiceImpl에서 이관)
    // =========================
    private record RefundCalcResult(
            int refundPayAsPoint,
            int restoreUsedPoint,
            int shippingDeduction
    ) {}

    // 쿠폰은 한 번 사용하면 반품해도 복구 안해줌(정책 유지)
    private RefundCalcResult calculateRefundPointAmountProRate(Refund refund) {
        Order order = refund.getOrder();
        if (order == null) return new RefundCalcResult(0, 0, 0);

        int usedPoint = nvl(order.getPointDiscount());
        int couponDiscount = nvl(order.getCouponDiscount());

        int orderBase = order.getOrderItems().stream()
                .filter(Objects::nonNull)
                .mapToInt(oi -> nvl(oi.getUnitPrice()) * nvl(oi.getOrderItemQuantity()))
                .sum();
        if (orderBase <= 0) return new RefundCalcResult(0, 0, 0);

        int thisBase = refund.getRefundItems().stream()
                .filter(Objects::nonNull)
                .mapToInt(ri -> {
                    OrderItem oi = ri.getOrderItem();
                    if (oi == null) return 0;
                    return nvl(oi.getUnitPrice()) * nvl(ri.getRefundQuantity());
                })
                .sum();
        if (thisBase <= 0) return new RefundCalcResult(0, 0, 0);

        int completedBase = refundRepository.findByOrderOrderId(order.getOrderId()).stream()
                .filter(Objects::nonNull)
                .filter(r -> !Objects.equals(r.getRefundId(), refund.getRefundId()))
                .filter(r -> r.getRefundStatus() == RefundStatus.REFUND_COMPLETED)
                .flatMap(r -> r.getRefundItems() == null ? List.<RefundItem>of().stream() : r.getRefundItems().stream())
                .filter(Objects::nonNull)
                .mapToInt(ri -> {
                    OrderItem oi = ri.getOrderItem();
                    if (oi == null) return 0;
                    return nvl(oi.getUnitPrice()) * nvl(ri.getRefundQuantity());
                })
                .sum();

        int usedPointAllocatedBefore = (int) Math.floor((double) usedPoint * completedBase / orderBase);
        int usedPointAllocatedTarget = (int) Math.floor((double) usedPoint * (completedBase + thisBase) / orderBase);
        int restoreUsedPoint = Math.max(usedPointAllocatedTarget - usedPointAllocatedBefore, 0);

        int couponAllocatedBefore = (int) Math.floor((double) couponDiscount * completedBase / orderBase);
        int couponAllocatedTarget = (int) Math.floor((double) couponDiscount * (completedBase + thisBase) / orderBase);
        int couponAllocated = Math.max(couponAllocatedTarget - couponAllocatedBefore, 0);

        int shippingDeduction = calculateShippingDeduction(refund);

        int refundPayAsPoint = Math.max(thisBase - couponAllocated - shippingDeduction, 0);

        return new RefundCalcResult(refundPayAsPoint, restoreUsedPoint, shippingDeduction);
    }

    private int calculateShippingDeduction(Refund refund) {
        Order order = refund.getOrder();
        if (order == null) return 0;

        long days = getDaysAfterShipment(order);
        RefundReason reason = refund.getRefundReason();

        // 파손/오배송은 차감 없음
        if (reason == RefundReason.PRODUCT_DEFECT || reason == RefundReason.WRONG_DELIVERY) {
            return 0;
        }

        // 단순변심/기타 + 10일 이내면 차감 대상
        if (days <= 10 && (reason == RefundReason.CHANGE_OF_MIND || reason == RefundReason.OTHER)) {

            // 주문당 1회 차감: 이미 완료된 반품 중 차감 적용이 있으면 이번엔 0
            boolean alreadyDeducted = refundRepository.existsCompletedRefundWithShippingDeduction(
                    order.getOrderId(),
                    refund.getRefundId() == null ? -1L : refund.getRefundId(),
                    RefundStatus.REFUND_COMPLETED
            );
            if (alreadyDeducted) return 0;

            Integer fee = order.getDeliveryFee();
            return (fee != null && fee > 0) ? fee : DEFAULT_RETURN_SHIPPING_FEE;
        }

        return 0;
    }

    private long getDaysAfterShipment(Order order) {
        Delivery delivery = deliveryRepository.findByOrder_OrderId(order.getOrderId())
                .orElseThrow(() -> new IllegalStateException("배송 정보가 존재하지 않습니다."));

        LocalDate baseDate = (delivery.getDeliveryStartAt() != null)
                ? delivery.getDeliveryStartAt().toLocalDate()
                : order.getOrderDateTime().toLocalDate();

        return ChronoUnit.DAYS.between(baseDate, LocalDate.now());
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
