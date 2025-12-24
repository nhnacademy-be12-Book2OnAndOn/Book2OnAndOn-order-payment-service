package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.MergeIssueItemDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartMergeResultResponseDto {

    private List<CartItemResponseDto> mergedItems;

    private List<MergeIssueItemDto> failedToMergeItems;
    private List<MergeIssueItemDto> exceededMaxQuantityItems;
    private List<MergeIssueItemDto> unavailableItems;

    private boolean mergeSucceeded;
}
