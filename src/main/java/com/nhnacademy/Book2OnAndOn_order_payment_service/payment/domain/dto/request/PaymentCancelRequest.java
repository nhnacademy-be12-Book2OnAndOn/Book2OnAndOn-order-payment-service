package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request;

public record PaymentCancelRequest(Long paymentId,
                                   String paymentTransactionKey) {
}
