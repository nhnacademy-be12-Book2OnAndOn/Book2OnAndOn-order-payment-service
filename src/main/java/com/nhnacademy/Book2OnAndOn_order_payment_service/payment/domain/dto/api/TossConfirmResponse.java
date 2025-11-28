package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record TossConfirmResponse(String orderId,
                                  Integer totalAmount,
                                  String method,
                                  String status,
                                  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
                                  LocalDateTime requestedAt,
                                  Receipt receipt,
                                  String paymentKey
) {
    public record Receipt(String url){}
}
