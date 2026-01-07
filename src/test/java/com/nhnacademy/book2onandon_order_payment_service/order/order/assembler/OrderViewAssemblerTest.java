package com.nhnacademy.book2onandon_order_payment_service.order.order.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.nhnacademy.book2onandon_order_payment_service.order.assembler.OrderViewAssembler;
import com.nhnacademy.book2onandon_order_payment_service.order.assembler.impl.OrderViewAssemblerImpl;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.DeliveryAddressResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderViewAssemblerTest {

    private OrderViewAssembler orderViewAssembler;

    @BeforeEach
    void setUp() {
        orderViewAssembler = new OrderViewAssemblerImpl();
    }

    @Test
    @DisplayName("배송지 엔티티를 DTO로 변환하는 명세를 검증한다")
    void toDeliveryAddressViewTest() {
        DeliveryAddress address = mock(DeliveryAddress.class);
        given(address.getDeliveryAddress()).willReturn("광주광역시");
        given(address.getRecipient()).willReturn("홍길동");

        DeliveryAddressResponseDto result = orderViewAssembler.toDeliveryAddressView(address);

        assertThat(result.deliveryAddress()).isEqualTo("광주광역시");
        assertThat(result.recipient()).isEqualTo("홍길동");
    }
}