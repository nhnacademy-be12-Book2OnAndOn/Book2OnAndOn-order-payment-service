package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import java.util.List;

public record OrderCancelResponseDto(String orderNumber,
                                     String orderStatus,
                                     List<PaymentCancelResponse> paymentCancelResponseList) {
}
