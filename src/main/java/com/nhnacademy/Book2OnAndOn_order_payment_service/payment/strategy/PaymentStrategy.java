package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.ConfirmSuccessResponse;
import java.util.Map;

public interface PaymentStrategy {
    String getProvider();
    ConfirmSuccessResponse confirmAndProcessPayment(Map<String, String> params);
}
