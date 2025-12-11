const API_BASE = {
    CART: '/cart',
    ORDER: '/orders',
    WRAP: '/wrappapers'
};

const USER_ID = 1; // TODO: 실제로 Spring Security Context에서 받아와야 합니다.
const GUEST_ID = 'uuid-test-1234'; // TODO: 실제로 쿠키/로컬 스토리지에서 받아와야 합니다.
const IS_USER = true; // 현재는 회원으로 가정합니다.

let cartData = null; // 장바구니에서 가져온 원본 상품 데이터
let wrapOptions = []; // 서버에서 가져온 포장지 옵션 목록

// --- 1. 초기화 및 데이터 로드 ---
document.addEventListener('DOMContentLoaded', async () => {
    // 1. 배송 희망 날짜 제약 설정
    setDeliveryDateConstraints();

    // 2. 장바구니 및 포장지 옵션 로드
    await loadInitialData();
    setupWrapToggleListeners(); // loadInitialData 완료 후 실행

    // 3. 배송 메시지 동적 입력 로직 (새로 추가된 로직)
    const messageSelect = document.getElementById('deliveryMessage');
    const customInput = document.getElementById('customDeliveryMessage');

    messageSelect.addEventListener('change', (e) => {
        if (e.target.value === 'direct_input') {
            customInput.style.display = 'block';
            customInput.focus();
        } else {
            customInput.style.display = 'none';
            customInput.value = '';
        }
    });
    // 4. 주소 검색 버튼에 이벤트 리스너 추가
    document.querySelector('.btn-search-address').addEventListener('click', openPostcodeSearch);

    // 5. 다음 단계 버튼 이벤트 설정
    document.getElementById('goToPayment').addEventListener('click', handleGoToPayment);
});

async function loadInitialData() {
    // 1. 장바구니 상품 로드 (선택된 상품만)
    const cartEndpoint = IS_USER ? `${API_BASE.CART}/user/items/selected` : `${API_BASE.CART}/guest/items/selected`;
    const cartHeader = {
        'X-User-Id': IS_USER ? USER_ID : undefined,
        'X-Guest-Id': !IS_USER ? GUEST_ID : undefined
    };

    try {
        const cartRes = await fetch(cartEndpoint, { headers: cartHeader });
        if (!cartRes.ok) throw new Error('장바구니 로드 실패');
        cartData = await cartRes.json();

        // 2. 포장지 옵션 로드
        const wrapRes = await fetch(API_BASE.WRAP);
        if (!wrapRes.ok) throw new Error('포장지 옵션 로드 실패');
        const wrapPage = await wrapRes.json();
        wrapOptions = wrapPage.content || [];

        renderProductList();
        updateSummary(cartData.selectedTotalPrice);

    } catch (error) {
        console.error('초기 데이터 로드 중 오류:', error);
        alert('주문 페이지를 로드할 수 없습니다.');
    }
}

function renderProductList() {
    const listContainer = document.getElementById('selectedProductList');
    listContainer.innerHTML = '';

    if (!cartData || cartData.items.length === 0) {
        listContainer.innerHTML = '<p>선택된 상품이 없습니다.</p>';
        return;
    }

    cartData.items.forEach(item => {
        const isPackable = true; //TODO: BookOrderResponse에 isPackable 필드가 있다면 여기서 사용해야 함

        listContainer.innerHTML += `
            <div class="order-item-detail" data-book-id="${item.bookId}">
                <div class="item-info">
                    <span class="item-title">${item.title} (${item.quantity}권)</span>
                    <span class="item-price">가격: ${(item.price * item.quantity).toLocaleString()}원</span>
                </div>
                <div class="item-wrap-option">
                    ${isPackable ? `
                        <label>
                            <input type="checkbox" name="isWrapped_${item.bookId}" data-book-id="${item.bookId}" class="wrap-toggle"> 포장 선택
                        </label>
                        <select name="wrappingPaperId_${item.bookId}" class="wrap-select" disabled>
                            <option value="">-- 포장지 선택 (가격) --</option>
                            ${wrapOptions.map(wrap =>
            `<option value="${wrap.wrappingPaperId}">
                                    ${wrap.wrappingPaperName} (+${wrap.wrappingPaperPrice.toLocaleString()}원)
                                </option>`
        ).join('')}
                        </select>
                    ` : '<span class="non-packable">포장 불가 상품</span>'}
                </div>
            </div>
        `;
    });
}

function updateSummary(amount) {
    document.getElementById('summaryTotalItemPrice').textContent = amount.toLocaleString() + '원';
    document.getElementById('summaryFinalAmount').textContent = amount.toLocaleString() + '원';
    document.getElementById('summaryFees').textContent = '배송비/포장비 (계산 필요)'; // 최종 계산: Payment.html
}

function setupWrapToggleListeners() {
    document.getElementById('selectedProductList').addEventListener('change', (e) => {
        if (e.target.classList.contains('wrap-toggle')) {
            const selectBox = e.target.closest('.item-wrap-option').querySelector('.wrap-select');
            selectBox.disabled = !e.target.checked;
            if (!e.target.checked) {
                selectBox.value = "";
            }
        }
    });
}

function setDeliveryDateConstraints() {
    const dateInput = document.getElementById('wantDeliveryDate');
    const today = new Date();
    const maxDate = new Date();
    maxDate.setDate(today.getDate() + 7);

    const formatDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    dateInput.setAttribute('min', formatDate(today));
    dateInput.setAttribute('max', formatDate(maxDate));
}

// 카카오 우편번호 찾기 팝업을 띄우는 함수
function openPostcodeSearch() {
    new daum.Postcode({
        oncomplete: function(data) {
            let addr = '';
            let extraAddr = '';

            if (data.userSelectedType === 'R') {
                addr = data.roadAddress;
            } else {
                addr = data.jibunAddress;
            }

            if (data.bname !== '' && /[동|로|가]$/g.test(data.bname)){
                extraAddr += data.bname;
            }
            if (data.buildingName !== '' && data.apartment === 'Y'){
                extraAddr += (extraAddr !== '' ? ', ' + data.buildingName : data.buildingName);
            }
            if (extraAddr !== '') {
            }

            document.getElementById('deliveryAddress').value = addr;

            document.getElementById('deliveryAddressDetail').focus();
        },
        // 팝업의 위치와 크기 설정
        width : '100%',
        height : '100%'
    }).open();
}

// --- 2. 다음 단계 버튼 핸들러 ---

function handleGoToPayment() {
    const orderItems = collectOrderItems();
    const deliveryAddress = collectDeliveryAddress();
    const wantDeliveryDate = document.getElementById('wantDeliveryDate').value;

    if (!validateInputs(deliveryAddress)) {
        alert('필수 배송 정보를 모두 입력해주세요.');
        return;
    }

    // 1. OrderCreateRequestDto의 핵심 부분만 구성
    const partialOrderRequest = {
        orderItems: orderItems,
        deliveryAddress: deliveryAddress,
        wantDeliveryDate: wantDeliveryDate,
        totalItemPrice: cartData.selectedTotalPrice
    };

    // 2. localStorage에 저장 후 다음 페이지로 리다이렉트
    localStorage.setItem('partialOrderRequest', JSON.stringify(partialOrderRequest));
    localStorage.setItem('isUserOrder', IS_USER); // 회원/비회원 상태 전달
    window.location.href = 'payment.html';
}

function collectOrderItems() {
    return cartData.items.map(item => {
        const container = document.querySelector(`.order-item-detail[data-book-id="${item.bookId}"]`);
        const isWrappedCheckbox = container ? container.querySelector(`.wrap-toggle`) : null;
        const wrapSelect = container ? container.querySelector(`.wrap-select`) : null;

        const isWrapped = isWrappedCheckbox && isWrappedCheckbox.checked;
        const wrappingPaperId = isWrapped && wrapSelect ? Number(wrapSelect.value) : null;

        // OrderItemRequestDto 형식으로 변환
        return {
            bookId: item.bookId,
            quantity: item.quantity,
            wrappingPaperId: wrappingPaperId,
            isWrapped: isWrapped
        };
    });
}

function collectDeliveryAddress() {
    // DeliveryAddressRequestDto 형식으로 변환
    return {
        deliveryAddress: document.getElementById('deliveryAddress').value,
        deliveryAddressDetail: document.getElementById('deliveryAddressDetail').value,
        deliveryMessage: document.getElementById('deliveryMessage').value,
        recipient: document.getElementById('recipient').value,
        recipientPhonenumber: document.getElementById('recipientPhonenumber').value.replace(/[^0-9]/g, '') // 숫자만 남김
    };
}

function validateInputs(address) {
    // 1. 필수 필드 검사 (기존 로직 유지)
    if (!address.recipient || !address.recipientPhonenumber || !address.deliveryAddress || !document.getElementById('wantDeliveryDate').value) {
        alert('수령인 정보, 주소, 연락처, 희망 배송일을 모두 입력해주세요.');
        return false;
    }

    // 2. 연락처 형식 검사 (010으로 시작하는 11자리 숫자)
    // 주소 DTO의 recipientPhonenumber는 숫자로만 이루어져야 합니다.
    const phoneRegex = /^\d{11}$/;
    if (!phoneRegex.test(address.recipientPhonenumber)) {
        alert('연락처 형식이 올바르지 않습니다. 11자리 숫자로 입력해주세요.');
        return false;
    }

    // 3. 포장지 선택 여부 검사
    const wrapSelects = document.querySelectorAll('.wrap-select:not(:disabled)');

    for (const selectBox of wrapSelects) {
        // 포장 체크박스가 활성화되었으나, 드롭다운에서 아무것도 선택하지 않은 경우
        if (selectBox.value === "") {
            alert('포장을 선택한 상품에 대해 포장지 종류를 반드시 선택해야 합니다.');
            // 해당 상품으로 스크롤 이동/포커스 주는 UX 추가 가능
            return false;
        }
    }
    // 모든 검증 통과
    return true;
}



// 중요: 최종 주문 데이터를 수집할 때,
// messageSelect.value가 'direct_input'이면 customInput.value를 사용해야 합니다.