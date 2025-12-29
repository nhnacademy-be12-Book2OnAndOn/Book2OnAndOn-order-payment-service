package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.assembler.OrderViewAssembler;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.assembler.impl.OrderViewAssemblerImpl;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.DeliveryAddressResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderViewAssemblerTest {

    // 인터페이스 타입으로 선언하여 인터페이스의 기능을 테스트함
    private OrderViewAssembler orderViewAssembler;

    @BeforeEach
    void setUp() {
        // 실제 테스트할 때는 구현체를 주입함
        orderViewAssembler = new OrderViewAssemblerImpl();
    }

    @Test
    @DisplayName("배송지 엔티티를 DTO로 변환하는 명세를 검증한다")
    void toDeliveryAddressViewTest() {
        // given
        DeliveryAddress address = mock(DeliveryAddress.class);
        given(address.getDeliveryAddress()).willReturn("광주광역시");
        given(address.getRecipient()).willReturn("홍길동");

        // when
        DeliveryAddressResponseDto result = orderViewAssembler.toDeliveryAddressView(address);

        // then
        assertThat(result.deliveryAddress()).isEqualTo("광주광역시");
        assertThat(result.recipient()).isEqualTo("홍길동");
    }
}