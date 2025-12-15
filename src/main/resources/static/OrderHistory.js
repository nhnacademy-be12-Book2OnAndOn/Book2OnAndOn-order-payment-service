const API_BASE = '/orders'; // 실제 API URL로 변경해야 함.
const USER_ID = 10; //  로그인 모듈에서 받아와야 함. 현재는 Mock ID 사용
const IS_MEMBER_LOGGED_IN = true; // 실제 로그인 상태에 따라 변경되어야 함.

document.addEventListener('DOMContentLoaded', () => {
    // 초기 뷰 설정 (회원 vs 비회원)
    initializeView();
    setupEventListeners();
});

function initializeView() {
    // URL에 주문번호가 있으면 상세 뷰로 시작 (비회원 조회 성공 후 리다이렉트 시)
    const urlParams = new URLSearchParams(window.location.search);
    const orderId = urlParams.get('orderId');

    if (orderId) {
        // 비회원 조회 성공 후 리다이렉트된 경우 (실제 서버 통신 가정)
        fetchOrderDetail(orderId, 'GUEST_MODE');
    } else if (IS_MEMBER_LOGGED_IN) {
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
}

// ----------------------------------------------------
// UI 제어 함수
// ----------------------------------------------------

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

//  회원: 전체 주문 목록 조회
async function fetchMemberOrders(userId) {
    //  Mock Data for Member History
    const mockOrders = [
        { orderId: 'M1001', date: '2025-12-10', total: 45000, status: '배송 완료', items: [{name: '클린 코드', count: 1}] },
        { orderId: 'M1002', date: '2025-11-20', total: 72000, status: '배송 중', items: [{name: '객체지향 설계', count: 2}, {name: '알고리즘', count: 1}] }
    ];
    renderOrderList(mockOrders);

    //  실제 API 호출 예시:
    /*
    const response = await fetch(`${API_BASE}/user/${userId}`);
    if (response.ok) {
        const orders = await response.json();
        renderOrderList(orders);
    } else {
        document.getElementById('orderList').innerHTML = '<p>주문 내역을 불러오는 데 실패했습니다.</p>';
    }
    */
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
                <strong>주문 번호: ${order.orderId}</strong> (${order.date})<br>
                총 금액: ${order.total.toLocaleString()}원 | 상태: ${order.status}<br>
                상품: ${order.items[0].name} ${order.items.length > 1 ? `외 ${order.items.length - 1}건` : ''}
                <button class="btn-primary" style="float: right;">상세 보기</button>
            </div>
        `;
    });
}

// 회원/비회원: 주문 상세 내역 조회
async function fetchOrderDetail(orderId, mode) {
    //  Mock Data for Detail
    const mockDetail = {
        id: orderId,
        date: '2025-12-10',
        total: 45000,
        status: '배송 완료',
        recipient: '홍길동',
        address: '서울시 강남구 테헤란로',
        items: [
            { name: '클린 코드 (Clean Code) 기초편', quantity: 1, price: 30000, isWrapped: true, wrapName: '고급 선물 포장' },
            { name: '객체지향 설계와 원리 심화', quantity: 2, price: 15000, isWrapped: false }
        ]
    };
    renderOrderDetail(mockDetail, mode);
    showOrderDetail();

    //  실제 API 호출 예시: (mode에 따라 API 엔드포인트나 인증 방식이 달라집니다.)
    /*
    let url = `${API_BASE}/${orderId}`;
    if (mode === 'GUEST_MODE') {
        // 비회원 상세 조회는 보통 POST 요청으로 인증 정보를 다시 확인합니다.
        // 여기서는 이미 조회가 성공했다고 가정합니다.
    }
    const response = await fetch(url);
    // ...
    */
}

function renderOrderDetail(detail, mode) {
    const detailContainer = document.getElementById('orderDetailContent');
    detailContainer.innerHTML = `
        <h3>#${detail.id} 주문 상세 내역</h3>
        <p><strong>주문 일자:</strong> ${detail.date}</p>
        <p><strong>주문 상태:</strong> ${detail.status}</p>
        
        <h4>배송 정보</h4>
        <p><strong>수령인:</strong> ${detail.recipient}</p>
        <p><strong>주소:</strong> ${detail.address}</p>
        
        <h4>상품 목록</h4>
        ${detail.items.map(item => `
            <div class="order-item-detail">
                ${item.name} (${item.quantity}권) - ${item.price.toLocaleString()}원
                ${item.isWrapped ? ` (포장 옵션: ${item.wrapName})` : ''}
            </div>
        `).join('')}

        <h3 style="margin-top: 20px;">최종 결제 금액: ${detail.total.toLocaleString()}원</h3>
    `;

    // 비회원 모드일 경우 목록 돌아가기 버튼을 숨기거나 텍스트 변경
    const backButton = document.getElementById('backToHistory');
    if (mode === 'GUEST_MODE') {
        backButton.textContent = '다른 주문 조회하기';
    } else {
        backButton.textContent = '목록으로 돌아가기';
    }
}


// ----------------------------------------------------
// 비회원 조회 핸들러
// ----------------------------------------------------

async function handleGuestLookup(e) {
    e.preventDefault();
    const orderId = document.getElementById('guestOrderId').value;
    const orderer = document.getElementById('guestOrderer').value;
    const password = document.getElementById('guestPassword').value;

    // Mock Authentication
    if (orderId === 'G1001' && orderer === '김철수' && password === '1234') {
        alert('조회 성공! 주문 상세 페이지로 이동합니다.');
        // 실제라면 서버에 인증 요청 후, 성공하면 주문 상세 정보를 받아와 렌더링
        fetchOrderDetail(orderId, 'GUEST_MODE');
    } else {
        alert('주문 정보가 일치하지 않습니다.');
    }

    // 실제 API 호출 예시:
    /*
    try {
        const response = await fetch(`${API_BASE}/guest/lookup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ orderId, orderer, password })
        });

        if (response.ok) {
            const detail = await response.json();
            renderOrderDetail(detail, 'GUEST_MODE');
            showOrderDetail();
        } else {
            alert('주문 정보가 일치하지 않거나 오류가 발생했습니다.');
        }
    } catch (error) {
        alert('서버 통신 중 오류가 발생했습니다.');
    }
    */
}