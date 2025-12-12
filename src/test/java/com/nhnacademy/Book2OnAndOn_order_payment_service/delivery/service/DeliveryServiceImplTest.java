package com.nhnacademy.Book2OnAndOn_order_payment_service.delivery.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryWaybillUpdateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryCompany;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.DeliveryNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @InjectMocks
    private DeliveryService deliveryService;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        // @Value 필드 주입
        ReflectionTestUtils.setField(deliveryService, "sweetTrackerApiKey", "test-api-key");
    }

    @Test
    @DisplayName("배송 데이터 생성 - 성공")
    void createPendingDelivery_Success() {
        // given
        Long orderId = 1L;
        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(orderId);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(deliveryRepository.existsByOrder_OrderId(orderId)).willReturn(false);

        Delivery savedDelivery = mock(Delivery.class);
        given(savedDelivery.getDeliveryId()).willReturn(10L);
        given(deliveryRepository.save(any(Delivery.class))).willReturn(savedDelivery);

        // when
        Long result = deliveryService.createPendingDelivery(orderId);

        // then
        assertThat(result).isEqualTo(10L);
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    @DisplayName("배송 데이터 생성 - 실패: 주문 없음")
    void createPendingDelivery_Fail_OrderNotFound() {
        // given
        Long orderId = 1L;
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deliveryService.createPendingDelivery(orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("배송 데이터 생성 - 실패: 이미 배송 정보 존재")
    void createPendingDelivery_Fail_AlreadyExists() {
        // given
        Long orderId = 1L;
        Order order = mock(Order.class);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(deliveryRepository.existsByOrder_OrderId(orderId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> deliveryService.createPendingDelivery(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 배송 정보가 생성된 주문입니다");
    }

    @Test
    @DisplayName("운송장 등록 - 성공")
    void registerWaybill_Success() {
        // given
        Long deliveryId = 1L;
        DeliveryWaybillUpdateDto dto = new DeliveryWaybillUpdateDto("CJ대한통운", "123456");

        Delivery delivery = mock(Delivery.class);
        Order order = mock(Order.class);

        given(deliveryRepository.findById(deliveryId)).willReturn(Optional.of(delivery));
        given(delivery.getOrder()).willReturn(order);

        // when
        deliveryService.registerWaybill(deliveryId, dto);

        // then
        verify(delivery).registerWaybill(any(DeliveryCompany.class), eq("123456"));
        verify(order).updateStatus(OrderStatus.SHIPPING);
    }

    @Test
    @DisplayName("운송장 등록 - 실패: 배송 정보 없음")
    void registerWaybill_Fail_NotFound() {
        // given
        Long deliveryId = 1L;
        DeliveryWaybillUpdateDto dto = new DeliveryWaybillUpdateDto("CJ대한통운", "123456");
        given(deliveryRepository.findById(deliveryId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deliveryService.registerWaybill(deliveryId, dto))
                .isInstanceOf(DeliveryNotFoundException.class);
    }

    @Test
    @DisplayName("배송 정보 단일 조회 - 성공")
    void getDelivery_Success() {
        // given
        Long orderId = 1L;
        Long userId = 100L;

        Delivery delivery = mock(Delivery.class);
        Order order = mock(Order.class);

        given(deliveryRepository.findByOrder_OrderId(orderId)).willReturn(Optional.of(delivery));
        given(delivery.getOrder()).willReturn(order);
        given(order.getUserId()).willReturn(userId);

        // when
        DeliveryResponseDto result = deliveryService.getDelivery(orderId, userId);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("배송 정보 단일 조회 - 실패: 본인 아님")
    void getDelivery_Fail_AccessDenied() {
        // given
        Long orderId = 1L;
        Long userId = 100L;
        Long otherUser = 999L;

        Delivery delivery = mock(Delivery.class);
        Order order = mock(Order.class);

        given(deliveryRepository.findByOrder_OrderId(orderId)).willReturn(Optional.of(delivery));
        given(delivery.getOrder()).willReturn(order);
        given(order.getUserId()).willReturn(otherUser); // 다른 유저

        // when & then
        assertThatThrownBy(() -> deliveryService.getDelivery(orderId, userId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("배송 목록 조회 - 상태값 없음(전체 조회)")
    void getDeliveries_All() {
        // given
        Pageable pageable = Pageable.unpaged();
        Page<Delivery> page = new PageImpl<>(List.of(mock(Delivery.class)));
        given(deliveryRepository.findAll(pageable)).willReturn(page);

        // when
        Page<DeliveryResponseDto> result = deliveryService.getDeliveries(pageable, null);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(deliveryRepository).findAll(pageable);
    }

    @Test
    @DisplayName("배송 목록 조회 - 상태값 있음")
    void getDeliveries_WithStatus() {
        // given
        Pageable pageable = Pageable.unpaged();
        OrderStatus status = OrderStatus.SHIPPING;
        Page<Delivery> page = new PageImpl<>(List.of(mock(Delivery.class)));
        given(deliveryRepository.findAllByOrder_OrderStatus(status, pageable)).willReturn(page);

        // when
        Page<DeliveryResponseDto> result = deliveryService.getDeliveries(pageable, status);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(deliveryRepository).findAllByOrder_OrderStatus(status, pageable);
    }

    @Test
    @DisplayName("배송 정보 수정 - 성공")
    void updateDeliveryInfo_Success() {
        // given
        Long deliveryId = 1L;
        DeliveryWaybillUpdateDto dto = new DeliveryWaybillUpdateDto("우체국택배", "999999");
        Delivery delivery = mock(Delivery.class);

        given(deliveryRepository.findById(deliveryId)).willReturn(Optional.of(delivery));

        // when
        deliveryService.updateDeliveryInfo(deliveryId, dto);

        // then
        verify(delivery).updateTrackingInfo(any(DeliveryCompany.class), eq("999999"));
    }
}