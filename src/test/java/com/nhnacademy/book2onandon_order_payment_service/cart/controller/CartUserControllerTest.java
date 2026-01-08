package com.nhnacademy.book2onandon_order_payment_service.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemDeleteRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartItemCountResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartItemsResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartMergeResultResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartRedisItem;
import com.nhnacademy.book2onandon_order_payment_service.cart.repository.CartRedisRepository;
import com.nhnacademy.book2onandon_order_payment_service.cart.service.CartService;
import com.nhnacademy.book2onandon_order_payment_service.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 목표:
 * - CartUserController 커버리지 확보(정상 200 + 헤더/바인딩 400 + merge-status 분기)
 * - 서비스 로직 검증 X, 컨트롤러의 검증/호출/매핑 + merge-status 계산만 검증
 */
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CartUserController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false"
})
class CartUserControllerTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String GUEST_ID_HEADER = "X-Guest-Id";

    private static final Long USER_ID = 1L;
    private static final String UUID = "guest-123";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CartService cartService;

    @MockitoBean
    CartRedisRepository cartRedisRepository;

    /**
     * DTO 필드명이 프로젝트와 다르면 JSON 키만 맞춰서 변경하세요.
     */
    private String validAddItemJson() {
        return """
                {
                  "bookId": 1,
                  "quantity": 2,
                  "selected": true
                }
                """;
    }

    private String validUpdateQuantityJson() {
        return """
                {
                  "bookId": 1,
                  "quantity": 5
                }
                """;
    }

    private String validSelectOneJson() {
        return """
                {
                  "bookId": 1,
                  "selected": true
                }
                """;
    }

    private String validSelectAllJson() {
        return """
                {
                  "selected": true
                }
                """;
    }

    // -------------------------------------------------------
    // 1) GET /cart/user
    // -------------------------------------------------------
    @Nested
    @DisplayName("GET /cart/user")
    class GetUserCart {

        @Test
        @DisplayName("성공: 회원 장바구니 조회(200) + 서비스 호출")
        void ok() throws Exception {
            CartItemsResponseDto response = mock(CartItemsResponseDto.class);
            when(cartService.getUserCart(USER_ID)).thenReturn(response);

            mockMvc.perform(get("/cart/user")
                            .header(USER_ID_HEADER, USER_ID))
                    .andExpect(status().isOk());

            verify(cartService).getUserCart(USER_ID);
        }

        @Test
        @DisplayName("실패: X-User-Id 헤더 누락 -> 400")
        void missingHeader_400() throws Exception {
            mockMvc.perform(get("/cart/user"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }

        @Test
        @DisplayName("실패: X-User-Id=0 -> 400 (validateUserId)")
        void userIdZero_400() throws Exception {
            mockMvc.perform(get("/cart/user")
                            .header(USER_ID_HEADER, 0))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }

        @Test
        @DisplayName("실패: X-User-Id 타입 불일치 -> 400 (TypeMismatch)")
        void userIdTypeMismatch_400() throws Exception {
            mockMvc.perform(get("/cart/user")
                            .header(USER_ID_HEADER, "not-a-number"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }

        @Test
        @DisplayName("실패: X-User-Id 헤더 공백 -> 400 (validateUserId -> USER_ID_REQUIRED/INVALID)")
        void missingHeader_hitsValidateUserIdBlank_400() throws Exception {
            mockMvc.perform(get("/cart/user")
                            .header(USER_ID_HEADER, "   ")) // blank
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }
    }

    // -------------------------------------------------------
    // 2) POST /cart/user/items
    // -------------------------------------------------------
    @Nested
    @DisplayName("POST /cart/user/items")
    class AddItem {

        @Test
        @DisplayName("성공: 담기(200) + 서비스 호출")
        void ok() throws Exception {
            mockMvc.perform(post("/cart/user/items")
                            .header(USER_ID_HEADER, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validAddItemJson()))
                    .andExpect(status().isOk());

            verify(cartService).addItemToUserCart(eq(USER_ID), any());
        }

        @Test
        @DisplayName("실패: 바디 누락 -> 400")
        void missingBody_400() throws Exception {
            mockMvc.perform(post("/cart/user/items")
                            .header(USER_ID_HEADER, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }

        @Test
        @DisplayName("실패: X-User-Id=0 -> 400")
        void invalidUserId_400() throws Exception {
            mockMvc.perform(post("/cart/user/items")
                            .header(USER_ID_HEADER, 0)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validAddItemJson()))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }
    }

    // -------------------------------------------------------
    // 3) PATCH /cart/user/items/quantity
    // -------------------------------------------------------
    @Nested
    @DisplayName("PATCH /cart/user/items/quantity")
    class UpdateQuantity {

        @Test
        @DisplayName("성공: 수량 변경(200) + 서비스 호출")
        void ok() throws Exception {
            mockMvc.perform(patch("/cart/user/items/quantity")
                            .header(USER_ID_HEADER, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUpdateQuantityJson()))
                    .andExpect(status().isOk());

            verify(cartService).updateQuantityUserCartItem(eq(USER_ID), any());
        }

        @Test
        @DisplayName("실패: 바디 누락 -> 400")
        void missingBody_400() throws Exception {
            mockMvc.perform(patch("/cart/user/items/quantity")
                            .header(USER_ID_HEADER, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }
    }

    // -------------------------------------------------------
    // 4) DELETE
    // -------------------------------------------------------
    @Nested
    @DisplayName("DELETE /cart/user/items/{bookId}")
    class DeleteOne {

        @Test
        @DisplayName("성공: 단건 삭제(200) + bookId가 DTO로 감싸져 서비스로 전달")
        void ok_andMapsBookId() throws Exception {
            long bookId = 10L;

            mockMvc.perform(delete("/cart/user/items/{bookId}", bookId)
                            .header(USER_ID_HEADER, USER_ID))
                    .andExpect(status().isOk());

            ArgumentCaptor<CartItemDeleteRequestDto> captor = ArgumentCaptor.forClass(CartItemDeleteRequestDto.class);
            verify(cartService).deleteUserCartItem(eq(USER_ID), captor.capture());

            CartItemDeleteRequestDto passed = captor.getValue();
            assertThat(passed).isNotNull();
            assertThat(passed.getBookId()).isEqualTo(bookId);
        }

        @Test
        @DisplayName("실패: bookId=0 -> 400 (validateBookId)")
        void bookIdZero_400() throws Exception {
            mockMvc.perform(delete("/cart/user/items/{bookId}", 0)
                            .header(USER_ID_HEADER, USER_ID))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }

        @Test
        @DisplayName("실패: bookId 타입 불일치 -> 400 (TypeMismatch)")
        void bookIdTypeMismatch_400() throws Exception {
            mockMvc.perform(delete("/cart/user/items/{bookId}", "not-a-number")
                            .header(USER_ID_HEADER, USER_ID))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }
    }

    // -------------------------------------------------------
    // 5) DELETE /cart/user/items
    // -------------------------------------------------------
    @Nested
    @DisplayName("DELETE /cart/user/items")
    class ClearAll {

        @Test
        @DisplayName("성공: 전체 비우기(200) + 서비스 호출")
        void ok() throws Exception {
            mockMvc.perform(delete("/cart/user/items")
                            .header(USER_ID_HEADER, USER_ID))
                    .andExpect(status().isOk());

            verify(cartService).clearUserCart(USER_ID);
        }
    }

    // -------------------------------------------------------
    // 6) DELETE /cart/user/items/selected
    // -------------------------------------------------------
    @Nested
    @DisplayName("DELETE /cart/user/items/selected")
    class DeleteSelected {

        @Test
        @DisplayName("성공: 선택 항목 삭제(200) + 서비스 호출")
        void ok() throws Exception {
            mockMvc.perform(delete("/cart/user/items/selected")
                            .header(USER_ID_HEADER, USER_ID))
                    .andExpect(status().isOk());

            verify(cartService).deleteSelectedUserCartItems(USER_ID);
        }
    }

    // -------------------------------------------------------
    // 7) PATCH /cart/user/items/select
    // -------------------------------------------------------
    @Nested
    @DisplayName("PATCH /cart/user/items/select")
    class SelectOne {

        @Test
        @DisplayName("성공: 단건 선택/해제(200) + 서비스 호출")
        void ok() throws Exception {
            mockMvc.perform(patch("/cart/user/items/select")
                            .header(USER_ID_HEADER, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validSelectOneJson()))
                    .andExpect(status().isOk());

            verify(cartService).selectUserCartItem(eq(USER_ID), any());
        }
    }

    // -------------------------------------------------------
    // 8) PATCH /cart/user/items/select-all
    // -------------------------------------------------------
    @Nested
    @DisplayName("PATCH /cart/user/items/select-all")
    class SelectAll {

        @Test
        @DisplayName("성공: 전체 선택/해제(200) + 서비스 호출")
        void ok() throws Exception {
            mockMvc.perform(patch("/cart/user/items/select-all")
                            .header(USER_ID_HEADER, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validSelectAllJson()))
                    .andExpect(status().isOk());

            verify(cartService).selectAllUserCartItems(eq(USER_ID), any());
        }
    }

    // -------------------------------------------------------
    // 9) GET /cart/user/items/count
    // -------------------------------------------------------
    @Nested
    @DisplayName("GET /cart/user/items/count")
    class GetCount {

        @Test
        @DisplayName("성공: 개수 조회(200) + 서비스 호출")
        void ok() throws Exception {
            CartItemCountResponseDto response = mock(CartItemCountResponseDto.class);
            when(cartService.getUserCartCount(USER_ID)).thenReturn(response);

            mockMvc.perform(get("/cart/user/items/count")
                            .header(USER_ID_HEADER, USER_ID))
                    .andExpect(status().isOk());

            verify(cartService).getUserCartCount(USER_ID);
        }
    }

    // -------------------------------------------------------
    // 10) GET /cart/user/items/selected
    // -------------------------------------------------------
    @Nested
    @DisplayName("GET /cart/user/items/selected")
    class GetSelectedForOrder {

        @Test
        @DisplayName("성공: 선택된+구매가능 항목 조회(200) + 서비스 호출")
        void ok() throws Exception {
            CartItemsResponseDto response = mock(CartItemsResponseDto.class);
            when(cartService.getUserSelectedCart(USER_ID)).thenReturn(response);

            mockMvc.perform(get("/cart/user/items/selected")
                            .header(USER_ID_HEADER, USER_ID))
                    .andExpect(status().isOk());

            verify(cartService).getUserSelectedCart(USER_ID);
        }
    }

    // -------------------------------------------------------
    // 11) POST /cart/user/merge
    // -------------------------------------------------------
    @Nested
    @DisplayName("POST /cart/user/merge")
    class MergeGuestToUser {

        @Test
        @DisplayName("성공: 비회원 -> 회원 병합(200) + 서비스 호출")
        void ok() throws Exception {
            CartMergeResultResponseDto response = mock(CartMergeResultResponseDto.class);
            when(cartService.mergeGuestCartToUserCart(USER_ID, UUID)).thenReturn(response);

            mockMvc.perform(post("/cart/user/merge")
                            .header(USER_ID_HEADER, USER_ID)
                            .header(GUEST_ID_HEADER, UUID))
                    .andExpect(status().isOk());

            verify(cartService).mergeGuestCartToUserCart(USER_ID, UUID);
        }

        @Test
        @DisplayName("실패: GUEST 헤더 누락 -> 400 (MissingRequestHeader)")
        void missingGuestHeader_400() throws Exception {
            mockMvc.perform(post("/cart/user/merge")
                            .header(USER_ID_HEADER, USER_ID))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }

        @Test
        @DisplayName("실패: GUEST 헤더 공백 -> 400 (validateGuestUuidRequired)")
        void blankGuestHeader_400() throws Exception {
            mockMvc.perform(post("/cart/user/merge")
                            .header(USER_ID_HEADER, USER_ID)
                            .header(GUEST_ID_HEADER, " "))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }

        @Test
        @DisplayName("성공: userCart=null 반환 -> hasUserCart=false (short-circuit 분기 커버)")
        void ok_userCartNull_hasUserCartFalse() throws Exception {
            // uuid는 null로 두든, UUID를 주든 상관없음. 여기선 단순화 위해 null로.
            when(cartService.getUserCart(USER_ID)).thenReturn(null);

            mockMvc.perform(get("/cart/user/merge-status")
                            .header(USER_ID_HEADER, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasUserCart").value(false))
                    .andExpect(jsonPath("$.hasGuestCart").value(false))
                    .andExpect(jsonPath("$.guestItemCount").value(0));

            verify(cartService).getUserCart(USER_ID);
        }

        @Test
        @DisplayName("실패: GUEST 헤더가 공백 -> 400 (validateGuestUuidRequired: blank uuid 분기)")
        void blankGuestHeader_hitsValidateGuestUuidRequiredBlank_400() throws Exception {
            mockMvc.perform(post("/cart/user/merge")
                            .header(USER_ID_HEADER, USER_ID)
                            .header(GUEST_ID_HEADER, "   ")) // blank
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }

    }

    // -------------------------------------------------------
    // 12) GET /cart/user/merge-status
    // -------------------------------------------------------
    @Nested
    @DisplayName("GET /cart/user/merge-status")
    class MergeStatus {

        @Test
        @DisplayName("성공: uuid 미전달(null 허용) -> guestCart 없음, userCart 여부는 userCart totalItemCount로 결정")
        void ok_uuidNull() throws Exception {
            // userCart = empty
            CartItemsResponseDto userCart = mock(CartItemsResponseDto.class);
            when(userCart.getTotalItemCount()).thenReturn(0);
            when(cartService.getUserCart(USER_ID)).thenReturn(userCart);

            mockMvc.perform(get("/cart/user/merge-status")
                            .header(USER_ID_HEADER, USER_ID))
                    .andExpect(status().isOk())
                    // 아래 jsonPath 필드명은 DTO의 getter 기반으로 직렬화될 때의 이름을 가정함
                    .andExpect(jsonPath("$.hasGuestCart").value(false))
                    .andExpect(jsonPath("$.hasUserCart").value(false))
                    .andExpect(jsonPath("$.guestItemCount").value(0));

            verify(cartService).getUserCart(USER_ID);
            verifyNoInteractions(cartRedisRepository);
        }

        @Test
        @DisplayName("실패: uuid='' (blank) -> 400 (validateGuestUuidOptional)")
        void blankUuid_400() throws Exception {
            mockMvc.perform(get("/cart/user/merge-status")
                            .header(USER_ID_HEADER, USER_ID)
                            .header(GUEST_ID_HEADER, " "))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService, cartRedisRepository);
        }

        @Test
        @DisplayName("성공: uuid 전달 + guestCart 존재 + userCart 존재 -> 응답 플래그/개수 계산")
        void ok_uuidProvided_guestAndUserHaveItems() throws Exception {
            // guest cart items: size=2
            Map<Long, CartRedisItem> guestItems = new HashMap<>();
            guestItems.put(1L, mock(CartRedisItem.class));
            guestItems.put(2L, mock(CartRedisItem.class));

            when(cartRedisRepository.getGuestCartItems(UUID)).thenReturn(guestItems);

            // userCart totalItemCount > 0
            CartItemsResponseDto userCart = mock(CartItemsResponseDto.class);
            when(userCart.getTotalItemCount()).thenReturn(3);
            when(cartService.getUserCart(USER_ID)).thenReturn(userCart);

            mockMvc.perform(get("/cart/user/merge-status")
                            .header(USER_ID_HEADER, USER_ID)
                            .header(GUEST_ID_HEADER, UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasGuestCart").value(true))
                    .andExpect(jsonPath("$.hasUserCart").value(true))
                    .andExpect(jsonPath("$.guestItemCount").value(2));

            verify(cartRedisRepository).getGuestCartItems(UUID);
            verify(cartService).getUserCart(USER_ID);
        }

        @Test
        @DisplayName("성공: uuid 전달했지만 repo가 null 반환 -> hasGuestCart=false로 처리")
        void ok_uuidProvided_repoReturnsNull() throws Exception {
            when(cartRedisRepository.getGuestCartItems(UUID)).thenReturn(null);

            CartItemsResponseDto userCart = mock(CartItemsResponseDto.class);
            when(userCart.getTotalItemCount()).thenReturn(1);
            when(cartService.getUserCart(USER_ID)).thenReturn(userCart);

            mockMvc.perform(get("/cart/user/merge-status")
                            .header(USER_ID_HEADER, USER_ID)
                            .header(GUEST_ID_HEADER, UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasGuestCart").value(false))
                    .andExpect(jsonPath("$.hasUserCart").value(true))
                    .andExpect(jsonPath("$.guestItemCount").value(0));

            verify(cartRedisRepository).getGuestCartItems(UUID);
            verify(cartService).getUserCart(USER_ID);
        }
    }
}
