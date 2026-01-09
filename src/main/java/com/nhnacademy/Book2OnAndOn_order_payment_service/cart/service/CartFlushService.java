package com.nhnacademy.book2onandon_order_payment_service.cart.service;

import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.Cart;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartItem;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartRedisItem;
import com.nhnacademy.book2onandon_order_payment_service.cart.repository.CartItemRepository;
import com.nhnacademy.book2onandon_order_payment_service.cart.repository.CartRedisRepository;
import com.nhnacademy.book2onandon_order_payment_service.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartFlushService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRedisRepository cartRedisRepository;

    @Transactional // 외부 호출될 경우 1이 성공하고 2가 실패 시 1은 롤백 안되고, 나머지는 에러처리.
    public void flushSingleUserCart(Long userId) {
        // 장바구니의 모든 아이템 불러오기
        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getUserCartItems(userId);
        log.debug("[Scheduler] Redis user cart 아이템 수 - userId={}, size={}",
                userId, redisItems != null ? redisItems.size() : 0);

        // Cart 엔티티 확보 또는 생성
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));

        // DB의 기존 CartItem 전부 삭제 -> 트랜잭션에 의해 삭제와 삽입 사이에 데이터 유실의 위험은 없다.
        List<CartItem> dbItems = cartItemRepository.findByCart(cart);
        Map<Long, CartItem> dbMap = dbItems.stream()
                .collect(Collectors.toMap(CartItem::getBookId, it -> it));

        // Redis가 비어있다면, DB도 비운 상태로 끝 (DB Flush 과정을 안전하게 종료하는 논리)
        if (redisItems == null || redisItems.isEmpty()) {
            log.info("[Scheduler] Redis cart 비어 있음 -> DB 빈 상태 유지 후 dirty 플래그 해제 - userId={}", userId);
            if (!dbItems.isEmpty()) {
                cartItemRepository.deleteAllInBatch(dbItems);
            }
            cartRedisRepository.clearUserCartDirty(userId);
            return;
        }

        // 2) Redis에는 없고 DB에만 있는 항목 -> delete
        List<Long> toDelete = dbMap.keySet().stream()
                .filter(bookId -> !redisItems.containsKey(bookId))
                .toList();
        if (!toDelete.isEmpty()) {
            cartItemRepository.deleteByCartAndBookIdIn(cart, toDelete);
        }

        // Redis 내용을 기준으로 DB CartItem 재구성
        // 3) Redis에 있는 항목 -> DB upsert(변경된 것만 update, 없는 것은 insert)
        List<CartItem> toSave = new ArrayList<>();
        for (CartRedisItem ri : redisItems.values()) {
            CartItem existing = dbMap.get(ri.getBookId());
            if (existing == null) {
                toSave.add(CartItem.builder()
                        .cart(cart)
                        .bookId(ri.getBookId())
                        .quantity(ri.getQuantity())
                        .selected(ri.isSelected())
                        .build());
            } else {
                boolean changed = (existing.getQuantity() != ri.getQuantity()) || (existing.isSelected() != ri.isSelected());
                if (changed) {
                    existing.updateQuantity(ri.getQuantity());
                    existing.setSelected(ri.isSelected());
                    toSave.add(existing);
                }
            }
        }
        if (!toSave.isEmpty()) {
            cartItemRepository.saveAll(toSave);
        }
        cartRedisRepository.clearUserCartDirty(userId);

        log.info("[Scheduler] flushSingleUserCart 완료 - userId={}, newItemCount={}", userId, toSave.size());
    }
}