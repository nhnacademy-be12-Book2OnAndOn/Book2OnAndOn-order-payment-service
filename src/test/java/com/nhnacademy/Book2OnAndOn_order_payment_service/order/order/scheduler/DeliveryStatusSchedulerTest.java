package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.SmartDeliveryClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryCompany;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.scheduler.DeliveryStatusScheduler;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryStatusSchedulerTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private SmartDeliveryClient smartDeliveryClient;

    @InjectMocks
    private DeliveryStatusScheduler deliveryStatusScheduler;

    @Test
    @DisplayName("배송 완료된 건에 대해 주문 상태를 DELIVERED로 갱신한다")
    void checkDeliveryStatus_UpdateSuccess() {
        Delivery delivery = mock(Delivery.class);
        Order order = mock(Order.class);
        DeliveryCompany company = mock(DeliveryCompany.class);

        given(deliveryRepository.findAllByOrder_OrderStatus(OrderStatus.SHIPPING))
                .willReturn(List.of(delivery));
        given(delivery.getDeliveryCompany()).willReturn(company);
        given(company.getCode()).willReturn("LOGIS_CODE");
        given(delivery.getWaybill()).willReturn("123456");
        given(delivery.getOrder()).willReturn(order);
        
        given(smartDeliveryClient.isDeliveryCompleted("LOGIS_CODE", "123456"))
                .willReturn(true);

        deliveryStatusScheduler.checkDeliveryStatus();

        verify(order, times(1)).updateStatus(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("배송이 아직 완료되지 않았으면 상태를 변경하지 않는다")
    void checkDeliveryStatus_StayShipping() {
        Delivery delivery = mock(Delivery.class);
        DeliveryCompany company = mock(DeliveryCompany.class);

        given(deliveryRepository.findAllByOrder_OrderStatus(OrderStatus.SHIPPING))
                .willReturn(List.of(delivery));
        given(delivery.getDeliveryCompany()).willReturn(company);
        given(company.getCode()).willReturn("CODE");
        given(delivery.getWaybill()).willReturn("WAYBILL");
        
        given(smartDeliveryClient.isDeliveryCompleted(anyString(), anyString()))
                .willReturn(false);

        deliveryStatusScheduler.checkDeliveryStatus();

        verify(delivery, times(0)).getOrder();
    }

    @Test
    @DisplayName("API 호출 중 예외가 발생하면 해당 건을 스킵하고 계속 진행한다")
    void checkDeliveryStatus_WithException() {
        Delivery delivery = mock(Delivery.class);
        DeliveryCompany company = mock(DeliveryCompany.class);

        given(deliveryRepository.findAllByOrder_OrderStatus(OrderStatus.SHIPPING))
                .willReturn(List.of(delivery));
        given(delivery.getDeliveryCompany()).willReturn(company);
        
        given(smartDeliveryClient.isDeliveryCompleted(any(), any()))
                .willThrow(new RuntimeException("API Error"));

        deliveryStatusScheduler.checkDeliveryStatus();

        verify(delivery, times(1)).getDeliveryId();
    }

    @Test
    @DisplayName("배송 중인 데이터가 없으면 즉시 종료한다")
    void checkDeliveryStatus_NoData() {
        given(deliveryRepository.findAllByOrder_OrderStatus(OrderStatus.SHIPPING))
                .willReturn(Collections.emptyList());

        deliveryStatusScheduler.checkDeliveryStatus();

        verify(smartDeliveryClient, times(0)).isDeliveryCompleted(anyString(), anyString());
    }
}