package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponseDto {

    private Long deliveryId;
    private Long orderId;
    private String deliveryCompany;
    private String waybill;
    private String trackingUrl;

    public DeliveryResponseDto(Delivery delivery, String sweetTrackerApiKey) {
        this.deliveryId = delivery.getDeliveryId();
        this.orderId = delivery.getOrder().getOrderId();

        // 배송사 정보가 있을 때만 세팅
        if (delivery.getDeliveryCompany() != null) {
            this.deliveryCompany = delivery.getDeliveryCompany().getName();
            this.waybill = delivery.getWaybill();

            if (this.waybill != null && sweetTrackerApiKey != null && !sweetTrackerApiKey.isBlank()) {
                this.trackingUrl = String.format(
                        "http://info.sweettracker.co.kr/tracking/5?t_key=%s&t_code=%s&t_invoice=%s",
                        sweetTrackerApiKey,
                        delivery.getDeliveryCompany().getCode(),
                        this.waybill
                );
            }
        }
    }
}