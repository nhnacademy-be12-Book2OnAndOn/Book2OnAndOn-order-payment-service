package com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cart") // DB
// “장바구니”라는 묶음(aggregate root) 자체를 표현
public class Cart extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long cartId;

    @NotNull
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
}
