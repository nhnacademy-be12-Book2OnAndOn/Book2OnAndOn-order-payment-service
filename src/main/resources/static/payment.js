/* payment.js */

// 1. 결제 정보 설정 (실제로는 서버나 이전 페이지에서 받아와야 함)
const amount = {
    currency: "KRW",
    value: 20000,
};
const orderName = "토스 티셔츠 외 2건";

// 랜덤 주문번호 생성 (테스트용)
function generateRandomString() {
    return window.btoa(Math.random()).slice(0, 20);
}
const orderId = generateRandomString();

// 화면에 주문번호 표시
document.getElementById("orderIdDisplay").textContent = orderId;
document.getElementById("orderNameDisplay").textContent = orderName;

// 2. Toss Payments SDK 초기화
const clientKey = "test_ck_Z1aOwX7K8m1x1vJ2AgDQ8yQxzvNP"; // 테스트용 클라이언트 키
const customerKey = generateRandomString(); // 실제로는 로그인한 사용자 ID 해시값 사용 권장
const tossPayments = TossPayments(clientKey);
const payment = tossPayments.payment({ customerKey });

// 3. 결제 수단 선택 로직
let selectedPaymentMethod = "CARD"; // 기본값

function selectPaymentMethod(method) {
    // 기존 선택 해제
    const buttons = document.querySelectorAll('.method-btn');
    buttons.forEach(btn => btn.classList.remove('selected'));

    // 선택된 버튼 강조
    // 클릭된 요소가 button인지 내부 span인지 확인하여 처리
    const targetBtn = event.currentTarget;
    targetBtn.classList.add('selected');

    selectedPaymentMethod = method;
    console.log("선택된 결제 수단:", selectedPaymentMethod);
}

// 4. 결제 요청 함수
async function requestPayment() {
    console.log(`결제 요청 시작 - 수단: ${selectedPaymentMethod}, 주문번호: ${orderId}`);

    // 공통 파라미터
    const paymentData = {
        amount,
        orderId: orderId,
        orderName: orderName,
        customerEmail: "customer@example.com",
        customerName: "김토스",
        // 성공 시 백엔드 PaymentController로 이동 (대소문자 주의: TOSS)
        successUrl: window.location.origin + "/payment/TOSS/confirm",
        failUrl: window.location.origin + "/fail.html",
    };

    try {
        switch (selectedPaymentMethod) {
            case "CARD":
                await payment.requestPayment({
                    method: "CARD",
                    ...paymentData,
                    card: {
                        useEscrow: false,
                        flowMode: "DEFAULT",
                        useCardPoint: false,
                        useAppCardOnly: false,
                    },
                });
                break;

            case "TRANSFER":
                await payment.requestPayment({
                    method: "TRANSFER",
                    ...paymentData,
                    transfer: {
                        cashReceipt: { type: "소득공제" },
                        useEscrow: false,
                    },
                });
                break;

            case "VIRTUAL_ACCOUNT":
                await payment.requestPayment({
                    method: "VIRTUAL_ACCOUNT",
                    ...paymentData,
                    virtualAccount: {
                        cashReceipt: { type: "소득공제" },
                        useEscrow: false,
                        validHours: 24,
                    },
                });
                break;

            case "MOBILE_PHONE":
                await payment.requestPayment({
                    method: "MOBILE_PHONE",
                    ...paymentData,
                });
                break;

            case "CULTURE_GIFT_CERTIFICATE":
                await payment.requestPayment({
                    method: "CULTURE_GIFT_CERTIFICATE",
                    ...paymentData,
                });
                break;

            case "FOREIGN_EASY_PAY":
                // 해외 결제는 통화가 다를 수 있으므로 별도 처리 필요시 수정
                await payment.requestPayment({
                    method: "FOREIGN_EASY_PAY",
                    ...paymentData,
                    amount: { value: 100, currency: "USD" }, // 예시
                    foreignEasyPay: {
                        provider: "PAYPAL",
                        country: "KR",
                    },
                });
                break;
        }
    } catch (error) {
        console.error("결제 요청 중 에러 발생:", error);
        alert("결제 요청 중 오류가 발생했습니다.");
    }
}

// (선택) 정기 결제 요청
async function requestBillingAuth() {
    await payment.requestBillingAuth({
        method: "CARD",
        successUrl: window.location.origin + "/payment/billing/success",
        failUrl: window.location.origin + "/fail.html",
        customerEmail: "customer@example.com",
        customerName: "김토스",
    });
}