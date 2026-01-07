package com.nhnacademy.book2onandon_order_payment_service.order.entity.order;

import com.nhnacademy.book2onandon_order_payment_service.order.converter.PhoneNumberConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "GuestOrder")
public class GuestOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guest_id")
    private Long guestId;

    @Column(name = "guest_name", length = 50)
    @NotNull
    private String guestName;

    @Convert(converter = PhoneNumberConverter.class)
    @Column(name = "guest_phonenumber", length = 100)
    @NotNull
    private String guestPhoneNumber;

    @Column(name = "guest_password", length = 255)
    @Size(max = 255)
    @NotNull
    private String guestPassword;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_number", referencedColumnName = "order_number")
    private Order order;

}