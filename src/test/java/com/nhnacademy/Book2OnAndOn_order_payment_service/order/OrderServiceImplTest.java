//package com.nhnacademy.Book2OnAndOn_order_payment_service.order;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.BookServiceClient;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.CouponServiceClient;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.UserServiceClient;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.BookOrderResponse;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.CurrentPointResponseDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareRequestDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareResponseDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.provider.OrderNumberProvider;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderServiceImpl;
//import java.util.List;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//
//@ExtendWith(MockitoExtension.class)
//class OrderServiceImplTest {
//
//    @InjectMocks
//    private OrderServiceImpl orderService;
//
//    @MockitoBean
//    private BookServiceClient bookServiceClient;
//    @MockitoBean
//    private UserServiceClient userServiceClient;
//    @MockitoBean
//    private CouponServiceClient couponServiceClient;
//    @MockitoBean
//    private OrderNumberProvider orderNumberProvider;
//
//    @Test
//    void 주문_준비_정상_조회() {
//        // given
//        Long userId = 1L;
//        OrderPrepareRequestDto req =
//                new OrderPrepareRequestDto(
//                        List.of(new OrderPrepareRequestDto.BookInfoDto(1L, 2))
//                );
//
//        given(bookServiceClient.getBooksForOrder(any()))
//                .willReturn(List.of(new BookOrderResponse(1L, "test", 100, 100, "imageUrl", true, 3, "test", 2)));
//        given(userServiceClient.getUserAddresses(userId))
//                .willReturn(List.of());
//        given(couponServiceClient.getUsableCoupons(eq(userId), any()))
//                .willReturn(List.of());
//        given(userServiceClient.getUserPoint(userId))
//                .willReturn(new CurrentPointResponseDto(1000));
//        given(orderNumberProvider.provideOrderNumber())
//                .willReturn("B2-000000000001");
//
//        // when
//        OrderPrepareResponseDto result = orderService.prepareOrder(userId, req);
//
//        // then
//        assertThat(result.orderNumber()).isEqualTo("B2-000000000001");
//    }
//}
