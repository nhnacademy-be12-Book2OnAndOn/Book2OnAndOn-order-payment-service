const API_BASE = '/orders';
const USER_ID = 10;
const IS_MEMBER_LOGGED_IN = true;

// 백엔드 Enum 및 UI 표시용 상태명
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

let currentOrderDetail = null;
let memberOrders = [];

document.addEventListener('DOMContentLoaded', () => {
    initializeView();
    setupEventListeners();
    setupModalListeners();
    initializeFilterForm();
});

// --- 초기화 및 UI 제어 ---
function initializeView() {
    const urlParams = new URLSearchParams(window.location.search);
    const orderId = urlParams.get('orderId');
    const mode = urlParams.get('mode');
    const isLoggedIn = IS_MEMBER_LOGGED_IN && mode !== 'guest';

    if (orderId) {
        fetchOrderDetail(orderId, isLoggedIn ? 'MEMBER_MODE' : 'GUEST_MODE');
    } else if (isLoggedIn) {
        showMemberHistory();
        fetchMemberOrders(USER_ID);
    } else {
        showGuestLookupForm();
    }
}

function setupEventListeners() {
    document.getElementById('guestLookupForm')?.addEventListener('submit', handleGuestLookup);
    document.getElementById('backToHistory')?.addEventListener('click', () => {
        IS_MEMBER_LOGGED_IN ? showMemberHistory() : showGuestLookupForm();
    });

    // 주문 목록 클릭 시 상세 보기
    document.getElementById('orderList')?.addEventListener('click', (e) => {
        const orderItem = e.target.closest('.order-item');
        if (orderItem) fetchOrderDetail(orderItem.dataset.orderId, 'MEMBER_MODE');
    });

    document.getElementById('sortOrderSelect')?.addEventListener('change', (e) => {
        const filtered = filterMockOrders(memberOrders, getCurrentFilters());
        sortOrdersAndRender(e.target.value, filtered);
    });

    document.getElementById('orderFilterForm')?.addEventListener('submit', handleOrderFiltering);
    document.getElementById('filterResetButton')?.addEventListener('click', initializeFilterForm);

    // 상세페이지 체크박스 금액 실시간 업데이트
    document.getElementById('orderDetailContent')?.addEventListener('change', (e) => {
        if (e.target.classList.contains('item-checkbox')) updateSelectedAmount();
    });
}

// --- 상태별 색상 반환 함수 ---
function getStatusColor(status) {
    switch (status) {
        case ORDER_STATUS.DELIVERED: return 'green';
        case ORDER_STATUS.CANCELED: return 'red';
        case ORDER_STATUS.RETURN_REQUESTED: return 'orange';
        case ORDER_STATUS.SHIPPING: return '#333';
        default: return '#333';
    }
}

/*
 * --- 주문 목록 렌더링 (이미지 스타일 복구) ---
 */
function renderOrderList(orders) {
    const container = document.getElementById('orderList');
    if (!container) return;
    container.innerHTML = orders.length ? '' : '<p id="noOrdersMessage">주문 내역이 없습니다.</p>';

    orders.forEach(o => {
        container.innerHTML += `
            <div class="order-item" data-order-id="${o.orderId}" style="cursor:pointer; border:1px solid #ddd; padding:15px; margin-bottom:10px; border-radius:8px;">
                <div class="order-info">
                    <strong>주문 번호: ${o.orderId}</strong> (${o.date})<br>
                    총 금액: ${o.total.toLocaleString()}원 | 상태: <span style="font-weight:bold; color:${getStatusColor(o.status)};">${o.status}</span><br>
                    상품: ${o.items[0].name} ${o.items.length > 1 ? `외 ${o.items.length - 1}건` : ''}
                </div>
                <button class="btn-primary" style="margin-top:10px;">상세 보기</button>
            </div>`;
    });
}

/*
 * --- 주문 상세 정보 렌더링 (이미지 내용 복구) ---
 */
async function fetchOrderDetail(orderId, mode) {
    const mockDetailMap = {
        'M1001': {
            id: 'M1001', date: '2025-12-10', total: 45000,
            status: ORDER_STATUS.PENDING, // 주문 대기
            recipient: '홍길동', address: '서울시 강남구',
            items: [{ id: 101, name: '클린 코드', quantity: 1, price: 45000, isWrapped: false }]
        },
        'M1002': {
            id: 'M1002', date: '2025-11-20', total: 72000,
            status: ORDER_STATUS.DELIVERED, // 배송 완료
            recipient: '김철수', address: '경기도 성남시',
            items: [{ id: 201, name: '객체지향 설계', quantity: 2, price: 36000, isWrapped: true, wrapName: '고급 포장' }]
        },
        'M1003': {
            id: 'M1003', date: '2025-11-01', total: 30000,
            status: ORDER_STATUS.SHIPPING, // 배송중
            recipient: '이영희', address: '부산시 해운대구',
            items: [{ id: 301, name: '알고리즘', quantity: 1, price: 30000, isWrapped: false }]
        },
        'M1004': {
            id: 'M1004', date: '2025-10-25', total: 50000,
            status: ORDER_STATUS.RETURN_REQUESTED, // 반품 신청
            recipient: '박민준', address: '대구시 달서구',
            items: [{ id: 401, name: '자바의 정석', quantity: 1, price: 50000, isWrapped: false }]
        },
        'M1005': {
            id: 'M1005', date: '2025-10-20', total: 20000,
            status: ORDER_STATUS.CANCELED, // 주문 취소
            recipient: '최현우', address: '인천시 연수구',
            items: [{ id: 501, name: '웹 개발', quantity: 1, price: 20000, isWrapped: false }]
        }
    };
    currentOrderDetail = mockDetailMap[orderId] || mockDetailMap['M1001'];

    const container = document.getElementById('orderDetailContent');

    //  기존 상단 버튼 영역은 비웁니다.
    const topActionContainer = document.getElementById('actionButtons');
    if (topActionContainer) topActionContainer.innerHTML = '';

    let itemsHtml = '';
    currentOrderDetail.items.forEach(item => {
        for (let i = 0; i < item.quantity; i++) {
            const isCheckable = [ORDER_STATUS.PENDING, ORDER_STATUS.SHIPPING, ORDER_STATUS.DELIVERED]
                .includes(currentOrderDetail.status);
            itemsHtml += `
                <div class="order-item-detail" style="display:flex; align-items:center; gap:10px; margin-bottom:10px; padding:10px; background:#fcfcfc; border-left:3px solid #ddd;">
                ${isCheckable ?
                `<input type="checkbox" class="item-checkbox" data-id="${item.id}" data-price="${item.price}" data-name="${item.name}">`
                : '' // 해당 상태가 아니면 빈 문자열로 처리하여 체크박스 제거
            }
                <span>${item.name} (1권) - ${item.price.toLocaleString()}원 ${item.isWrapped ? `(포장 옵션: ${item.wrapName})` : ''}</span>
            </div>`;
        }
    });

    container.innerHTML = `
        <h3 style="border-left:5px solid #4A7C59; padding-left:10px; color:#4A7C59;">#${currentOrderDetail.id} 주문 상세 내역</h3>
        <p><strong>주문 일자:</strong> ${currentOrderDetail.date}</p>
        <p><strong>주문 상태:</strong> <span style="font-weight:bold; color:${getStatusColor(currentOrderDetail.status)};">${currentOrderDetail.status}</span></p>
        
        <h4 style="margin-top:20px; border-bottom:1px dashed #eee; padding-bottom:5px;">배송 정보</h4>
        <p><strong>수령인:</strong> ${currentOrderDetail.recipient}</p>
        <p><strong>주소:</strong> ${currentOrderDetail.address}</p>
        
        <h4 style="margin-top:20px; border-bottom:1px dashed #eee; padding-bottom:5px;">상품 목록</h4>
        <div id="itemList">${itemsHtml}</div>
        
        <div style="margin-top:15px; padding:10px; background:#f9f9f9; border-radius:5px; text-align:right;">
            선택된 취소/반품 금액: <strong id="selectedAmount" style="color:red;">0</strong>원
        </div>

        <h3 style="margin-top:20px; text-align:right;">최종 결제 금액: ${currentOrderDetail.total.toLocaleString()}원</h3>
    `;

    // 하단에 버튼을 생성하는 함수를 호출합니다.
    renderBottomButtons(currentOrderDetail, mode);

    showOrderDetail();
}

function renderBottomButtons(detail, mode) {
    const section = document.getElementById('orderDetailSection');

    // 기존에 생성된 하단 버튼 컨테이너가 있다면 제거 (중복 방지)
    const oldBtnContainer = document.querySelector('.detail-bottom-actions');
    if (oldBtnContainer) oldBtnContainer.remove();

    // 새 버튼 컨테이너 생성
    const btnContainer = document.createElement('div');
    btnContainer.className = 'detail-bottom-actions';
    btnContainer.style.marginTop = '20px';
    btnContainer.style.display = 'flex';
    btnContainer.style.gap = '10px';

    // 1. 목록으로 돌아가기 버튼 (기존 HTML에 있는 버튼은 숨기고 새로 생성)
    const originalBackBtn = document.getElementById('backToHistory');
    if (originalBackBtn) originalBackBtn.classList.add('hidden');

    const newBackBtn = document.createElement('button');
    newBackBtn.className = 'btn-secondary';
    newBackBtn.textContent = mode === 'GUEST_MODE' ? '다른 주문 조회하기' : '목록으로 돌아가기';
    newBackBtn.onclick = () => {
        IS_MEMBER_LOGGED_IN ? showMemberHistory() : showGuestLookupForm();
    };
    btnContainer.appendChild(newBackBtn);

    // 2. 주문 상태에 따른 액션 버튼 추가 (오른쪽 배치)
    if (detail.status === ORDER_STATUS.PENDING) {
        const cancelBtn = document.createElement('button');
        cancelBtn.className = 'btn-primary';
        cancelBtn.style.backgroundColor = '#e74c3c'; // 취소 버튼 강조색
        cancelBtn.textContent = '선택 항목 취소';
        cancelBtn.onclick = () => showModal('cancel', detail);
        btnContainer.appendChild(cancelBtn);
    } else if (detail.status === ORDER_STATUS.DELIVERED || detail.status === ORDER_STATUS.SHIPPING) {
        const returnBtn = document.createElement('button');
        returnBtn.className = 'btn-primary';
        returnBtn.textContent = '선택 항목 반품';
        returnBtn.onclick = () => showModal('return', detail);
        btnContainer.appendChild(returnBtn);
    }

    // 상세 섹션의 가장 마지막에 버튼 바 추가
    section.appendChild(btnContainer);
}

function updateSelectedAmount() {
    let total = 0;
    document.querySelectorAll('.item-checkbox:checked').forEach(cb => {
        total += parseInt(cb.dataset.price);
    });
    const display = document.getElementById('selectedAmount');
    if (display) display.textContent = total.toLocaleString();
}

function renderActionButtons(detail) {
    const container = document.getElementById('actionButtons');
    if (!container) return;
    container.innerHTML = '';
    if (detail.status === ORDER_STATUS.PENDING) {
        container.innerHTML = '<button class="btn-secondary" id="btnCancel">선택 항목 취소</button>';
        document.getElementById('btnCancel').onclick = () => showModal('cancel', detail);
    } else if (detail.status === ORDER_STATUS.DELIVERED) {
        container.innerHTML = '<button class="btn-primary" id="btnReturn">선택 항목 반품</button>';
        document.getElementById('btnReturn').onclick = () => showModal('return', detail);
    }
}

/*
 * --- 모달 및 공통 기능 (기존 유지) ---
 */
function showModal(actionType, detail) {
    const selected = document.querySelectorAll('.item-checkbox:checked');
    if (selected.length === 0) return alert('항목을 선택해주세요.');

    const modal = document.getElementById('actionModal');
    const amount = Array.from(selected).reduce((sum, cb) => sum + parseInt(cb.dataset.price), 0);

    document.getElementById('modalTitle').textContent = actionType === 'cancel' ? '부분 취소' : '부분 반품';
    document.getElementById('modalOrderId').textContent = detail.id;
    document.getElementById('modalAmount').textContent = amount.toLocaleString();

    const reasonGroup = document.getElementById('reasonGroup');
    reasonGroup.innerHTML = '';
    if (actionType === 'cancel') {
        reasonGroup.innerHTML = '<label>취소 사유</label><textarea id="actionReason" style="width:100%;" rows="3"></textarea>';
    } else {
        let options = Object.entries(RETURN_REASON).map(([k, v]) => `<option value="${k}">${v}</option>`).join('');
        reasonGroup.innerHTML = `
            <label>반품 사유</label>
            <select id="returnReason" style="width:100%; margin-bottom:10px;">${options}</select>
            <textarea id="actionReason" style="width:100%;" placeholder="상세 사유"></textarea>`;
    }

    document.getElementById('confirmActionButton').onclick = () => handleActionRequest(actionType, detail, amount);
    modal.classList.remove('hidden');
}

async function handleActionRequest(type, detail, amount) {
    alert(`성공적으로 처리되었습니다.\n금액: ${amount.toLocaleString()}원`);
    hideModal();
    fetchMemberOrders(USER_ID);
    showMemberHistory();
}

function hideModal() { document.getElementById('actionModal').classList.add('hidden'); }
function setupModalListeners() {
    document.querySelector('#actionModal .close-button')?.addEventListener('click', hideModal);
}

/*
 * --- 주문 내역에 가이드 추가 ---
 */
// 모든 섹션 숨기기 함수 수정
function hideAllSections() {
    document.getElementById('guestLookupSection').classList.add('hidden');
    document.getElementById('memberHistorySection').classList.add('hidden');
    document.getElementById('orderDetailSection').classList.add('hidden');

    // 가이드 섹션도 일단 전체 숨김.
    const guideSection = document.querySelector('.order-guide-section');
    if (guideSection) guideSection.classList.add('hidden');
}

// 비회원 조회 폼 보여주기
function showGuestLookupForm() {
    hideAllSections();
    document.getElementById('guestLookupSection').classList.remove('hidden');

    // 가이드 표시
    const guideSection = document.querySelector('.order-guide-section');
    if (guideSection) guideSection.classList.remove('hidden');
}

// 회원 주문 목록 보여주기
function showMemberHistory() {
    hideAllSections();
    document.getElementById('memberHistorySection').classList.remove('hidden');

    // 가이드 표시
    const guideSection = document.querySelector('.order-guide-section');
    if (guideSection) guideSection.classList.remove('hidden');
}

// 주문 상세 정보 보여주기 (여기서는 가이드 표시 x)
function showOrderDetail() {
    hideAllSections();
    document.getElementById('orderDetailSection').classList.remove('hidden');

}


/*
 * --- 검색 및 정렬 기능 (기존 유지) ---
 */
function initializeFilterForm() {
    const currentYear = new Date().getFullYear();
    const filterYear = document.getElementById('filterYear');
    const filterMonth = document.getElementById('filterMonth');
    const filterStatus = document.getElementById('filterStatus');
    if(!filterYear || !filterMonth) return;
    filterYear.innerHTML = '<option value="all">전체보기</option>';
    for (let y = currentYear; y >= currentYear - 3; y--) filterYear.innerHTML += `<option value="${y}">${y}년</option>`;
    filterMonth.innerHTML = '<option value="all">전체보기</option>';
    for (let m = 1; m <= 12; m++) filterMonth.innerHTML += `<option value="${String(m).padStart(2, '0')}">${m}월</option>`;
    filterStatus.innerHTML = '<option value="all">전체보기</option>';
    Object.values(ORDER_STATUS).forEach(v => filterStatus.innerHTML += `<option value="${v}">${v}</option>`);
}

async function fetchMemberOrders(userId) {
    memberOrders = [
        { orderId: 'M1001', date: '2025-12-10', total: 45000, status: ORDER_STATUS.PENDING, items: [{name: '클린 코드'}] },
        { orderId: 'M1002', date: '2025-11-20', total: 72000, status: ORDER_STATUS.DELIVERED, items: [{name: '객체지향 설계'}] },
        { orderId: 'M1003', date: '2025-11-01', total: 30000, status: ORDER_STATUS.SHIPPING, items: [{name: '알고리즘'}] },
        { orderId: 'M1004', date: '2025-10-25', total: 50000, status: ORDER_STATUS.RETURN_REQUESTED, items: [{name: '자바의 정석'}] },
        { orderId: 'M1005', date: '2025-10-20', total: 20000, status: ORDER_STATUS.CANCELED, items: [{name: '웹 개발'}] }
    ];
    sortOrdersAndRender('latest', memberOrders);
}

function sortOrdersAndRender(sortType, orders) {
    const sorted = [...(orders || memberOrders)].sort((a, b) =>
        sortType === 'latest' ? new Date(b.date) - new Date(a.date) : new Date(a.date) - new Date(b.date));
    renderOrderList(sorted);
}

function handleOrderFiltering(e) { e.preventDefault(); sortOrdersAndRender('latest', filterMockOrders(memberOrders, getCurrentFilters())); }
function getCurrentFilters() { return { year: document.getElementById('filterYear').value, month: document.getElementById('filterMonth').value, status: document.getElementById('filterStatus').value, keyword: document.getElementById('searchKeyword').value.trim() }; }
function filterMockOrders(orders, f) {
    return orders.filter(o => {
        const d = new Date(o.date);
        if (f.year !== 'all' && f.year !== String(d.getFullYear())) return false;
        if (f.month !== 'all' && f.month !== String(d.getMonth() + 1).padStart(2, '0')) return false;
        if (f.status !== 'all' && f.status !== o.status) return false;
        return true;
    });
}
function handleGuestLookup(e) { e.preventDefault(); fetchOrderDetail('G1001', 'GUEST_MODE'); }