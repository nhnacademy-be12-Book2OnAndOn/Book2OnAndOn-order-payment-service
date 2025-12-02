package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.Cancel;
import java.util.List;

public record CommonCancelResponse(String paymentKey,
                                   String status,
                                   List<Cancel> cancels) {
}
