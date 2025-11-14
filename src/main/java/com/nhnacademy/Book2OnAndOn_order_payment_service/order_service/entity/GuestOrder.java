package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 비회원 주문시 기록하는
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Guest_order")
public class GuestOrder {
    @Id
    private Long orderId;

    private String guestName;

    private String guestAddress;

    private String guestPassword;

    /** 비밀번호는 암호화하여 저장해야 함 */
}