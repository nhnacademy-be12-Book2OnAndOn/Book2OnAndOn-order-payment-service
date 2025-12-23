package com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class SmartTrackingResponse {

    @JsonProperty("result")       // 결과 코드 (Y/N)
    private String result;

    @JsonProperty("msg")          // 에러 메시지
    private String msg;

    @JsonProperty("complete")     // 배송 완료 여부 (true/false)
    private Boolean complete;

    @JsonProperty("level")        // 진행 단계 (1~6, 6이 완료)
    private Integer level;

    @JsonProperty("invoiceNo")    // 운송장 번호
    private String invoiceNo;
}