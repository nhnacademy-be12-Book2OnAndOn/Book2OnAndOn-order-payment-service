package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemsResponseDto {

    private List<CartItemResponseDto> items;

    private int totalItemCount;      // 아이템 종류 수
    private int totalQuantity;       // 전체 수량
    private int totalPrice;          // 전체 금액

    private int selectedQuantity;    // 선택된 항목 수량 합
    private int selectedTotalPrice;  // 선택된 항목 총 금액

    // === 배송 정책 관련 추가 ===
//    private int deliveryFee;             // 배송비
//    private int freeDeliveryThreshold;   // 무료배송 기준

    // === 최종 결제금액 (옵션이지만 권장) ===
//    private int finalPaymentAmount;      // selectedTotalPrice + 배송비
}
