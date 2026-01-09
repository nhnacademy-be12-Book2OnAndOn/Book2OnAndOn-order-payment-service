package com.nhnacademy.book2onandon_order_payment_service.payment.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "tosspayments")
public class TossPaymentsProperties {
    private String secretKey;
}
