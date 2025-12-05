package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.NotSupportedException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

//    @Value("${smart-delivery.api-key}")
    private String sweetTrackerApiKey;


    // 배송 데이터 선 생성 (주문 완료 시 호출)
    // 이거 주문 생성할 때  orderService에서 호출
    public Long createPendingDelivery(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 1:1 관계 중복 체크
        if (deliveryRepository.existsByOrder_OrderId(orderId)) {
            throw new IllegalStateException("이미 배송 정보가 생성된 주문입니다. Order ID: " + orderId);
        }

        // 배송 엔티티 생성
        Delivery delivery = new Delivery(order);

        Delivery savedDelivery = deliveryRepository.save(delivery);

        log.info("배송 데이터 생성 완료: deliveryId={}, orderId={}", savedDelivery.getDeliveryId(), order.getOrderId());

        return savedDelivery.getDeliveryId();
    }


     // admin 운송장 등록
     //상태: PREPARING -> SHIPPING
    public void registerWaybill(Long deliveryId, DeliveryWaybillUpdateDto requestDto) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        // String -> Enum 변환
        DeliveryCompany company = DeliveryCompany.findByName(requestDto.getDeliveryCompany());

        // 정보 업데이트 및 상태 변경
        delivery.registerWaybill(company, requestDto.getWaybill());
        Order order = delivery.getOrder();
        order.updateStatus(OrderStatus.SHIPPING);
        log.info("운송장 등록 완료 (배송시작): ID={}, 택배사={}, 송장={}",
                deliveryId, company.getName(), requestDto.getWaybill());
    }

    // 배송 정보 단일 조회
    @Transactional(readOnly = true)
    public DeliveryResponseDto getDelivery(Long orderId, Long userId) {
        Delivery delivery = deliveryRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new DeliveryNotFoundException("배송 정보를 찾을 수 없습니다. Order ID: " + orderId));

        if(!delivery.getOrder().getUserId().equals(userId)) {
            throw new AccessDeniedException("본인의 배송정보만 조회할 수 있습니다.");
        }

        // API 키 주입하여 DTO 생성 (추적 URL 포함)
        return new DeliveryResponseDto(delivery, sweetTrackerApiKey);
    }

    //admin 배송 목록 조회
    @Transactional(readOnly = true)
    public Page<DeliveryResponseDto> getDeliveries(Pageable pageable, OrderStatus status) {
        Page<Delivery> deliveries;

        if (status == null) {
            deliveries = deliveryRepository.findAll(pageable);
        } else {
            deliveries = deliveryRepository.findAllByOrder_OrderStatus(status, pageable);
        }

        return deliveries.map(delivery -> new DeliveryResponseDto(delivery, sweetTrackerApiKey));
    }


    // admin 배송 정보 수정
    public void updateDeliveryInfo(Long deliveryId, DeliveryWaybillUpdateDto requestDto) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        DeliveryCompany company = DeliveryCompany.findByName(requestDto.getDeliveryCompany());

        delivery.updateTrackingInfo(company, requestDto.getWaybill());

        log.info("배송 정보 수정 완료: deliveryId={}, waybill={}", deliveryId, requestDto.getWaybill());
    }
}