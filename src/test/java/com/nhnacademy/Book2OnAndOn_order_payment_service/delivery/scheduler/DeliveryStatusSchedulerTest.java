package com.nhnacademy.Book2OnAndOn_order_payment_service.delivery.scheduler;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.SmartDeliveryClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryCompany;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.scheduler.DeliveryStatusScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryStatusSchedulerTest {

    @InjectMocks
    private DeliveryStatusScheduler scheduler;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private SmartDeliveryClient smartDeliveryClient;

    @Test
    @DisplayName("배송 완료 확인 시 주문 상태가 DELIVERED로 변경되어야 한다")
    void checkDeliveryStatus_updates_to_delivered() {
        // Given
        Order order = createEntity(Order.class);
        setField(order, "orderId", 1L);
        setField(order, "orderStatus", OrderStatus.SHIPPING);

        Delivery delivery = createEntity(Delivery.class);
        setField(delivery, "deliveryId", 10L);
        setField(delivery, "order", order);
        setField(delivery, "deliveryCompany", DeliveryCompany.POST_OFFICE);
        setField(delivery, "waybill", "123456789");

        // Mocking
        given(deliveryRepository.findAllByOrder_OrderStatus(OrderStatus.SHIPPING))
                .willReturn(List.of(delivery));

        given(smartDeliveryClient.isDeliveryCompleted("01", "123456789"))
                .willReturn(true);

        // When
        scheduler.checkDeliveryStatus();

        // Then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(smartDeliveryClient, times(1)).isDeliveryCompleted("01", "123456789");
    }

    @Test
    @DisplayName("배송이 아직 완료되지 않았으면 상태는 SHIPPING을 유지해야 한다")
    void checkDeliveryStatus_keeps_shipping_status() {
        // Given
        Order order = createEntity(Order.class);
        setField(order, "orderId", 2L);
        setField(order, "orderStatus", OrderStatus.SHIPPING);

        Delivery delivery = createEntity(Delivery.class);
        setField(delivery, "deliveryId", 20L);
        setField(delivery, "order", order);
        setField(delivery, "deliveryCompany", DeliveryCompany.CJ_LOGISTICS);
        setField(delivery, "waybill", "555555555");

        given(deliveryRepository.findAllByOrder_OrderStatus(OrderStatus.SHIPPING))
                .willReturn(List.of(delivery));

        given(smartDeliveryClient.isDeliveryCompleted(anyString(), anyString()))
                .willReturn(false);

        // When
        scheduler.checkDeliveryStatus();

        // Then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SHIPPING);
    }

    @Test
    @DisplayName("외부 API 조회 중 예외가 발생해도 다음 배송 건을 처리해야 한다")
    void checkDeliveryStatus_handles_exception_and_continues() {
        // Given
        Order order1 = createEntity(Order.class);
        setField(order1, "orderId", 100L);
        setField(order1, "orderStatus", OrderStatus.SHIPPING);
        Delivery delivery1 = createEntity(Delivery.class);
        setField(delivery1, "deliveryId", 100L);
        setField(delivery1, "order", order1);
        setField(delivery1, "deliveryCompany", DeliveryCompany.HANJIN);
        setField(delivery1, "waybill", "ERROR_WAYBILL");

        Order order2 = createEntity(Order.class);
        setField(order2, "orderId", 200L);
        setField(order2, "orderStatus", OrderStatus.SHIPPING);
        Delivery delivery2 = createEntity(Delivery.class);
        setField(delivery2, "deliveryId", 200L);
        setField(delivery2, "order", order2);
        setField(delivery2, "deliveryCompany", DeliveryCompany.LOTTE);
        setField(delivery2, "waybill", "SUCCESS_WAYBILL");

        given(deliveryRepository.findAllByOrder_OrderStatus(OrderStatus.SHIPPING))
                .willReturn(List.of(delivery1, delivery2));

        given(smartDeliveryClient.isDeliveryCompleted("05", "ERROR_WAYBILL"))
                .willThrow(new RuntimeException("API Error"));

        given(smartDeliveryClient.isDeliveryCompleted("08", "SUCCESS_WAYBILL"))
                .willReturn(true);

        // When
        scheduler.checkDeliveryStatus();

        // Then
        assertThat(order1.getOrderStatus()).isEqualTo(OrderStatus.SHIPPING);
        assertThat(order2.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("대상 배송 건이 없으면 아무 일도 일어나지 않는다")
    void checkDeliveryStatus_empty_list() {
        given(deliveryRepository.findAllByOrder_OrderStatus(OrderStatus.SHIPPING))
                .willReturn(Collections.emptyList());

        scheduler.checkDeliveryStatus();

        verify(smartDeliveryClient, never()).isDeliveryCompleted(anyString(), anyString());
    }

    // ================= Helper Methods =================
    private <T> T createEntity(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true); // private/protected 접근 허용
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Entity 생성 실패 (Reflection Error): " + clazz.getName(), e);
        }
    }

    // 필드 값을 강제로 주입하는 메서드
    private void setField(Object object, String fieldName, Object value) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true); // private 필드 접근 허용
            field.set(object, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("필드 주입 실패: " + fieldName, e);
        }
    }
}