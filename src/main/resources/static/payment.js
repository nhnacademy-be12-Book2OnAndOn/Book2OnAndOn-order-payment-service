const PAYMENT_API_BASE = {
    ORDER: '/api/orders',
    TOSS_CONFIRM: '/payment/TOSS/confirm'
};

const USER_ID = 1;

const TOSS_CLIENT_KEY = "test_ck_YOUR_CLIENT_KEY"; // TODO: 실제 키로 교체 필요
const FIXED_DELIVERY_FEE = 3000;
const FREE_DELIVERY_THRESHOLD = 30000;

let partialOrderRequest = null;
let isUserOrder = false;
let currentPoint = 12500; // TODO: 유저 모듈에서 가져와야 함


// --- 1. 초기화 및 데이터 로드 ---
document.addEventListener('DOMContentLoaded', () => {
    // 1. localStorage에서 데이터 로드
    const partialData = localStorage.getItem('partialOrderRequest');
    isUserOrder = localStorage.getItem('isUserOrder') === 'true';

    if (!partialData) {
        alert('주문 정보가 유실되었습니다. 처음부터 다시 진행해주세요.');
        window.location.href = 'order.html';
        return;
    }
    partialOrderRequest = JSON.parse(partialData);
    // 2. 할인 계산 및 이벤트 설정
    setupDiscountCalculation();
    // 3. 결제 버튼 이벤트 설정
    document.getElementById('requestTossPayment').addEventListener('click', handleTossPaymentRequest);
});

function setupDiscountCalculation() {
    // 포인트/쿠폰 입력 필드에 변경 이벤트 리스너 추가
    document.getElementById('couponSelect').addEventListener('change', calculateFinalAmount);
    document.getElementById('pointDiscountAmount').addEventListener('input', calculateFinalAmount);

    calculateFinalAmount();
}

function calculateFinalAmount() {
    if (!partialOrderRequest) return;

    // 1. 기본 금액 정보
    const totalItemPrice = partialOrderRequest.totalItemPrice;

    // 2. 할인 금액 수집
    const couponDiscount = Number(document.getElementById('couponSelect').value) || 0;
    let pointDiscount = Number(document.getElementById('pointDiscountAmount').value) || 0;

    // 3. 포인트 유효성 검사 (보유 포인트 초과 사용 방지)
    pointDiscount = Math.min(pointDiscount, currentPoint);
    if (pointDiscount < 0) pointDiscount = 0;

    // 4. 포장비 계산 (JS에서 계산하는 것은 부정확, 백엔드 계산 필요하나 임시 구현)
    const wrappingFee = partialOrderRequest.orderItems.reduce((sum, item) => {
        if (item.isWrapped) {
            // TODO: order.js에서 wrappingPaperId를 통해 가격을 미리 저장해두거나, 서버 API를 호출해야 함
            // 여기서는 임시 포장비 2000원으로 가정
            return sum + 2000;
        }
        return sum;
    }, 0);

    // 5. 배송비 계산
    const totalItemPriceAfterDiscount = totalItemPrice - couponDiscount;
    const deliveryFee = totalItemPriceAfterDiscount >= FREE_DELIVERY_THRESHOLD ? 0 : FIXED_DELIVERY_FEE;

    // 6. 최종 금액
    const totalDiscount = couponDiscount + pointDiscount;
    const finalPaymentAmount = totalItemPrice + deliveryFee + wrappingFee - totalDiscount;

    // 7. 화면 업데이트
    document.getElementById('totalItemPrice').textContent = totalItemPrice.toLocaleString() + '원';
    document.getElementById('deliveryFee').textContent = deliveryFee.toLocaleString() + '원';
    document.getElementById('wrappingFee').textContent = wrappingFee.toLocaleString() + '원';
    document.getElementById('couponDiscount').textContent = '-' + couponDiscount.toLocaleString() + '원';
    document.getElementById('pointDiscount').textContent = '-' + pointDiscount.toLocaleString() + '원';

    const finalAmountText = Math.max(0, finalPaymentAmount).toLocaleString() + '원';
    document.getElementById('finalPaymentAmount').textContent = finalAmountText;
    document.getElementById('finalPaymentButtonText').textContent = finalAmountText + ' 결제하기';

    // 8. 최종 DTO에 저장될 할인 금액을 localStorage에 업데이트 (결제 요청 시 사용)
    partialOrderRequest.couponDiscountAmount = couponDiscount;
    partialOrderRequest.pointDiscountAmount = pointDiscount;

    partialOrderRequest.deliveryFee = deliveryFee;
    partialOrderRequest.wrappingFee = wrappingFee;
}

// --- 2. 결제 요청 핸들러 (Toss API 연동) ---

async function handleTossPaymentRequest() {

    // 1-1. 최종 금액 재계산 및 할인 DTO 업데이트 (calculateFinalAmount 함수가 실행되어 금액이 확정됨을 가정)
    // 💡 이 로직은 calculateFinalAmount 함수가 포인트/쿠폰 적용 후 호출해야 합니다.
    // calculateFinalAmount();

    // 1-2. 최종 결제 금액이 0원 이하인지 확인 (토스 API 호출 방지)
    const finalAmount =
        Math.max(0,
            partialOrderRequest.totalItemPrice +
            (partialOrderRequest.deliveryFee || 0) +
            (partialOrderRequest.wrappingFee || 0) -
            (partialOrderRequest.couponDiscountAmount + partialOrderRequest.pointDiscountAmount)
        );

    console.log('최종 결제 금액 계산 결과:', finalAmount);
    if (finalAmount <= 0) {
        alert('결제 금액이 0원 이하입니다. 결제 없이 주문만 진행합니다.');
        // TODO: 0원 주문은 별도의 API(예: POST /api/orders/zero-payment)를 호출해야 합니다.
        return;
    }

    // 2. OrderCreateRequestDto 완성 (서버로 보낼 최종 DTO 구성)
    // finalOrderRequest 객체는 order.js와 payment.js에서 수집된 모든 필드를 포함해야 합니다.
    const finalOrderRequest = {
        userId: isUserOrder ? USER_ID : null,
        orderItems: partialOrderRequest.orderItems,
        deliveryAddress: partialOrderRequest.deliveryAddress,
        couponDiscountAmount: partialOrderRequest.couponDiscountAmount,
        pointDiscountAmount: partialOrderRequest.pointDiscountAmount,
        // wantDeliveryDate 등 기타 필드도 추가
        // 비회원일 경우 guestName, guestPhonenumber, guestPassword 필드가 추가되어야 함
    };

    // 3. 주문 생성 API 호출 (DB에 주문 정보를 PENDING 상태로 임시 저장)
    let orderResponse;
    const orderEndpoint = isUserOrder ? PAYMENT_API_BASE.ORDER : PAYMENT_API_BASE.ORDER + '/guest';

    const headers = {
        'Content-Type': 'application/json',
    };

    if (isUserOrder) {
        // isUserOrder가 true일 때만 X-USER-ID 헤더를 추가합니다.
        headers['X-USER-ID'] = String(USER_ID);
    }

    try {
        const res = await fetch(orderEndpoint, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(finalOrderRequest)
        });

        if (!res.ok) {
            const errorBody = await res.json();
            alert('주문 생성 실패: ' + (errorBody.message || res.statusText));
            return;
        }
        orderResponse = await res.json();

        // 4. 주문 생성 성공 후, 토스 결제 요청 (Toss SDK) 실행
        requestTossPayment(orderResponse);

    } catch (e) {
        console.error('주문 생성 통신 오류:', e);
        alert('서버 통신 중 오류가 발생했습니다.');
    }
}

function requestTossPayment(orderResponse) {
    const tossPayments = TossPayments(TOSS_CLIENT_KEY);

    const finalAmount = orderResponse.totalAmount;
    const orderId = orderResponse.orderNumber;

    // 회원, 비회원 구분
    const customerKey = isUserOrder ? String(USER_ID) : TossPayments.ANONYMOUS;

    tossPayments.payment({
        method: "CARD", // 카드 결제로 가정
        amount: { currency: "KRW", value: finalAmount },
        orderId: orderId,
        orderName: "Book2OnAndOn 도서 외",
        customerKey: customerKey,
        successUrl: window.location.origin + PAYMENT_API_BASE.TOSS_CONFIRM, // 서버 콜백 주소
        failUrl: window.location.origin + "/fail.html",
        // ... 기타 정보 (customerName, customerEmail 등)
    }).catch(error => {
        console.error('토스 결제 요청 실패:', error);
        alert('결제 요청 중 오류가 발생했습니다: ' + error.message);
    });
}

window.calculateFinalAmount = calculateFinalAmount;
window.handleTossPaymentRequest = handleTossPaymentRequest;