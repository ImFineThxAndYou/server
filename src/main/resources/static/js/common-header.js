// 공통 헤더 컴포넌트
class CommonHeader {
    constructor() {
        this.init();
    }

    init() {
        this.createHeader();
        this.createNavigation();
    }

    createHeader() {
        const header = document.createElement('header');
        header.className = 'common-header';
        header.innerHTML = `
            <div class="header-container">
                <div class="header-left">
                    <a href="/" class="logo">🔧 HowAreYou</a>
                </div>
                <div class="header-center">
                    <nav class="main-nav">
                        <a href="/" class="nav-item">🏠 메인</a>
                        <a href="/notification-test.html" class="nav-item">🔔 알림 테스트</a>
                        <a href="/test-login.html" class="nav-item">🔐 로그인</a>
                        <a href="/test-signup.html" class="nav-item">📝 회원가입</a>
                        <a href="/test-info.html" class="nav-item">ℹ️ 사용자 정보</a>
                        <a href="/swagger-ui.html" class="nav-item" target="_blank">📚 API 문서</a>
                    </nav>
                </div>
                <div class="header-right">
                    <div class="auth-section">
                        <div id="auth-status" class="auth-status">로그인 필요</div>
                        <button id="login-btn" class="btn btn-primary" onclick="window.location.href='/test-login.html'">로그인</button>
                        <button id="logout-btn" class="btn btn-secondary" onclick="window.authManager.logout()" style="display: none;">로그아웃</button>
                    </div>
                </div>
            </div>
        `;

        // 페이지 시작 부분에 헤더 삽입
        document.body.insertBefore(header, document.body.firstChild);
    }

    createNavigation() {
        const nav = document.createElement('nav');
        nav.className = 'breadcrumb-nav';
        nav.innerHTML = `
            <div class="breadcrumb-container">
                <span class="breadcrumb-item">
                    <a href="/">🏠 메인</a>
                </span>
                <span class="breadcrumb-separator">/</span>
                <span class="breadcrumb-current" id="current-page">현재 페이지</span>
            </div>
        `;

        // 헤더 다음에 네비게이션 삽입
        const header = document.querySelector('.common-header');
        if (header) {
            header.parentNode.insertBefore(nav, header.nextSibling);
        }

        // 현재 페이지 이름 설정
        this.setCurrentPageName();
    }

    setCurrentPageName() {
        const currentPageElement = document.getElementById('current-page');
        if (!currentPageElement) return;

        const path = window.location.pathname;
        const pageNames = {
            '/': '메인 페이지',
            '/index.html': '메인 페이지',
            '/notification-test.html': '알림 시스템 테스트',
            '/test-login.html': '로그인 테스트',
            '/test-signup.html': '회원가입 테스트',
            '/test-info.html': '사용자 정보 테스트'
        };

        currentPageElement.textContent = pageNames[path] || '알 수 없는 페이지';
    }
}

// 공통 스타일 추가
const commonStyles = `
    .common-header {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        padding: 15px 0;
        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        position: sticky;
        top: 0;
        z-index: 1000;
    }

    .header-container {
        max-width: 1200px;
        margin: 0 auto;
        padding: 0 20px;
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    .header-left .logo {
        color: white;
        text-decoration: none;
        font-size: 1.5rem;
        font-weight: bold;
    }

    .main-nav {
        display: flex;
        gap: 20px;
    }

    .nav-item {
        color: white;
        text-decoration: none;
        padding: 8px 16px;
        border-radius: 6px;
        transition: background-color 0.3s ease;
    }

    .nav-item:hover {
        background-color: rgba(255,255,255,0.2);
    }

    .auth-section {
        display: flex;
        align-items: center;
        gap: 15px;
    }

    .auth-status {
        font-size: 0.9rem;
        padding: 5px 10px;
        border-radius: 4px;
        background-color: rgba(255,255,255,0.2);
    }

    .btn {
        padding: 8px 16px;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-size: 0.9rem;
        text-decoration: none;
        display: inline-block;
        transition: all 0.3s ease;
    }

    .btn-primary {
        background-color: #28a745;
        color: white;
    }

    .btn-primary:hover {
        background-color: #218838;
    }

    .btn-secondary {
        background-color: #dc3545;
        color: white;
    }

    .btn-secondary:hover {
        background-color: #c82333;
    }

    .breadcrumb-nav {
        background-color: #f8f9fa;
        padding: 10px 0;
        border-bottom: 1px solid #dee2e6;
    }

    .breadcrumb-container {
        max-width: 1200px;
        margin: 0 auto;
        padding: 0 20px;
        display: flex;
        align-items: center;
        gap: 10px;
    }

    .breadcrumb-item a {
        color: #007bff;
        text-decoration: none;
    }

    .breadcrumb-item a:hover {
        text-decoration: underline;
    }

    .breadcrumb-separator {
        color: #6c757d;
    }

    .breadcrumb-current {
        color: #495057;
        font-weight: 500;
    }

    /* 기존 컨테이너에 상단 여백 추가 */
    .container {
        margin-top: 20px;
    }

    @media (max-width: 768px) {
        .header-container {
            flex-direction: column;
            gap: 15px;
        }

        .main-nav {
            flex-wrap: wrap;
            justify-content: center;
        }

        .auth-section {
            flex-direction: column;
            gap: 10px;
        }
    }
`;

// 스타일 추가
const styleSheet = document.createElement('style');
styleSheet.textContent = commonStyles;
document.head.appendChild(styleSheet);

// 헤더 초기화
document.addEventListener('DOMContentLoaded', function() {
    new CommonHeader();
}); 