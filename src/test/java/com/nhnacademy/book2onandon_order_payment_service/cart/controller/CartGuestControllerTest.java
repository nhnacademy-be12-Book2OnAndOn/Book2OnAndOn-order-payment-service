package com.nhnacademy.book2onandon_order_payment_service.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemDeleteRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartItemCountResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartItemsResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartBusinessException;
import com.nhnacademy.book2onandon_order_payment_service.cart.service.CartService;
import com.nhnacademy.book2onandon_order_payment_service.cart.support.CartCalculator;
import com.nhnacademy.book2onandon_order_payment_service.exception.GlobalExceptionHandler;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 목표:
 * - 컨트롤러 레이어 커버리지 확보 (요구사항: Sonar 80%+)
 * - 정상(200) + 헤더/바인딩/검증 실패(400) 위주
 * - 서비스 로직 검증 X, 호출/매핑만 검증
 */
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CartGuestController.class)
class CartGuestControllerTest {

    private static final String GUEST_ID_HEADER = "X-Guest-Id";
    private static final String UUID = "guest-123";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CartService cartService;

    /**
     * 주의:
     * DTO 필드명이 다르면 여기 JSON 키만 프로젝트 DTO에 맞게 바꾸세요.
     * (예: bookId -> itemId 등)
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

    // ---------- calculate ----------
    @Test
    void calculatePricing_whenBookIdNull_throwInvalidBookId() {
        CartCalculator calc = new CartCalculator();

        assertThatThrownBy(() -> calc.calculatePricing(null, 1, Map.of()))
                .isInstanceOf(CartBusinessException.class);
    }

    @Test
    void calculatePricing_whenBookIdZero_throwInvalidBookId() {
        CartCalculator calc = new CartCalculator();

        assertThatThrownBy(() -> calc.calculatePricing(0L, 1, Map.of()))
                .isInstanceOf(CartBusinessException.class);
    }


    // ---------- GET /cart/guest ----------
    @Nested
    @DisplayName("GET /cart/guest")
    class GetGuestCart {

        @Test
        @DisplayName("성공: 비회원 장바구니 조회(200) + 서비스 호출 확인")
        void ok() throws Exception {
            CartItemsResponseDto response = mock(CartItemsResponseDto.class);
            when(cartService.getGuestCart(UUID)).thenReturn(response);

            mockMvc.perform(get("/cart/guest")
                            .header(GUEST_ID_HEADER, UUID))
                    .andExpect(status().isOk());

            verify(cartService).getGuestCart(UUID);
        }

        @Test
        @DisplayName("실패: 헤더 누락 -> 400 (@RequestHeader 필수)")
        void missingHeader_400() throws Exception {
            mockMvc.perform(get("/cart/guest"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }

        @Test
        @DisplayName("실패: 헤더 공백 -> 400 (validateUuid -> CartBusinessException)")
        void blankHeader_400() throws Exception {
            mockMvc.perform(get("/cart/guest")
                            .header(GUEST_ID_HEADER, " "))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }
    }

    // ---------- POST /cart/guest/items ----------
    @Nested
    @DisplayName("POST /cart/guest/items")
    class AddItem {

        @Test
        @DisplayName("성공: 아이템 추가(200) + 서비스 호출 확인")
        void ok() throws Exception {
            mockMvc.perform(post("/cart/guest/items")
                            .header(GUEST_ID_HEADER, UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validAddItemJson()))
                    .andExpect(status().isOk());

            // DTO 내부 검증은 @Valid가 처리, 컨트롤러는 서비스 호출만 확인
            verify(cartService).addItemToGuestCart(eq(UUID), any());
        }

        @Test
        @DisplayName("실패: 헤더 공백 -> 400")
        void blankHeader_400() throws Exception {
            mockMvc.perform(post("/cart/guest/items")
                            .header(GUEST_ID_HEADER, " ")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validAddItemJson()))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }

        @Test
        @DisplayName("실패: 바디 누락 -> 400 (JSON 바인딩 실패)")
        void missingBody_400() throws Exception {
            mockMvc.perform(post("/cart/guest/items")
                            .header(GUEST_ID_HEADER, UUID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }
    }

    // ---------- PATCH /cart/guest/items/quantity ----------
    @Nested
    @DisplayName("PATCH /cart/guest/items/quantity")
    class UpdateQuantity {

        @Test
        @DisplayName("성공: 수량 변경(200) + 서비스 호출 확인")
        void ok() throws Exception {
            mockMvc.perform(patch("/cart/guest/items/quantity")
                            .header(GUEST_ID_HEADER, UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUpdateQuantityJson()))
                    .andExpect(status().isOk());

            verify(cartService).updateQuantityGuestCartItem(eq(UUID), any());
        }

        @Test
        @DisplayName("실패: 바디 누락 -> 400")
        void missingBody_400() throws Exception {
            mockMvc.perform(patch("/cart/guest/items/quantity")
                            .header(GUEST_ID_HEADER, UUID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }
    }

    // ---------- PATCH /cart/guest/items/select ----------
    @Nested
    @DisplayName("PATCH /cart/guest/items/select")
    class SelectOne {

        @Test
        @DisplayName("성공: 단건 선택/해제(200) + 서비스 호출 확인")
        void ok() throws Exception {
            mockMvc.perform(patch("/cart/guest/items/select")
                            .header(GUEST_ID_HEADER, UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validSelectOneJson()))
                    .andExpect(status().isOk());

            verify(cartService).selectGuestCartItem(eq(UUID), any());
        }

        @Test
        @DisplayName("실패: 바디 누락 -> 400")
        void missingBody_400() throws Exception {
            mockMvc.perform(patch("/cart/guest/items/select")
                            .header(GUEST_ID_HEADER, UUID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }
    }

    // ---------- PATCH /cart/guest/items/select-all ----------
    @Nested
    @DisplayName("PATCH /cart/guest/items/select-all")
    class SelectAll {

        @Test
        @DisplayName("성공: 전체 선택/해제(200) + 서비스 호출 확인")
        void ok() throws Exception {
            mockMvc.perform(patch("/cart/guest/items/select-all")
                            .header(GUEST_ID_HEADER, UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validSelectAllJson()))
                    .andExpect(status().isOk());

            verify(cartService).selectAllGuestCartItems(eq(UUID), any());
        }

        @Test
        @DisplayName("실패: 바디 누락 -> 400")
        void missingBody_400() throws Exception {
            mockMvc.perform(patch("/cart/guest/items/select-all")
                            .header(GUEST_ID_HEADER, UUID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }
    }

    // ---------- DELETE /cart/guest/items/{bookId} ----------
    @Nested
    @DisplayName("DELETE /cart/guest/items/{bookId}")
    class DeleteOne {

        @Test
        @DisplayName("성공: 단건 삭제(200) + bookId가 DTO로 감싸져 서비스에 전달된다")
        void ok_andMapsBookId() throws Exception {
            long bookId = 10L;

            mockMvc.perform(delete("/cart/guest/items/{bookId}", bookId)
                            .header(GUEST_ID_HEADER, UUID))
                    .andExpect(status().isOk());

            ArgumentCaptor<CartItemDeleteRequestDto> captor = ArgumentCaptor.forClass(CartItemDeleteRequestDto.class);
            verify(cartService).deleteGuestCartItem(eq(UUID), captor.capture());

            CartItemDeleteRequestDto passed = captor.getValue();
            assertThat(passed).isNotNull();
            assertThat(passed.getBookId()).isEqualTo(bookId);
        }

        @Test
        @DisplayName("실패: pathVariable 타입 불일치 -> 400")
        void invalidBookIdType_400() throws Exception {
            mockMvc.perform(delete("/cart/guest/items/{bookId}", "not-a-number")
                            .header(GUEST_ID_HEADER, UUID))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }

        @Test
        @DisplayName("실패: bookId=0 -> 400 (validateBookId -> CartBusinessException)")
        void bookIdZero_400() throws Exception {
            mockMvc.perform(delete("/cart/guest/items/{bookId}", 0)
                            .header(GUEST_ID_HEADER, UUID))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }

        @Test
        @DisplayName("실패: bookId<0 -> 400 (validateBookId -> CartBusinessException)")
        void bookIdNegative_400() throws Exception {
            mockMvc.perform(delete("/cart/guest/items/{bookId}", -1)
                            .header(GUEST_ID_HEADER, UUID))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }
    }

    // ---------- DELETE /cart/guest/items/selected ----------
    @Nested
    @DisplayName("DELETE /cart/guest/items/selected")
    class DeleteSelected {

        @Test
        @DisplayName("성공: 선택된 항목 삭제(200) + 서비스 호출 확인")
        void ok() throws Exception {
            mockMvc.perform(delete("/cart/guest/items/selected")
                            .header(GUEST_ID_HEADER, UUID))
                    .andExpect(status().isOk());

            verify(cartService).deleteSelectedGuestCartItems(UUID);
        }

        @Test
        @DisplayName("실패: 헤더 공백 -> 400")
        void blankHeader_400() throws Exception {
            mockMvc.perform(delete("/cart/guest/items/selected")
                            .header(GUEST_ID_HEADER, " "))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }
    }

    // ---------- GET /cart/guest/items/selected ----------
    @Nested
    @DisplayName("GET /cart/guest/items/selected")
    class GetSelectedForOrder {

        @Test
        @DisplayName("성공: 선택된+구매가능 항목 조회(200) + 서비스 호출 확인")
        void ok() throws Exception {
            CartItemsResponseDto response = mock(CartItemsResponseDto.class);
            when(cartService.getGuestSelectedCart(UUID)).thenReturn(response);

            mockMvc.perform(get("/cart/guest/items/selected")
                            .header(GUEST_ID_HEADER, UUID))
                    .andExpect(status().isOk());

            verify(cartService).getGuestSelectedCart(UUID);
        }

        @Test
        @DisplayName("실패: 헤더 누락 -> 400")
        void missingHeader_400() throws Exception {
            mockMvc.perform(get("/cart/guest/items/selected"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }
    }

    // ---------- GET /cart/guest/items/count ----------
    @Nested
    @DisplayName("GET /cart/guest/items/count")
    class GetCount {

        @Test
        @DisplayName("성공: 장바구니 개수 조회(200) + 서비스 호출 확인")
        void ok() throws Exception {
            CartItemCountResponseDto response = mock(CartItemCountResponseDto.class);
            when(cartService.getGuestCartCount(UUID)).thenReturn(response);

            mockMvc.perform(get("/cart/guest/items/count")
                            .header(GUEST_ID_HEADER, UUID))
                    .andExpect(status().isOk());

            verify(cartService).getGuestCartCount(UUID);
        }

        @Test
        @DisplayName("실패: 헤더 공백 -> 400")
        void blankHeader_400() throws Exception {
            mockMvc.perform(get("/cart/guest/items/count")
                            .header(GUEST_ID_HEADER, " "))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }
    }

    // ---------- DELETE /cart/guest/items ----------
    @Nested
    @DisplayName("DELETE /cart/guest/items")
    class ClearAll {

        @Test
        @DisplayName("성공: 장바구니 비우기(200) + 서비스 호출 확인")
        void ok() throws Exception {
            mockMvc.perform(delete("/cart/guest/items")
                            .header(GUEST_ID_HEADER, UUID))
                    .andExpect(status().isOk());

            verify(cartService).clearGuestCart(UUID);
        }

        @Test
        @DisplayName("실패: 헤더 공백 -> 400")
        void blankHeader_400() throws Exception {
            mockMvc.perform(delete("/cart/guest/items")
                            .header(GUEST_ID_HEADER, " "))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(cartService);
        }
    }
}
