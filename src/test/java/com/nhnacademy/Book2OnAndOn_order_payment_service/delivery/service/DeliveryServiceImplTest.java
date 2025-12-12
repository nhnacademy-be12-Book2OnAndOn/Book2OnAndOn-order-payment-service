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
import org.springframework.data.domain.PageRequest;
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
        // Given
        Order order = createEntity(Order.class);
        setField(order, "orderId", 1L);
        setField(order, "orderStatus", OrderStatus.COMPLETED);

        // Delivery 객체 생성 및 Order 주입
        Delivery delivery = createEntity(Delivery.class);
        setField(delivery, "deliveryId", 100L);
        setField(delivery, "order", order); // [중요] Order 객체를 연결해줘야 NPE가 안 납니다.
        setField(delivery, "deliveryCompany", DeliveryCompany.CJ_LOGISTICS); // DTO 변환 시 필요할 수 있음
        setField(delivery, "waybill", "123456789");

        Pageable pageable = PageRequest.of(0, 10);
        Page<Delivery> page = new PageImpl<>(List.of(delivery));

        given(deliveryRepository.findAll(any(Pageable.class))).willReturn(page);

        // When
        Page<DeliveryResponseDto> result = deliveryService.getDeliveries(pageable, null);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderId()).isEqualTo(1L); // 데이터 검증
        verify(deliveryRepository).findAll(any(Pageable.class));

    }

    @Test
    @DisplayName("배송 목록 조회 - 상태값 있음")
    void getDeliveries_WithStatus() {
        // Given
        Order order = createEntity(Order.class);
        setField(order, "orderId", 2L);
        setField(order, "orderStatus", OrderStatus.SHIPPING);

        Delivery delivery = createEntity(Delivery.class);
        setField(delivery, "deliveryId", 200L);
        setField(delivery, "order", order); // [NPE 해결]
        setField(delivery, "deliveryCompany", DeliveryCompany.POST_OFFICE);
        setField(delivery, "waybill", "987654321");

        Pageable pageable = PageRequest.of(0, 10);
        OrderStatus status = OrderStatus.SHIPPING;
        Page<Delivery> page = new PageImpl<>(List.of(delivery));

        given(deliveryRepository.findAllByOrder_OrderStatus(status, pageable)).willReturn(page);

        // When
        Page<DeliveryResponseDto> result = deliveryService.getDeliveries(pageable, status);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderStatus()).isEqualTo("SHIPPING");
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

    private <T> T createEntity(Class<T> clazz) {
        try {
            java.lang.reflect.Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true); // protected 생성자 접근 허용
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Entity 생성 실패 (Reflection Error): " + clazz.getName(), e);
        }
    }

    // 2. Private 필드에 값 주입 (Setter 없이 주입)
    private void setField(Object object, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true); // private 필드 접근 허용
            field.set(object, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("필드 주입 실패: " + fieldName, e);
        }
    }
}