package com.nhnacademy.book2onandon_order_payment_service.order.dto.order;

import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import java.util.List;

public record OrderCancelResponseDto(String orderNumber,
                                     String orderStatus,
                                     List<PaymentCancelResponse> paymentCancelResponseList) {
}
