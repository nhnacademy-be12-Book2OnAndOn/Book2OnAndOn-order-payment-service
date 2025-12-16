const API_BASE = '/api/orders';
const USER_ID = 10;
const IS_MEMBER_LOGGED_IN = true;

// 🚨 [반영된 상태 코드] 백엔드 Enum과 일치
const ORDER_STATUS = {
    PENDING: "주문 대기",
    SHIPPING: "배송중",
    DELIVERED: "배송 완료",
    CANCELED: "주문 취소",
    COMPLETED: "주문 완료",
    RETURN_REQUESTED: "반품 신청",
    RETURN_COMPLETED: "반품 완료"
};

const RETURN_REASON = {
    CHANGE_OF_MIND: "단순 변심",
    PRODUCT_DEFECT: "상품 불량",
    WRONG_DELIVERY: "배송 문제",
    OTHER: "기타"
};

let currentOrderDetail = null; // 현재 보고 있는 주문 상세 정보를 저장
let memberOrders = []; // Mock 주문 목록을 저장할 변수 (정렬 기능용)

document.addEventListener('DOMContentLoaded', () => {
    initializeView();
    setupEventListeners();
    setupModalListeners();
});

// ----------------------------------------------------
// 초기화 및 UI 제어 함수
// ----------------------------------------------------

function initializeView() {
    const urlParams = new URLSearchParams(window.location.search);
    const orderId = urlParams.get('orderId');
    const mode = urlParams.get('mode');

    const forceGuestMode = mode === 'guest';
    const isLoggedIn = IS_MEMBER_LOGGED_IN && !forceGuestMode;

    if (orderId) {
        fetchOrderDetail(orderId,isLoggedIn ? 'MEMBER_MODE' : 'GUEST_MODE');
    } else if (isLoggedIn) {
        showMemberHistory();
        fetchMemberOrders(USER_ID);
    } else {
        showGuestLookupForm();
    }
}

function setupEventListeners() {
    // 1. 비회원 조회 폼 제출 이벤트
    document.getElementById('guestLookupForm')?.addEventListener('submit', handleGuestLookup);

    // 2. 목록으로 돌아가기 버튼
    document.getElementById('backToHistory')?.addEventListener('click', () => {
        if (IS_MEMBER_LOGGED_IN) {
            showMemberHistory();
        } else {
            showGuestLookupForm();
        }
    });

    // 3. 주문 목록 클릭 이벤트 (회원)
    document.getElementById('orderList')?.addEventListener('click', (e) => {
        const orderItem = e.target.closest('.order-item');
        if (orderItem) {
            const orderId = orderItem.dataset.orderId;
            fetchOrderDetail(orderId, 'MEMBER_MODE');
        }
    });

    // 정렬 옵션 변경 이벤트 리스너
    document.getElementById('sortOrderSelect')?.addEventListener('change', (e) => {
        sortOrdersAndRender(e.target.value);
    });
}

function setupModalListeners() {
    document.querySelector('#actionModal .close-button')?.addEventListener('click', hideModal);

    window.addEventListener('click', (e) => {
        if (e.target === document.getElementById('actionModal')) {
            hideModal();
        }
    });
}

function hideAllSections() {
    document.getElementById('guestLookupSection').classList.add('hidden');
    document.getElementById('memberHistorySection').classList.add('hidden');
    document.getElementById('orderDetailSection').classList.add('hidden');
}

function showGuestLookupForm() {
    hideAllSections();
    document.getElementById('guestLookupSection').classList.remove('hidden');
}

function showMemberHistory() {
    hideAllSections();
    document.getElementById('memberHistorySection').classList.remove('hidden');
}

function showOrderDetail() {
    hideAllSections();
    document.getElementById('orderDetailSection').classList.remove('hidden');
}

// ----------------------------------------------------
// 데이터 로드 및 렌더링 함수
// ----------------------------------------------------

async function fetchMemberOrders(userId) {
    // Mock Data with varying statuses
    memberOrders = [ // 전역 변수 memberOrders에 저장
        { orderId: 'M1001', date: '2025-12-10', total: 45000, status: ORDER_STATUS.PENDING, items: [{name: '클린 코드', count: 1}] },
        { orderId: 'M1002', date: '2025-11-20', total: 72000, status: ORDER_STATUS.DELIVERED, items: [{name: '객체지향 설계', count: 2}] },
        { orderId: 'M1003', date: '2025-11-01', total: 30000, status: ORDER_STATUS.SHIPPING, items: [{name: '알고리즘', count: 1}] },
        { orderId: 'M1004', date: '2025-10-25', total: 50000, status: ORDER_STATUS.RETURN_REQUESTED, items: [{name: '자바의 정석', count: 1}] },
        { orderId: 'M1005', date: '2025-10-20', total: 20000, status: ORDER_STATUS.CANCELED, items: [{name: '웹 개발', count: 1}] },
    ];

    // 초기 로드 시 최신순으로 정렬하여 렌더링
    sortOrdersAndRender('latest');
}

// 정렬 로직 및 렌더링 통합 함수
function sortOrdersAndRender(sortType) {
    if (!memberOrders || memberOrders.length === 0) {
        renderOrderList([]);
        return;
    }

    const sortedOrders = [...memberOrders].sort((a, b) => {
        const dateA = new Date(a.date);
        const dateB = new Date(b.date);

        if (sortType === 'latest') {
            // 최신순 (내림차순)
            return dateB - dateA;
        } else {
            // 과거순 (오름차순)
            return dateA - dateB;
        }
    });

    renderOrderList(sortedOrders);
}


function renderOrderList(orders) {
    const listContainer = document.getElementById('orderList');
    listContainer.innerHTML = '';

    if (orders.length === 0) {
        document.getElementById('noOrdersMessage').classList.remove('hidden');
        return;
    }
    document.getElementById('noOrdersMessage').classList.add('hidden');

    orders.forEach(order => {
        listContainer.innerHTML += `
            <div class="order-item" data-order-id="${order.orderId}">
                <div class="order-info">
                    <strong>주문 번호: ${order.orderId}</strong> (${order.date})<br>
                    총 금액: ${order.total.toLocaleString()}원 | 상태: <span style="font-weight: bold; color: ${order.status === ORDER_STATUS.DELIVERED ? 'green' : (order.status === ORDER_STATUS.CANCELED ? 'red' : (order.status === ORDER_STATUS.RETURN_REQUESTED ? 'orange' : '#333'))}">${order.status}</span><br>
                    상품: ${order.items[0].name} ${order.items.length > 1 ? `외 ${order.items.length - 1}건` : ''}
                </div>
                <button class="btn-primary" data-action="detail">상세 보기</button>
            </div>
        `;
    });
}

async function fetchOrderDetail(orderId, mode) {
    // Mock Detail Data
    const mockDetailMap = {
        'M1001': { id: 'M1001', date: '2025-12-10', total: 45000, status: ORDER_STATUS.PENDING, recipient: '홍길동', address: '서울시 강남구', items: [{ name: '클린 코드', quantity: 1, price: 45000, isWrapped: false }] },
        'M1002': { id: 'M1002', date: '2025-11-20', total: 72000, status: ORDER_STATUS.DELIVERED, recipient: '김철수', address: '경기도 성남시', items: [{ name: '객체지향 설계', quantity: 2, price: 36000, isWrapped: true, wrapName: '고급 포장' }] },
        'M1003': { id: 'M1003', date: '2025-11-01', total: 30000, status: ORDER_STATUS.SHIPPING, recipient: '이영희', address: '부산시 해운대구', items: [{ name: '알고리즘', quantity: 1, price: 30000, isWrapped: false }] },
        'M1004': { id: 'M1004', date: '2025-10-25', total: 50000, status: ORDER_STATUS.RETURN_REQUESTED, recipient: '박민준', address: '대구시 달서구', items: [{ name: '자바의 정석', quantity: 1, price: 50000, isWrapped: false }] },
        'M1005': { id: 'M1005', date: '2025-10-20', total: 20000, status: ORDER_STATUS.CANCELED, recipient: '최현우', address: '인천시 연수구', items: [{ name: '웹 개발', quantity: 1, price: 20000, isWrapped: false }] },
        'G1001': { id: 'G1001', date: '2025-12-15', total: 55000, status: ORDER_STATUS.DELIVERED, recipient: '비회원', address: '인천시 연수구', items: [{ name: '리액트 바이블', quantity: 1, price: 55000, isWrapped: true, wrapName: '에코 포장' }] },
    };

    const detail = mockDetailMap[orderId] || mockDetailMap['M1001'];
    currentOrderDetail = detail;

    renderOrderDetailContent(detail, mode);
    showOrderDetail();
}

function renderOrderDetailContent(detail, mode) {
    const detailContainer = document.getElementById('orderDetailContent');

    renderActionButtons(detail);

    detailContainer.innerHTML = `
        <h3>#${detail.id} 주문 상세 내역</h3>
        <p><strong>주문 일자:</strong> ${detail.date}</p>
        <p><strong>주문 상태:</strong> <span style="font-weight: bold; color: ${detail.status === ORDER_STATUS.DELIVERED ? 'green' : (detail.status === ORDER_STATUS.CANCELED ? 'red' : (detail.status === ORDER_STATUS.RETURN_REQUESTED ? 'orange' : '#333'))}">${detail.status}</span></p>
        
        <h4>배송 정보</h4>
        <p><strong>수령인:</strong> ${detail.recipient}</p>
        <p><strong>주소:</strong> ${detail.address}</p>
        
        <h4>상품 목록</h4>
        ${detail.items.map(item => `
            <div class="order-item-detail">
                ${item.name} (${item.quantity}권) - ${(item.price * item.quantity).toLocaleString()}원
                ${item.isWrapped ? ` (포장 옵션: ${item.wrapName})` : ''}
            </div>
        `).join('')}

        <h3 style="margin-top: 20px;">최종 결제 금액: ${detail.total.toLocaleString()}원</h3>
    `;

    const backButton = document.getElementById('backToHistory');
    if (mode === 'GUEST_MODE') {
        backButton.textContent = '다른 주문 조회하기';
    } else {
        backButton.textContent = '목록으로 돌아가기';
    }
}

// ----------------------------------------------------
// 액션 버튼 및 모달 제어 (상태별 로직)
// ----------------------------------------------------

function renderActionButtons(detail) {
    const buttonContainer = document.getElementById('actionButtons');
    buttonContainer.innerHTML = '';

    const status = detail.status;

    // 1. 주문 취소 버튼: 주문 대기(PENDING) 상태에서만 가능
    if (status === ORDER_STATUS.PENDING) {
        const cancelButton = document.createElement('button');
        cancelButton.className = 'btn-secondary';
        cancelButton.textContent = '주문 취소';
        cancelButton.style.marginRight = '10px';
        cancelButton.onclick = () => showModal('cancel', detail);
        buttonContainer.appendChild(cancelButton);
    }

    // 2. 반품 신청 버튼: 배송 완료(DELIVERED) 상태에서만 가능
    if (status === ORDER_STATUS.DELIVERED) {
        const returnButton = document.createElement('button');
        returnButton.className = 'btn-primary';
        returnButton.textContent = '반품 신청';
        returnButton.onclick = () => showModal('return', detail);
        buttonContainer.appendChild(returnButton);
    }
}

function hideModal() {
    document.getElementById('actionModal').classList.add('hidden');
}

function showModal(actionType, orderDetail) {
    const modal = document.getElementById('actionModal');
    const modalTitle = document.getElementById('modalTitle');
    const reasonGroup = document.getElementById('reasonGroup');
    const confirmButton = document.getElementById('confirmActionButton');

    modalTitle.textContent = actionType === 'cancel' ? '주문 취소 요청' : '상품 반품 요청';
    document.getElementById('modalOrderId').textContent = orderDetail.id;
    document.getElementById('modalAmount').textContent = orderDetail.total.toLocaleString();

    reasonGroup.innerHTML = '';

    if (actionType === 'cancel') {
        // 주문 취소 (cancelReason 필드에 들어갈 상세 사유)
        reasonGroup.innerHTML = `
            <label for="cancelReason">취소 상세 사유</label>
            <textarea id="cancelReason" rows="4" placeholder="취소 사유를 입력해주세요. (100자 이내)" maxlength="100" required></textarea>
        `;
        confirmButton.textContent = '주문 취소 처리';
    } else { //
        // 반품은 Enum 기반의 사유 선택
        reasonGroup.innerHTML = `
            <label for="returnReasonSelect">반품 사유 선택</label>
            <select id="returnReasonSelect" required>
                <option value="" disabled selected>-- 사유를 선택해주세요 --</option>
                ${Object.keys(RETURN_REASON).map(key =>
            `<option value="${key}">${RETURN_REASON[key]}</option>`
        ).join('')}
            </select>
            <div id="otherReasonInput" class="hidden" style="margin-top: 10px;">
                <label for="detailedReason">기타 상세 사유 (필수)</label>
                <textarea id="detailedReason" rows="2" placeholder="기타 사유의 상세 내용을 입력해주세요. (100자 이내)" maxlength="100"></textarea>
            </div>
            <p style="margin-top: 15px; font-size: 14px; color: #777;">* 반품 신청 시, 사유는 백엔드로 전송되며 회수 절차가 시작됩니다.</p>
        `;
        confirmButton.textContent = '반품 요청';

        // '기타' 선택 시 상세 입력 활성화 리스너
        document.getElementById('returnReasonSelect').addEventListener('change', (e) => {
            const isOther = e.target.value === 'OTHER';
            const detailedReasonElement = document.getElementById('detailedReason');

            document.getElementById('otherReasonInput').classList.toggle('hidden', !isOther);
            if (detailedReasonElement) {
                detailedReasonElement.required = isOther;
            }
        });
    }

    confirmButton.onclick = () => handleActionRequest(actionType, orderDetail);

    modal.classList.remove('hidden');
}

async function handleActionRequest(actionType, detail) {
    let reasonText = '';
    let reasonEnum = null;

    if (actionType === 'cancel') {
        reasonText = document.getElementById('cancelReason').value.trim();
        reasonEnum = 'ORDER_CANCELED'; // 백엔드 처리용 (Enum 필요시)
    } else { // 'return'
        reasonEnum = document.getElementById('returnReasonSelect').value;

        if (!reasonEnum) { alert('반품 사유를 선택해주세요.'); return; }

        if (reasonEnum === 'OTHER') {
            reasonText = document.getElementById('detailedReason').value.trim();
            if (!reasonText) { alert('기타 사유의 상세 내용을 입력해주세요.'); return; }
        } else {
            reasonText = RETURN_REASON[reasonEnum];
        }
    }

    if (!reasonText || reasonText.length > 100) {
        alert('사유는 1자 이상 100자 이내로 입력해주세요.');
        return;
    }

    const isCancel = actionType === 'cancel';

    const endpoint = isCancel
        ? `${API_BASE}/${detail.id}/cancel`
        : `${API_BASE}/${detail.id}/return`;

    const requestBody = {
        orderId: detail.id,
        paymentKey: 'MOCK_PAYMENT_KEY_1234',
        cancelAmount: detail.total,
        cancelReason: reasonText,
        ...(isCancel ? {} : { returnReason: reasonEnum })
    };

    console.log(`📡 ${isCancel ? '주문 취소' : '반품 요청'} API 호출:`, endpoint, requestBody);

    //  실제 API 호출 부분 (주석 처리)
    /*
    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            // headers: { ... },
            // body: JSON.stringify(requestBody)
        });
        // ... 성공/실패 처리 로직 ...
    } catch (e) {
        // ... 에러 처리 ...
    }
    */

    // Mock Success (테스트용)
    alert(`${isCancel ? '주문이 성공적으로 취소' : '반품 요청이 완료'}되었습니다. (Mock 성공)`);
    hideModal();
    // 갱신을 위해 목록으로 돌아가기
    fetchMemberOrders(USER_ID);
}

// ----------------------------------------------------
// 비회원 조회 핸들러 (Mock 유지)
// ----------------------------------------------------

async function handleGuestLookup(e) {
    e.preventDefault();
    const orderId = document.getElementById('guestOrderId').value;
    const orderer = document.getElementById('guestOrderer').value;
    const password = document.getElementById('guestPassword').value;

    if (orderId === 'G1001' && orderer === '비회원' && password === '1234') {
        alert('조회 성공! 주문 상세 페이지로 이동합니다.');
        fetchOrderDetail(orderId, 'GUEST_MODE');
    } else {
        alert('주문 정보가 일치하지 않습니다.');
    }
}