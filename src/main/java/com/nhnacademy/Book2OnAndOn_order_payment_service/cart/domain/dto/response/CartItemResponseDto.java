package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartItemUnavailableReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDto {

    private Long bookId;
    private String title;
    private String thumbnailUrl;

    private int originalPrice;  // 정가
    private int salePrice;          // 판매가
    private int quantity;

    private boolean selected;

    private boolean available;
    private CartItemUnavailableReason unavailableReason;

    private int stockCount;     // 남은 재고
    private boolean lowStock;   // 재고 적음 여부 (예: 10개 미만)
}
