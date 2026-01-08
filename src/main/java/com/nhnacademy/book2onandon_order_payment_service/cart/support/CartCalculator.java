package com.nhnacademy.book2onandon_order_payment_service.cart.support;

import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartItemUnavailableReason;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartBusinessException;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartErrorCode;
import com.nhnacademy.book2onandon_order_payment_service.client.BookServiceClient.BookSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartConstants.LOW_STOCK_THRESHOLD;

@Component
@RequiredArgsConstructor
public class CartCalculator {

    public CartItemPricingResult calculatePricing(
            Long bookId,
            int quantity,
            Map<Long, BookSnapshot> snapshotMap
    ) {
        // 1) 입력 검증 (커스텀 예외로 통일)
        if (bookId == null || bookId <= 0) {
            throw new CartBusinessException(CartErrorCode.INVALID_BOOK_ID);
        }
        if (quantity <= 0) {
            throw new CartBusinessException(CartErrorCode.INVALID_QUANTITY);
        }
        if (snapshotMap == null) {
            // 스냅샷 맵 자체가 null이면 시스템/연동 문제에 가까운데,
            // 일단 BAD_REQUEST로 떨어뜨리기보다 "도서 정보를 불러올 수 없음"으로 통일.
            throw new CartBusinessException(CartErrorCode.BOOK_SNAPSHOT_NOT_FOUND);
        }

        // 2) snapshot 조회
        BookSnapshot snapshot = snapshotMap.get(bookId);
        if (snapshot == null) {
            // book-service에서 해당 bookId 스냅샷을 못 받았거나, 조회 대상에 포함되지 않음
            throw new CartBusinessException(CartErrorCode.BOOK_SNAPSHOT_NOT_FOUND,
                    "book snapshot을 찾을 수 없습니다. bookId=" + bookId);
        }

        // 3) 필드 채우기
        String title = snapshot.getTitle();
        String thumbnailUrl = snapshot.getThumbnailUrl();
        int originalPrice = snapshot.getOriginalPrice();
        int salePrice = snapshot.getSalePrice();
        int stock = snapshot.getStockCount();
        boolean lowStock = (stock > 0 && stock < LOW_STOCK_THRESHOLD);

        // 4) 주문 가능 여부 판단
        boolean available = true;
        CartItemUnavailableReason reason = null;

        if (snapshot.isDeleted()) {
            available = false;
            reason = CartItemUnavailableReason.BOOK_DELETED;
        } else if (snapshot.isSaleEnded()) {
            available = false;
            reason = CartItemUnavailableReason.SALE_ENDED;
        } else if (stock <= 0) {
            available = false;
            reason = CartItemUnavailableReason.OUT_OF_STOCK;
        }

        // 5) 최종 금액
        int lineTotalPrice = salePrice * quantity;

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

    public record CartItemPricingResult(
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
        // 정적 팩토리로 의미 부여 + 추후 파라미터 순서 실수 방지에 유리
        public static CartItemPricingResult of(
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
            return new CartItemPricingResult(
                    title,
                    thumbnailUrl,
                    originalPrice,
                    salePrice,
                    stockCount,
                    lowStock,
                    available,
                    unavailableReason,
                    lineTotalPrice
            );
        }
    }
}
