package com.nhnacademy.book2onandon_order_payment_service.order.dto.delivery;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryWaybillUpdateDto {
    @NotNull(message = "택배사를 입력해주세요.")
    private String deliveryCompany;

    @NotNull(message = "운송장 번호를 입력해주세요.")
    private String waybill;
}
