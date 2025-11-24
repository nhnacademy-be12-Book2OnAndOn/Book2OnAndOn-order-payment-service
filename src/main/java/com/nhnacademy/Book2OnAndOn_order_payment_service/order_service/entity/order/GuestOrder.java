package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "GuestOrder")
public class GuestOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long guestId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "guest_name", length = 50, nullable = false)
    private String guestName;

    @Column(name = "guest_phonenumber", length = 11, nullable = false)
    private String guestPhonenumber;

    @Column(name = "guest_password", length = 255, nullable = false)
    private String guestPassword;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "order_id")
    private Order order;
}