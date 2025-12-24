package com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.NotFoundException;

public class DeliveryPolicyNotFoundException extends NotFoundException {
    public DeliveryPolicyNotFoundException() {
        super("배송정책 정보를 찾을 수 없습니다.");
    }
    public DeliveryPolicyNotFoundException(Long deliveryPolicyId) {
        super("배송정보를 찾을 수 없습니다. (deliveryPolicyId: " + deliveryPolicyId + ")");
    }
    public DeliveryPolicyNotFoundException(String message) {
        super(message);
    }
}