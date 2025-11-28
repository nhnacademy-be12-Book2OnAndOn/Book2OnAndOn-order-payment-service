package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotSupportedPayments;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PaymentStrategyFactory {
    private final Map<String, PaymentStrategy> strategyMap = new HashMap<>();

    public PaymentStrategyFactory(List<PaymentStrategy> strategyList){
        for (PaymentStrategy paymentStrategy : strategyList) {
            strategyMap.put(paymentStrategy.getProvider(), paymentStrategy);
        }
    }

    public PaymentStrategy getStrategy(String provider){
        PaymentStrategy strategy = strategyMap.get(provider.toUpperCase());

        if(Objects.isNull(strategy)){
            throw new NotSupportedPayments("Not Supported Payments : " + provider);
        }

        return strategy;
    }
}
