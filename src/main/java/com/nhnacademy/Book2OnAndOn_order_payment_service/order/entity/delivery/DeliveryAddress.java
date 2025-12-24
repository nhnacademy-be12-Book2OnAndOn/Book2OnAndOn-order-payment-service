package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.converter.PhoneNumberConverter;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.DeliveryAddressResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
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
@Table(name = "DeliveryAddress")
public class DeliveryAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_address_id")
    private Long deliveryAddressId;

    @Column(name = "delivery_address", length = 100)
    @NotNull
    private String deliveryAddress;

    @Column(name = "delivery_address_detail", length = 100)
    private String deliveryAddressDetail;

    @Column(name = "delivery_message", length = 100)
    private String deliveryMessage;

    @Column(name = "recipient", length = 50)
    @NotNull
    private String recipient;

    @Convert(converter = PhoneNumberConverter.class)
    @Column(name = "recipient_phonenumber", length = 100)
    @NotNull
    private String recipientPhoneNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @NotNull
    private Order order;
}