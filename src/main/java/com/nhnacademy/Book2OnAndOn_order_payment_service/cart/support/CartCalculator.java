package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.support;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartItemUnavailableReason;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception.CartErrorCode;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception.CartItemNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.BookServiceClient.BookSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartConstants.LOW_STOCK_THRESHOLD;

@Component
@RequiredArgsConstructor
// 장바구니 아이템 하나”의 최종 상태/가격 정보를 만들어주는 도메인 계산 모듈
public class CartCalculator {

    // Map으로 받아서 bookId 기반으로 스냅샷을 조회하고 가격, 가용성, 재고 등의 정보를 계산해서 돌려준다.
    public CartItemPricingResult calculatePricing(
            Long bookId, // 도서id
            int quantity, // 수량
            Map<Long, BookSnapshot> snapshotMap // 도서(book-service) 스냅샷 묶음
    ) {
        BookSnapshot snapshot = snapshotMap.get(bookId);
        if (snapshot == null) {
            throw new CartItemNotFoundException(CartErrorCode.CART_ITEM_NOT_FOUND);
        }

        // 내부변수 초기화
        boolean available = true;
        CartItemUnavailableReason reason = null;
        int salePrice = 0;
        int originalPrice = 0;
        int stock = 0;
        boolean lowStock = false;
        String title = "";
        String thumbnailUrl = "";

        // snapshot null인 경우 (잘못된 bookId)
        if (snapshot == null) {
            available = false;
            reason = CartItemUnavailableReason.INVALID_BOOK;
        } else { // snapshot이 존재하는 경우 — 필드 채우기
            title = snapshot.getTitle();
            thumbnailUrl = snapshot.getThumbnailUrl();
            originalPrice = snapshot.getOriginalPrice();
            salePrice = snapshot.getSalePrice();
            stock = snapshot.getStockCount();
            lowStock = (stock > 0 && stock < LOW_STOCK_THRESHOLD);

            // “이 책을 장바구니에서 주문할 수 있는지” 판단 로직
            if (snapshot.isDeleted()) {
                available = false;
                reason = CartItemUnavailableReason.BOOK_DELETED;
            } else if (snapshot.isSaleEnded()) {
                available = false;
                reason = CartItemUnavailableReason.SALE_ENDED;
//            } else if (snapshot.isHidden()) {
//                available = false;
//                reason = CartItemUnavailableReason.BOOK_HIDDEN;
            } else if (stock <= 0) {
                available = false;
                reason = CartItemUnavailableReason.OUT_OF_STOCK;
            }
        }
        // 최종 금액 계산
        int lineTotalPrice = salePrice * quantity;
        // CartItemPricingResult 객체 생성 및 결과 반환
        return new CartItemPricingResult(
                title,
                thumbnailUrl,
                originalPrice,
                salePrice,
                stock,
                lowStock,
                available,
                reason,
                lineTotalPrice
        );
    }

    @Getter
    public static class CartItemPricingResult {
        private final String title;
        private final String thumbnailUrl;
        private final int originalPrice;
        private final int salePrice;
        private final int stockCount;
        private final boolean lowStock;
        private final boolean available;
        private final CartItemUnavailableReason unavailableReason;
        private final int lineTotalPrice;

        public CartItemPricingResult(
                String title,
                String thumbnailUrl,
                int originalPrice,
                int salePrice,
                int stockCount,
                boolean lowStock,
                boolean available,
                CartItemUnavailableReason unavailableReason,
                int lineTotalPrice
        ) {
            this.title = title;
            this.thumbnailUrl = thumbnailUrl;
            this.originalPrice = originalPrice;
            this.salePrice = salePrice;
            this.stockCount = stockCount;
            this.lowStock = lowStock;
            this.available = available;
            this.unavailableReason = unavailableReason;
            this.lineTotalPrice = lineTotalPrice;
        }
    }
}
