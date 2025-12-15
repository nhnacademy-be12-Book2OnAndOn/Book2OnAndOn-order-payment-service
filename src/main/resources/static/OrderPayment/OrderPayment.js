// =================================================================
// checkout.js: 통합 주문/결제 로직 (TOSS V2 FINAL VERSION)
// =================================================================

// --- 상수 및 전역 변수 영역 (Order & Payment 공통) ---
const API_BASE = {
    CART: '/cart',
    ORDER: '/orders', // Mock 환경에서 서버 통신 없이 사용
    WRAP: '/wrappapers',
    TOSS_CONFIRM: '/payment/TOSS/confirm'
};

const USER_ID = 10;
const GUEST_ID = 'uuid-test-1234';
const IS_USER = true;

const TOSS_CLIENT_KEY = "test_ck_Z1aOwX7K8m1x1vJ2AgDQ8yQxzvNP";
const FIXED_DELIVERY_FEE = 3000;
const FREE_DELIVERY_THRESHOLD = 30000;
const CURRENT_POINT = 12500;

let cartData = null;
let wrapOptions = [];
let selectedWrapData = {};
let currentBookId = null;
let isUserOrder = IS_USER;

// --- 1. 초기화 및 데이터 로드 ---
document.addEventListener('DOMContentLoaded', async () => {
    setDeliveryDateOptions();
    await loadInitialData();
    setupEventListeners();
    calculateFinalAmount();
});


// =================================================================
// I. ORDER LOGIC (주문 상품, 배송지, 포장지 관리)
// =================================================================

async function loadInitialData() {
    // 테스트용 Mock 데이터 정의 (총 금액 60000원)
    cartData = {
        selectedTotalPrice: 60000,
        items: [
            { bookId: 101, title: "클린 코드 (Clean Code) 기초편", quantity: 1, price: 30000, isPackable: true },
            { bookId: 102, title: "객체지향 설계와 원리 심화", quantity: 2, price: 15000, isPackable: true }
        ]
    };
    wrapOptions = [
        { wrappingPaperId: 5, wrappingPaperName: "🎁 고급 선물 포장", wrappingPaperPrice: 5000, wrappingPaperPath: "https://via.placeholder.com/150/99e699/333333?text=Premium+Wrap" },
        { wrappingPaperId: 6, wrappingPaperName: "♻️ 친환경 에코 포장", wrappingPaperPrice: 2000, wrappingPaperPath: "https://via.placeholder.com/150/d4f0d4/333333?text=Eco+Wrap" },
        { wrappingPaperId: 7, wrappingPaperName: "💌 메시지 카드 포함", wrappingPaperPrice: 1000, wrappingPaperPath: "https://via.placeholder.com/150/e0e0e0/333333?text=Message+Card" },
        { wrappingPaperId: 8, wrappingPaperName: "파손 방지 (무료)", wrappingPaperPrice: 0, wrappingPaperPath: "https://via.placeholder.com/150/f0f0f0/333333?text=Protection+Wrap" }
    ];
    console.log("✅ Mock 테스트 데이터 로드 완료.");

    renderProductList();
}

function setupEventListeners() {

    // 1. 배송 메시지 동적 입력 로직 (Order)
    const messageSelect = document.getElementById('deliveryMessage');
    const customInput = document.getElementById('customDeliveryMessage');
    if (messageSelect) {
        messageSelect.addEventListener('change', (e) => {
            if (e.target.value === 'direct_input') {
                customInput.style.display = 'block';
                customInput.focus();
            } else {
                customInput.style.display = 'none';
                customInput.value = '';
            }
        });
    }

    // 2. 주소 검색 버튼 이벤트 설정 (Order)
    document.querySelector('.btn-search-address')?.addEventListener('click', openPostcodeSearch);

    // 3. 최종 결제 버튼 이벤트 설정 (Payment)
    document.getElementById('requestTossPayment')?.addEventListener('click', handleTossPaymentRequest);

    // 4. 포장 토글 및 버튼 활성화 리스너 (Order)
    setupWrapToggleListeners();

    // 5. 포장지 선택 버튼 클릭 이벤트 (모달 오픈)
    document.getElementById('selectedProductList')?.addEventListener('click', (e) => {
        if (e.target.classList.contains('btn-select-wrap')) {
            const bookId = Number(e.target.getAttribute('data-book-id'));

            const itemTitleFull = e.target.closest('.order-item-detail')
                .querySelector('.item-title').textContent;
            const itemTitle = itemTitleFull.substring(0, itemTitleFull.lastIndexOf('(')).trim();

            if (!e.target.disabled) {
                openWrappingModal(bookId, itemTitle);
            }
        }
    });

    // 6. 모달 닫기 버튼 및 외부 클릭 설정 (Order)
    document.querySelector('#wrappingModal .close-button')?.addEventListener('click', closeModal);
    window.addEventListener('click', (e) => {
        if (e.target === document.getElementById('wrappingModal')) {
            closeModal();
        }
    });

    // 7. 할인 계산 이벤트 리스너 (Payment)
    document.getElementById('couponSelect')?.addEventListener('change', calculateFinalAmount);
    document.getElementById('pointDiscountAmount')?.addEventListener('input', calculateFinalAmount);
}

function renderProductList() {
    const listContainer = document.getElementById('selectedProductList');
    if (!listContainer) return;

    listContainer.innerHTML = '';

    if (!cartData || cartData.items.length === 0) {
        listContainer.innerHTML = '<p>선택된 상품이 없습니다.</p>';
        return;
    }

    cartData.items.forEach(item => {
        const isPackable = true;
        const currentWrapId = selectedWrapData[item.bookId];
        const wrapText = currentWrapId
            ? `선택됨: ${getWrapNameById(currentWrapId)}`
            : '포장지 선택/변경';
        const isDisabled = currentWrapId ? '' : 'disabled';
        const isChecked = currentWrapId ? 'checked' : '';
        const totalItemPrice = (item.price * item.quantity).toLocaleString();

        listContainer.innerHTML += `
            <div class="order-item-detail" data-book-id="${item.bookId}">
                <div class="item-info">
                    <span class="item-title">${item.title} (${item.quantity}권)</span>
                    <span class="item-price">가격: ${totalItemPrice}원</span>
                </div>
                <div class="item-wrap-option">
                    ${isPackable ? `
                        <label>
                            <input type="checkbox" name="isWrapped_${item.bookId}" data-book-id="${item.bookId}" class="wrap-toggle" ${isChecked}> 포장 선택
                        </label>
                        <button type="button" 
                                class="btn-select-wrap" 
                                data-book-id="${item.bookId}" 
                                ${isDisabled}> 
                            ${wrapText}
                        </button>
                    ` : '<span class="non-packable">포장 불가 상품</span>'}
                </div>
            </div>
        `;
    });
}

function getWrapNameById(id) {
    const wrap = wrapOptions.find(opt => opt.wrappingPaperId === id);
    return wrap ? wrap.wrappingPaperName : '선택됨';
}

function getWrapDataById(id) {
    return wrapOptions.find(opt => opt.wrappingPaperId === id);
}

function setupWrapToggleListeners() {
    document.getElementById('selectedProductList')?.addEventListener('change', (e) => {
        if (e.target.classList.contains('wrap-toggle')) {
            const bookId = Number(e.target.getAttribute('data-book-id'));
            const selectButton = e.target.closest('.item-wrap-option').querySelector('.btn-select-wrap');

            selectButton.disabled = !e.target.checked;

            if (e.target.checked) {
                const itemTitleFull = e.target.closest('.order-item-detail')
                    .querySelector('.item-title').textContent;
                const itemTitle = itemTitleFull.substring(0, itemTitleFull.lastIndexOf('(')).trim();

                if (!selectedWrapData[bookId]) {
                    openWrappingModal(bookId, itemTitle);
                }
            } else {
                selectedWrapData[bookId] = null;
                selectButton.textContent = '포장지 선택/변경';
            }
            calculateFinalAmount();
        }
    });
}

function openWrappingModal(bookId, bookTitle) {
    currentBookId = bookId;
    const modalElement = document.getElementById('wrappingModal');
    if (!modalElement) {
        console.error("Fatal Error: wrappingModal 요소를 찾을 수 없습니다.");
        return;
    }

    document.getElementById('modalTitle').textContent = `[${bookTitle}] 포장 옵션 선택`;
    renderOptionsInModal();
    modalElement.style.display = 'block';

    const currentSelection = selectedWrapData[bookId];
    document.querySelectorAll('.wrap-card').forEach(c => c.classList.remove('selected'));
    if (currentSelection) {
        const selectedCard = document.querySelector(`.wrap-card[data-wrap-id="${currentSelection}"]`);
        if (selectedCard) selectedCard.classList.add('selected');
        document.getElementById('confirmWrapButton').disabled = false;
    } else {
        document.getElementById('confirmWrapButton').disabled = true;
    }
}

function closeModal() {
    document.getElementById('wrappingModal').style.display = 'none';
}

function renderOptionsInModal() {
    const optionsContainer = document.getElementById('wrappingOptions');
    if (!optionsContainer) return;
    optionsContainer.innerHTML = '';

    wrapOptions.forEach(option => {
        const card = document.createElement('div');
        card.className = 'wrap-card';
        card.setAttribute('data-wrap-id', option.wrappingPaperId);
        card.innerHTML = `
            <img src="${option.wrappingPaperPath}" alt="${option.wrappingPaperName}">
            <p><strong>${option.wrappingPaperName}</strong></p>
            <p>${option.wrappingPaperPrice.toLocaleString()}원</p>
        `;
        card.addEventListener('click', () => {
            handleOptionSelection(card, option);
        });
        optionsContainer.appendChild(card);
    });
}

function handleOptionSelection(selectedCard, wrapData) {
    document.querySelectorAll('.wrap-card').forEach(c => c.classList.remove('selected'));
    selectedCard.classList.add('selected');
    selectedWrapData[currentBookId] = wrapData.wrappingPaperId;

    const confirmButton = document.getElementById('confirmWrapButton');
    confirmButton.disabled = false;
    confirmButton.onclick = () => {
        finalizeWrapSelection(currentBookId, wrapData);
    };
}

function finalizeWrapSelection(bookId, wrapData) {
    closeModal();
    const selectButton = document.querySelector(`.order-item-detail[data-book-id="${bookId}"] .btn-select-wrap`);
    if (selectButton) {
        selectButton.textContent = `${wrapData.wrappingPaperName} (+${wrapData.wrappingPaperPrice.toLocaleString()}원) 선택 완료`;
    }
    calculateFinalAmount();
}

function collectOrderItems() {
    if (!cartData || !cartData.items) return [];

    return cartData.items.map(item => {
        const container = document.querySelector(`.order-item-detail[data-book-id="${item.bookId}"]`);
        const isWrappedCheckbox = container ? container.querySelector(`.wrap-toggle`) : null;
        const isWrapped = isWrappedCheckbox && isWrappedCheckbox.checked;
        const wrappingPaperId = isWrapped ? selectedWrapData[item.bookId] : null;

        const wrapData = wrappingPaperId ? getWrapDataById(wrappingPaperId) : null;

        return {
            bookId: item.bookId,
            quantity: item.quantity,
            wrappingPaperId: wrappingPaperId,
            isWrapped: isWrapped,
            wrappingPaperPrice: wrapData ? wrapData.wrappingPaperPrice : 0
        };
    });
}

function collectDeliveryAddress() {
    let deliveryMessage = document.getElementById('deliveryMessage')?.value;

    if (deliveryMessage === 'direct_input') {
        deliveryMessage = document.getElementById('customDeliveryMessage')?.value || '요청사항 없음';
    }

    return {
        deliveryAddress: document.getElementById('deliveryAddress')?.value,
        deliveryAddressDetail: document.getElementById('deliveryAddressDetail')?.value,
        deliveryMessage: deliveryMessage,
        recipient: document.getElementById('recipient')?.value,
        recipientPhonenumber: document.getElementById('recipientPhonenumber')?.value.replace(/[^0-9]/g, '')
    };
}

function validateInputs(address, orderItems) {
    if (!address.recipient || !address.recipientPhonenumber || !address.deliveryAddress || !document.getElementById('wantDeliveryDate')?.value) {
        alert('수령인 정보, 주소, 연락처, 희망 배송일을 모두 입력해주세요.');
        return false;
    }
    const phoneRegex = /^\d{11}$/;
    if (!phoneRegex.test(address.recipientPhonenumber)) {
        alert('연락처 형식이 올바르지 않습니다. 11자리 숫자로 입력해주세요.');
        return false;
    }
    for (const item of orderItems) {
        if (item.isWrapped && !item.wrappingPaperId) {
            alert(`도서 ID ${item.bookId}에 대해 포장을 선택했지만, 포장지 종류를 선택하지 않았습니다.`);
            return false;
        }
    }
    return true;
}

function setDeliveryDateOptions() {
    const container = document.getElementById('deliveryDateOptions');
    if (!container) return;
    container.innerHTML = '';

    const today = new Date();
    const days = ['일', '월', '화', '수', '목', '금', '토'];
    const MAX_OPTIONS_TO_SHOW = 7;

    const setHiddenDate = (dateString) => {
        const hiddenInput = document.getElementById('wantDeliveryDate');
        if (hiddenInput) {
            hiddenInput.value = dateString;
            hiddenInput.dispatchEvent(new Event('change'));
        }
    };

    let generatedCount = 0;
    let daysToAdd = 0;

    while (generatedCount < MAX_OPTIONS_TO_SHOW) {
        const currentDay = new Date(today);
        currentDay.setDate(today.getDate() + daysToAdd);

        const dayOfWeek = currentDay.getDay();

        if (dayOfWeek !== 0) { // 일요일이 아니면 버튼 생성
            const dateString = `${currentDay.getFullYear()}-${String(currentDay.getMonth() + 1).padStart(2, '0')}-${String(currentDay.getDate()).padStart(2, '0')}`;
            const displayDay = days[dayOfWeek];
            const displayDate = `${currentDay.getMonth() + 1}/${currentDay.getDate()}`;

            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'date-option-button';
            button.setAttribute('data-date', dateString);

            let dayTextDisplay = displayDay;
            if (generatedCount === 0) {
                dayTextDisplay = '오늘';
            } else if (generatedCount === 1) {
                dayTextDisplay = '내일';
            }

            button.innerHTML = `<span class="day-of-week">${dayTextDisplay} (${displayDay})</span><span class="date-text">${displayDate}</span>`;

            button.addEventListener('click', () => {
                document.querySelectorAll('.date-option-button').forEach(btn => btn.classList.remove('selected'));
                button.classList.add('selected');
                setHiddenDate(dateString);
            });

            container.appendChild(button);
            generatedCount++;
        }

        daysToAdd++;
    }

    const firstButton = document.querySelector('.date-option-button');
    if (firstButton) {
        firstButton.click();
    }
}

function openPostcodeSearch() {
    if (typeof daum === 'undefined' || !daum.Postcode) {
        alert("Daum Postcode SDK가 로드되지 않았습니다. HTML 스크립트 태그를 확인해주세요.");
        return;
    }
    new daum.Postcode({
        oncomplete: function(data) {
            let addr = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress;
            document.getElementById('deliveryAddress').value = addr;
            document.getElementById('deliveryAddressDetail').focus();
        },
        width : '100%',
        height : '100%'
    }).open();
}


// =================================================================
// II. PAYMENT LOGIC (할인, 금액 계산, 결제 요청)
// =================================================================

function calculateFinalAmount() {
    if (!cartData) return;

    const totalItemPrice = cartData.selectedTotalPrice;

    const couponDiscount = Number(document.getElementById('couponSelect')?.value) || 0;
    let pointDiscount = Number(document.getElementById('pointDiscountAmount')?.value) || 0;

    pointDiscount = Math.min(pointDiscount, CURRENT_POINT);
    if (pointDiscount < 0) pointDiscount = 0;

    const orderItemsWithWrapInfo = collectOrderItems();
    const calculated = calculateFeesAndDiscounts(totalItemPrice, couponDiscount, pointDiscount, orderItemsWithWrapInfo);

    const finalPaymentAmount = calculated.finalAmount;

    document.getElementById('summaryTotalItemPrice').textContent = totalItemPrice.toLocaleString() + '원';
    document.getElementById('deliveryFee').textContent = calculated.deliveryFee.toLocaleString() + '원';
    document.getElementById('wrappingFee').textContent = calculated.wrappingFee.toLocaleString() + '원';
    document.getElementById('couponDiscount').textContent = '-' + couponDiscount.toLocaleString() + '원';
    document.getElementById('pointDiscount').textContent = '-' + pointDiscount.toLocaleString() + '원';

    const finalAmountText = Math.max(0, finalPaymentAmount).toLocaleString() + '원';
    document.getElementById('finalPaymentAmount').textContent = finalAmountText;
    document.getElementById('finalPaymentButtonText').textContent = finalAmountText + ' 결제하기';
}

async function handleTossPaymentRequest() {
    // 1. Order DTO 수집 및 유효성 검사
    const orderItems = collectOrderItems();
    const deliveryAddress = collectDeliveryAddress();

    if (!validateInputs(deliveryAddress, orderItems)) {
        return;
    }

    // 2. 금액 및 할인 정보 확보
    const couponDiscount = Number(document.getElementById('couponSelect')?.value) || 0;
    const pointDiscount = Number(document.getElementById('pointDiscountAmount')?.value) || 0;
    const totalItemPrice = cartData.selectedTotalPrice;

    // 3. 최종 금액 확인
    const calculatedFeeAndDiscount = calculateFeesAndDiscounts(totalItemPrice, couponDiscount, pointDiscount, orderItems);
    const finalAmount = calculatedFeeAndDiscount.finalAmount;

    if (finalAmount <= 0) {
        alert('결제 금액이 0원 이하입니다. 결제 없이 주문만 진행합니다.');
        return;
    }

    // 4. Mock OrderResponse 생성
    const orderResponse = {
        orderNumber: `TOSS-MOCK-${Date.now()}`,
        totalAmount: finalAmount
    };

    // 5. 주문명 생성
    let orderName = "주문 상품";
    if (cartData && cartData.items.length > 0) {
        const firstItem = cartData.items[0];
        orderName = cartData.items.length > 1
            ? `${firstItem.title.substring(0, firstItem.title.lastIndexOf('(')).trim()} 외 ${cartData.items.length - 1}건`
            : firstItem.title.substring(0, firstItem.title.lastIndexOf('(')).trim();
    }

    // 6. 결제 수단 확인
    const selectedMethod = document.querySelector('input[name="paymentMethod"]:checked')?.value || 'CARD';

    console.log("✅ Mock 주문 생성 완료. 서버 통신 건너뛰고 토스 V2 결제 요청 시작.");

    // 7. 토스 V2 결제 요청 (Toss SDK) 실행
    await requestTossPaymentV2(
        orderResponse.totalAmount,
        orderResponse.orderNumber,
        orderName,
        selectedMethod,
        deliveryAddress.recipient,
        'test@example.com' // 임시 이메일
    );
}

function calculateFeesAndDiscounts(totalItemPrice, couponDiscount, pointDiscount, orderItems) {
    let pointDiscountApplied = pointDiscount;
    if (pointDiscount > CURRENT_POINT) pointDiscountApplied = CURRENT_POINT;

    const wrappingFee = orderItems.reduce((sum, item) => {
        if (item.isWrapped) {
            return sum + (item.wrappingPaperPrice * item.quantity);
        }
        return sum;
    }, 0);

    const totalItemPriceAfterCoupon = totalItemPrice - couponDiscount;
    const deliveryFee = totalItemPriceAfterCoupon >= FREE_DELIVERY_THRESHOLD ? 0 : FIXED_DELIVERY_FEE;

    const totalDiscount = couponDiscount + pointDiscountApplied;
    const finalPaymentAmount = totalItemPrice + deliveryFee + wrappingFee - totalDiscount;

    return {
        deliveryFee: deliveryFee,
        wrappingFee: wrappingFee,
        finalAmount: Math.max(0, finalPaymentAmount)
    };
}

// [Toss Payment V2 Logic] 요청하신 V2 연쇄 호출 구조
async function requestTossPaymentV2(amount, orderId, orderName, method, customerName, customerEmail) {
    console.log("🚀 토스 V2 결제 요청 인자:", { amount, orderId, orderName, method, customerName, customerEmail });

    if (typeof window.TossPayments === 'undefined') {
        console.error("TossPayments SDK가 로드되지 않았습니다.");
        alert("결제 시스템 로드 실패. 잠시 후 다시 시도해 주세요.");
        return;
    }

    try {
        // 1. V2 TossPayments 인스턴스 생성
        const a = TossPayments(TOSS_CLIENT_KEY);

        // 2. payment 객체 생성 (고객키 사용)
        const customerKey = IS_USER ? String(USER_ID) : TossPayments.ANONYMOUS;
        const payment = a.payment({ customerKey });

        // 3. 결제 금액 객체 생성
        const amountObject = {
            currency: "KRW",
            value: amount,
        };

        // 4. requestPayment 호출 (연쇄 호출)
        await payment.requestPayment({
            method: method,
            amount: amountObject,
            orderId: orderId,
            orderName: orderName,
            successUrl: window.location.origin + API_BASE.TOSS_CONFIRM,
            failUrl: window.location.origin + "/fail.html",
            customerEmail: customerEmail,
            customerName: customerName,
            // 기타 V2 옵션 (필요시 주석 해제)
            // card: {
            //     useEscrow: false,
            //     flowMode: "DEFAULT",
            //     useCardPoint: false,
            //     useAppCardOnly: false,
            // },
        });

    } catch (error) {
        // 결제 요청 실패 처리
        console.error('토스 V2 결제 요청 실패:', error);
        alert('결제 요청 중 오류가 발생했습니다: ' + error.message);
    }
}