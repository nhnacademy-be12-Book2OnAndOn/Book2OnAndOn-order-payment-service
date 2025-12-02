package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api;

import java.util.List;

public record TossCancelResponse (String paymentKey,
                                  String status,
                                  List<Cancel> cancels){
}
