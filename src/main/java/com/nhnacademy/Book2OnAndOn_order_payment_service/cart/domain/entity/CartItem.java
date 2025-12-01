package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity;

import static com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartConstants.MAX_QUANTITY;
import static com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartConstants.MIN_QUANTITY;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "cart_item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cart_book",
                        columnNames = {"cart_id", "book_id"}
                )
        }
) // DB
// 장바구니 안의 “한 줄(line item) 도메인 엔티티”
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long cartItemId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;  // Cart 1 : N CartItem

    @NotNull
    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Min(1)
    @Max(99)
    @NotNull
    @Column(name = "cart_item_quantity", nullable = false)
    private int quantity;

    @NotNull
    @Column(name = "cart_item_selected", nullable = false)
    private boolean selected;

    // 낙관적 락은 반드시 JPA를 통해 UPDATE가 나갈 때만 동작한다.
    @Version
    @Column(name = "version")
    private Long version;

    // 안정적 검증
    public void updateQuantity(int newQuantity) {
        validateQuantityRange(newQuantity);
        this.quantity = newQuantity;
    }

    public void updateSelected(boolean selected) {
        this.selected = selected;
    }

    private void validateQuantityRange(int quantity) {
        validate(quantity >= MIN_QUANTITY && quantity <= MAX_QUANTITY, "수량은 최소 " +MIN_QUANTITY+ "개에서 최대 "+MAX_QUANTITY+"개까지 입니다. : " + quantity);
    }

    private void validate(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}

