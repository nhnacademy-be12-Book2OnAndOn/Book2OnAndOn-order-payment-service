// ============================
// 기본 설정
// ============================

const userId = localStorage.getItem('userId');
let uuid = localStorage.getItem('uuid');

const isGuest = !userId;
const API_BASE = '/cart';
const USE_DUMMY = false; // 나중에 false로 변경

// 서버 응답 전체를 담아둘 상태 (배송비, 최종 결제금액 포함)
let cartSummaryData = null;

// ============================
// 더미 데이터 (BookSnapshot 필드 포함)
// ============================

const DUMMY_ITEMS = [
    {
        bookId: 1,
        title: '자바의 정석',
        thumbnailUrl: '',
        originalPrice: 35000,
        price: 30000,
        stockCount: 15,
        saleEnded: false,
        deleted: false,
        hidden: false,
        quantity: 2,
        selected: true,
    },
    {
        bookId: 2,
        title: '스프링 부트와 AWS로 혼자 구현하는 웹 서비스',
        thumbnailUrl: '',
        originalPrice: 30000,
        price: 27000,
        stockCount: 3,
        saleEnded: false,
        deleted: false,
        hidden: false,
        quantity: 1,
        selected: true,
    },
    {
        bookId: 3,
        title: '클린 코드',
        thumbnailUrl: '',
        originalPrice: 29000,
        price: 29000,
        stockCount: 0,
        saleEnded: false,
        deleted: false,
        hidden: false,
        quantity: 1,
        selected: false,
    },
    {
        bookId: 4,
        title: '리팩터링 2판',
        thumbnailUrl: '',
        originalPrice: 45000,
        price: 38000,
        stockCount: 20,
        saleEnded: true,
        deleted: false,
        hidden: false,
        quantity: 1,
        selected: true,
    },
    {
        bookId: 5,
        title: '모던 자바스크립트 Deep Dive',
        thumbnailUrl: '',
        originalPrice: 45000,
        price: 45000,
        stockCount: 8,
        saleEnded: false,
        deleted: true,
        hidden: false,
        quantity: 1,
        selected: false,
    },
];

let cartItems = USE_DUMMY ? [...DUMMY_ITEMS] : [];

// ============================
// 서버에서 장바구니 조회
// ============================

async function loadCartFromServer() {
    try {
        let url;
        const headers = {
            'Content-Type': 'application/json',
        };

        if (userId) {
            // 회원 장바구니 조회: GET /cart/user
            url = `${API_BASE}/user`;
            headers['X-User-Id'] = userId;
        } else {
            // 비회원 장바구니 조회: GET /cart/guest
            url = `${API_BASE}/guest`;
            headers['X-Guest-Id'] = uuid;
        }

        const response = await fetch(url, {
            method: 'GET',
            headers
        });

        if (!response.ok) {
            console.error('장바구니 조회 실패', response.status);
            return;
        }

        const data = await response.json(); // CartItemsResponseDto 구조
        cartSummaryData = data;
        cartItems = data.items || [];

        renderCart();
    } catch (e) {
        console.error('장바구니 조회 중 오류', e);
    }
}

// ============================
// 렌더링
// ============================

function renderCart() {
    const cartContent = document.getElementById('cartContent');
    const cartSummary = document.getElementById('cartSummary');

    if (!cartItems || cartItems.length === 0) {
        cartContent.innerHTML = `
      <div class="empty-cart">
        <div class="empty-cart-icon">🛒</div>
        <h2>장바구니가 비어있습니다</h2>
        <p>원하는 책을 담아보세요!</p>
      </div>
    `;
        cartSummary.style.display = 'none';
        return;
    }

    cartSummary.style.display = 'block';

    cartContent.innerHTML = `
    <div class="cart-items">
      ${cartItems.map(item => {
        const isUnavailable = item.deleted || item.hidden || item.saleEnded;
        const isOutOfStock = item.stockCount === 0;
        const isLowStock = item.stockCount > 0 && item.stockCount <= 5;
        const hasDiscount = item.originalPrice > item.price;
        const discountRate = hasDiscount ? Math.round((1 - item.price / item.originalPrice) * 100) : 0;

        return `
          <div class="cart-item ${isUnavailable ? 'item-unavailable-overlay' : ''}">
            <div class="item-checkbox">
              <input type="checkbox"
                ${item.selected ? 'checked' : ''}
                ${isUnavailable || isOutOfStock ? 'disabled' : ''}
                onchange="toggleItem(${item.bookId})">
            </div>
            <div class="item-image">
              ${item.thumbnailUrl
            ? `<img src="${item.thumbnailUrl}" alt="${item.title}">`
            : '책 이미지'}
            </div>
            <div class="item-details">
              <div class="item-title">${item.title}</div>
              <div class="item-meta">
                ${isOutOfStock
            ? '<span class="item-badge badge-stock out">품절</span>'
            : isLowStock
                ? `<span class="item-badge badge-stock low">재고 ${item.stockCount}개</span>`
                : `<span class="item-badge badge-stock">재고 ${item.stockCount}개</span>`
        }
                ${item.saleEnded ? '<span class="item-badge badge-sale">판매종료</span>' : ''}
                ${item.deleted ? '<span class="item-badge badge-unavailable">삭제된 상품</span>' : ''}
                ${item.hidden ? '<span class="item-badge badge-unavailable">숨김 상품</span>' : ''}
                ${hasDiscount && !isUnavailable ? `<span class="item-badge badge-discount">${discountRate}% 할인</span>` : ''}
              </div>
              <div class="item-price-section">
                ${hasDiscount ? `<span class="item-original-price">${item.originalPrice.toLocaleString()}원</span>` : ''}
                <span class="item-price">${item.price.toLocaleString()}원</span>
                ${hasDiscount ? `<span class="item-discount-rate">${discountRate}%↓</span>` : ''}
              </div>
            </div>
            <div class="item-controls">
              <div class="quantity-control">
                <button class="quantity-btn"
                  onclick="updateQuantity(${item.bookId}, ${item.quantity - 1})"
                  ${isUnavailable || isOutOfStock ? 'disabled' : ''}>-</button>
                <div class="quantity-display">${item.quantity}</div>
                <button class="quantity-btn"
                  onclick="updateQuantity(${item.bookId}, ${item.quantity + 1})"
                  ${isUnavailable || isOutOfStock || item.quantity >= item.stockCount ? 'disabled' : ''}>+</button>
              </div>
              <div class="item-total">${(item.price * item.quantity).toLocaleString()}원</div>
              <button class="btn-remove" onclick="removeItem(${item.bookId})">삭제</button>
            </div>
          </div>
        `;
    }).join('')}
    </div>
  `;

    updateSummary();
    updateSelectAllCheckbox();
}

function updateSummary() {
    const subtotalElem = document.getElementById('subtotal');
    const shippingElem = document.getElementById('shipping');
    const totalElem = document.getElementById('total');

    // 더미 모드일 때는 기존 방식 유지
    if (USE_DUMMY) {
        const selectedItems = cartItems.filter(item =>
            item.selected &&
            !item.deleted &&
            !item.hidden &&
            !item.saleEnded &&
            item.stockCount > 0
        );
        const subtotal = selectedItems.reduce((sum, item) => sum + item.price * item.quantity, 0);

        subtotalElem.textContent = subtotal.toLocaleString() + '원';
        shippingElem.textContent = '무료';
        totalElem.textContent = subtotal.toLocaleString() + '원';
        return;
    }

    // 실제 서버 데이터 기반
    if (!cartSummaryData) {
        subtotalElem.textContent = '0원';
        shippingElem.textContent = '0원';
        totalElem.textContent = '0원';
        return;
    }

    const selectedTotalPrice = cartSummaryData.selectedTotalPrice || 0;
    const deliveryFee = cartSummaryData.deliveryFee || 0;
    const finalPaymentAmount = cartSummaryData.finalPaymentAmount || 0;

    subtotalElem.textContent = selectedTotalPrice.toLocaleString() + '원';
    shippingElem.textContent =
        deliveryFee === 0 ? '무료' : deliveryFee.toLocaleString() + '원';
    totalElem.textContent = finalPaymentAmount.toLocaleString() + '원';
}

function updateSelectAllCheckbox() {
    const selectAllCheckbox = document.getElementById('selectAll');
    const availableItems = cartItems.filter(item =>
        !item.deleted && !item.hidden && !item.saleEnded && item.stockCount > 0
    );
    const allSelected = availableItems.length > 0 && availableItems.every(item => item.selected);
    selectAllCheckbox.checked = allSelected;
}

// ============================
// 액션
// ============================

async function toggleSelectAll() {
    const selectAllCheckbox = document.getElementById('selectAll');
    const selectAll = selectAllCheckbox.checked;

    // 더미 모드
    if (USE_DUMMY) {
        cartItems.forEach(item => {
            if (!item.deleted && !item.hidden && !item.saleEnded && item.stockCount > 0) {
                item.selected = selectAll;
            }
        });
        renderCart();
        return;
    }

    // 실제 API 모드
    try {
        let url;
        const body = JSON.stringify({ selected: selectAll });
        const headers = {
            'Content-Type': 'application/json'
        };

        if (userId) {
            // 회원: PATCH /cart/items/select-all
            url = `${API_BASE}/user/items/select-all`;
            headers['X-User-Id'] = userId;
        } else {
            // 비회원: PATCH /cart/guest/items/select-all
            url = `${API_BASE}/guest/items/select-all`;
            headers['X-Guest-Id'] = uuid;
        }

        const res = await fetch(url, {
            method: 'PATCH',
            headers,
            body
        });

        if (!res.ok) {
            console.error('전체 선택/해제 실패', res.status);
            alert('전체 선택/해제 중 문제가 발생했습니다.');
            return;
        }

        await loadCartFromServer();
    } catch (e) {
        console.error('전체 선택/해제 중 오류', e);
        alert('전체 선택/해제 중 오류가 발생했습니다.');
    }
}

async function toggleItem(bookId) {
    const item = cartItems.find(i => i.bookId === bookId);
    if (!item) return;

    // 더미 모드
    if (USE_DUMMY) {
        item.selected = !item.selected;
        renderCart();
        return;
    }

    // 실제 API 모드
    try {
        let url;
        const nextSelected = !item.selected;

        const body = JSON.stringify({
            bookId: bookId,
            selected: nextSelected
        });

        const headers = {
            'Content-Type': 'application/json'
        };

        if (userId) {
            // 회원: PATCH /cart/items/select
            url = `${API_BASE}/user/items/select`;
            headers['X-User-Id'] = userId;
        } else {
            // 비회원: PATCH /cart/guest/items/select..
            url = `${API_BASE}/guest/items/select`;
            headers['X-Guest-Id'] = uuid;
        }

        const res = await fetch(url, {
            method: 'PATCH',
            headers,
            body
        });

        if (!res.ok) {
            console.error('선택/해제 실패', res.status);
            alert('선택/해제 중 문제가 발생했습니다.');
            return;
        }

        await loadCartFromServer();
    } catch (e) {
        console.error('선택/해제 중 오류', e);
        alert('선택/해제 중 오류가 발생했습니다.');
    }
}

async function updateQuantity(bookId, newQuantity) {
    const item = cartItems.find(i => i.bookId === bookId);
    if (!item) return;

    // 간단한 프론트 유효성 검사
    if (newQuantity < 1 || newQuantity > item.stockCount) return;

    // 더미 모드
    if (USE_DUMMY) {
        item.quantity = newQuantity;
        renderCart();
        return;
    }

    // 실제 API 모드
    try {
        let url;
        const body = JSON.stringify({
            bookId: bookId,
            quantity: newQuantity
        });

        const headers = {
            'Content-Type': 'application/json'
        };

        if (userId) {
            // 회원: PATCH /cart/user/items/quantity
            url = `${API_BASE}/user/items/quantity`;
            headers['X-User-Id'] = userId;
        } else {
            // 비회원: PATCH /cart/guest/items/quantity.
            url = `${API_BASE}/guest/items/quantity`;
            headers['X-Guest-Id'] = uuid;
        }

        const res = await fetch(url, {
            method: 'PATCH',
            headers,
            body
        });

        if (!res.ok) {
            console.error('수량 변경 실패', res.status);
            alert('수량 변경 중 문제가 발생했습니다.');
            return;
        }

        await loadCartFromServer();
    } catch (e) {
        console.error('수량 변경 중 오류', e);
        alert('수량 변경 중 오류가 발생했습니다.');
    }
}

async function removeItem(bookId) {
    if (!confirm('이 상품을 삭제하시겠습니까?')) return;

    // 더미 모드
    if (USE_DUMMY) {
        cartItems = cartItems.filter(item => item.bookId !== bookId);
        renderCart();
        return;
    }

    // 실제 API 모드
    try {
        let url;
        const headers = {
            'Content-Type': 'application/json'
        };

        if (userId) {
            // 회원: DELETE /cart/user/items/{bookId}
            url = `${API_BASE}/user/items/${bookId}`;
            headers['X-User-Id'] = userId;
        } else {
            // 비회원: DELETE /cart/guest/items/{bookId}?uuid=...
            url = `${API_BASE}/guest/items/${bookId}`;
            headers['X-Guest-Id'] = uuid;
        }

        const res = await fetch(url, {
            method: 'DELETE',
            headers
        });

        if (!res.ok) {
            console.error('상품 삭제 실패', res.status);
            alert('상품 삭제 중 문제가 발생했습니다.');
            return;
        }

        await loadCartFromServer();
    } catch (e) {
        console.error('상품 삭제 중 오류', e);
        alert('상품 삭제 중 오류가 발생했습니다.');
    }
}

async function deleteSelected() {
    const selectedItems = cartItems.filter(item => item.selected);
    if (selectedItems.length === 0) {
        alert('선택된 상품이 없습니다.');
        return;
    }

    if (!confirm(`선택한 ${selectedItems.length}개 상품을 삭제하시겠습니까?`)) return;

    // 더미 모드
    if (USE_DUMMY) {
        cartItems = cartItems.filter(item => !item.selected);
        renderCart();
        return;
    }

    // 실제 API 모드
    try {
        let url;
        const headers = {
            'Content-Type': 'application/json'
        };

        if (userId) {
            // 회원: DELETE /cart/user/items/selected
            url = `${API_BASE}/user/items/selected`;
            headers['X-User-Id'] = userId;
        } else {
            // 비회원: DELETE /cart/guest/items/selected
            url = `${API_BASE}/guest/items/selected`;
            headers['X-Guest-Id'] = uuid;
        }

        const res = await fetch(url, {
            method: 'DELETE',
            headers
        });

        if (!res.ok) {
            console.error('선택 삭제 실패', res.status);
            alert('선택 삭제 중 문제가 발생했습니다.');
            return;
        }

        await loadCartFromServer();
    } catch (e) {
        console.error('선택 삭제 중 오류', e);
        alert('선택 삭제 중 오류가 발생했습니다.');
    }
}

async function clearCart() {
    if (cartItems.length === 0) {
        alert('장바구니가 비어있습니다.');
        return;
    }

    if (!confirm('장바구니를 전체 삭제하시겠습니까?')) return;

    // 더미 모드
    if (USE_DUMMY) {
        cartItems = [];
        renderCart();
        return;
    }

    // 실제 API 모드
    try {
        let url;
        const headers = {
            'Content-Type': 'application/json'
        };

        if (userId) {
            // 회원: DELETE /cart/items
            url = `${API_BASE}/user/items`;
            headers['X-User-Id'] = userId;
        } else {
            // 비회원: DELETE /cart/guest/items
            url = `${API_BASE}/guest/items`;
            headers['X-Guest-Id'] = uuid;
        }

        const res = await fetch(url, {
            method: 'DELETE',
            headers
        });

        if (!res.ok) {
            console.error('전체 삭제 실패', res.status);
            alert('전체 삭제 중 문제가 발생했습니다.');
            return;
        }

        await loadCartFromServer();
    } catch (e) {
        console.error('전체 삭제 중 오류', e);
        alert('전체 삭제 중 오류가 발생했습니다.');
    }
}

function checkout() {
    const selectedItems = cartItems.filter(item =>
        item.selected &&
        !item.deleted &&
        !item.hidden &&
        !item.saleEnded &&
        item.stockCount > 0
    );

    if (selectedItems.length === 0) {
        alert('주문할 수 있는 상품을 선택해주세요.');
        return;
    }

    const total = selectedItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
    alert(`${selectedItems.length}개 상품 / 총 ${total.toLocaleString()}원\n주문 페이지로 이동합니다.`);

    // 실제 주문 페이지로 이동하는 로직은 나중에 연결
}

async function initCartPage() {
    if (USE_DUMMY) {
        renderCart();
        return;
    }

    await loadCartFromServer(); // 기존 장바구니 렌더링

    // 로그인 상태 + uuid가 있는 경우에만 merge-status 조회
    if (userId && uuid) {
        await checkMergeStatusAndMaybeOpenModal();
    }
}

async function checkMergeStatusAndMaybeOpenModal() {
    try {
        const res = await fetch('/cart/user/merge-status', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': userId,
                'X-Guest-Id': uuid
            }
        });

        if (!res.ok) {
            console.error('merge-status 조회 실패', res.status);
            return;
        }

        const data = await res.json(); // CartMergeStatusResponseDto

        // 1) 게스트 카트가 아예 없으면 아무것도 안 함
        if (!data.hasGuestCart) {
            return;
        }

        // 2) 게스트 O + 회원 X → 자동 병합
        if (data.hasGuestCart && !data.hasUserCart) {
            // 자동 병합 후 간단 안내만 띄우고 끝
            await mergeGuestCart(true); // true = autoMergeFlag 정도로
            return;
        }

        // 3) 게스트 O + 회원 O → 모달 띄워서 선택형 병합
        if (data.hasGuestCart && data.hasUserCart) {
            openMergeModal(data.guestItemCount);
        }
    } catch (e) {
        console.error('merge-status 조회 중 오류', e);
    }
}


function openMergeModal(guestItemCount) {
    const confirmMerge = confirm(
        `비회원 장바구니에 ${guestItemCount}개의 상품이 있습니다.\n` +
        `현재 회원 장바구니와 병합하시겠습니까?`
    );

    if (confirmMerge) {
        mergeGuestCart();
    } else {
        // 정책에 따라:
        // 1) 그냥 아무것도 안 하기 (게스트 카트 유지)
        // 2) 게스트 카트 바로 삭제
        //   fetch('/cart/user/guest-clear', ...) 같은 API 만들어서 처리
    }
}

async function mergeGuestCart(isAuto = false) {
    try {
        const res = await fetch('/cart/user/merge', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': userId,
                'X-Guest-Id': uuid
            }
        });

        if (!res.ok) {
            alert('장바구니 병합 중 오류가 발생했습니다.');
            return;
        }

        const mergeResult = await res.json();

        // 병합 성공 시 uuid 정리할지 정책에 따라 선택
        // if (mergeResult.mergeSucceeded) {
        //     localStorage.removeItem('uuid');
        //     uuid = null;
        // }

        if (isAuto) {
            // 자동 병합 케이스라면 살짝 안내 한 줄 정도
            alert('비회원 장바구니를 회원 장바구니로 자동 병합했습니다.');
        } else {
            // 모달에서 사용자가 "예"를 누른 병합 케이스
            alert('장바구니 병합이 완료되었습니다.');
        }

        await loadCartFromServer();
    } catch (e) {
        console.error('merge 호출 중 오류', e);
        alert('장바구니 병합 중 오류가 발생했습니다.');
    }
}




// ============================
// 초기화
// ============================

initCartPage();
// if (USE_DUMMY) {
//     renderCart();
// } else {
//     loadCartFromServer();
// }
