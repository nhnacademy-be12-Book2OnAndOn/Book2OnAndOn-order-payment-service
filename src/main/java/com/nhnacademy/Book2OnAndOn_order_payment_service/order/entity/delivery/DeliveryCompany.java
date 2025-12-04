package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum DeliveryCompany {
    
    // 주요 택배사 코드 (스마트택배 기준)
    CJ_LOGISTICS("CJ대한통운", "04"),
    POST_OFFICE("우체국택배", "01"),
    HANJIN("한진택배", "05"),
    LOTTE("롯데택배", "08"),
    LOGEN("로젠택배", "06"),
    CU_POST("CU 편의점택배", "46"),
    GS_POST("GS25 편의점택배", "24");

    private final String name; // 화면 표시용 (한글)
    private final String code; // API 호출용 (숫자 문자열)

    // 이름으로 Enum 찾기 (역조회용)
    public static DeliveryCompany findByName(String name) {
        return Arrays.stream(values())
                .filter(c -> c.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 택배사입니다: " + name));
    }
}