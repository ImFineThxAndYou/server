// 공통 인증 관리 JavaScript
class AuthManager {
    constructor() {
        this.currentUser = null;
        this.token = null;
        this.init();
    }

    init() {
        // 페이지 로드 시 저장된 정보 복원
        this.loadAuthInfo();
        this.updateUI();
    }

    // 로그인 정보 저장
    saveAuthInfo(user, token) {
        this.currentUser = user;
        this.token = token;
        localStorage.setItem('howareyou_user', JSON.stringify(user));
        localStorage.setItem('howareyou_token', token);
        this.updateUI();
    }

    // 로그인 정보 로드
    loadAuthInfo() {
        const savedUser = localStorage.getItem('howareyou_user');
        const savedToken = localStorage.getItem('howareyou_token');
        
        if (savedUser && savedToken) {
            this.currentUser = JSON.parse(savedUser);
            this.token = savedToken;
            return true;
        }
        return false;
    }

    // 로그인 정보 삭제
    clearAuthInfo() {
        this.currentUser = null;
        this.token = null;
        localStorage.removeItem('howareyou_user');
        localStorage.removeItem('howareyou_token');
        this.updateUI();
    }

    // 로그인 상태 확인
    isLoggedIn() {
        return this.currentUser !== null && this.token !== null;
    }

    // 현재 사용자 정보 반환
    getCurrentUser() {
        return this.currentUser;
    }

    // 토큰 반환
    getToken() {
        return this.token;
    }

    // API 요청 헤더 생성
    getAuthHeaders() {
        const headers = {
            'Content-Type': 'application/json',
        };
        
        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }
        
        return headers;
    }

    // 인증된 fetch 요청 래퍼
    async authenticatedFetch(url, options = {}) {
        const headers = {
            ...this.getAuthHeaders(),
            ...options.headers
        };

        const response = await fetch(url, {
            ...options,
            headers,
            credentials: 'include' // 쿠키 포함
        });

        // 401 에러 시 토큰 갱신 시도
        if (response.status === 401) {
            const refreshed = await this.refreshToken();
            if (refreshed) {
                // 토큰 갱신 성공 시 재시도
                const retryHeaders = {
                    ...this.getAuthHeaders(),
                    ...options.headers
                };
                return await fetch(url, {
                    ...options,
                    headers: retryHeaders,
                    credentials: 'include'
                });
            }
        }

        return response;
    }

    // 토큰 갱신
    async refreshToken() {
        try {
            const response = await fetch('/api/auth/refresh', {
                method: 'POST',
                credentials: 'include'
            });

            if (response.ok) {
                const newToken = response.headers.get('Authorization')?.replace('Bearer ', '');
                if (newToken) {
                    this.token = newToken;
                    localStorage.setItem('howareyou_token', newToken);
                    return true;
                }
            }
        } catch (error) {
            console.error('토큰 갱신 실패:', error);
        }

        // 토큰 갱신 실패 시 로그아웃
        this.clearAuthInfo();
        return false;
    }

    // UI 업데이트
    updateUI() {
        const authStatus = document.getElementById('auth-status');
        const userInfo = document.getElementById('user-info');
        const loginBtn = document.getElementById('login-btn');
        const logoutBtn = document.getElementById('logout-btn');

        if (this.isLoggedIn()) {
            if (authStatus) authStatus.textContent = '로그인됨';
            if (userInfo) {
                userInfo.innerHTML = `
                    <strong>사용자:</strong> ${this.currentUser.membername || this.currentUser.email}<br>
                    <strong>이름:</strong> ${this.currentUser.nickname || '미설정'}<br>
                    <strong>ID:</strong> ${this.currentUser.id}
                `;
            }
            if (loginBtn) loginBtn.style.display = 'none';
            if (logoutBtn) logoutBtn.style.display = 'inline-block';
        } else {
            if (authStatus) authStatus.textContent = '로그인 필요';
            if (userInfo) userInfo.innerHTML = '<em>로그인이 필요합니다.</em>';
            if (loginBtn) loginBtn.style.display = 'inline-block';
            if (logoutBtn) logoutBtn.style.display = 'none';
        }
    }

    // 로그아웃
    logout() {
        this.clearAuthInfo();
        // 로그인 페이지로 리다이렉트
        window.location.href = '/test-login.html';
    }

    // 토큰 유효성 검사
    async validateToken() {
        if (!this.token) return false;

        try {
            const response = await fetch('/api/members/me', {
                headers: this.getAuthHeaders()
            });
            
            if (response.ok) {
                const userData = await response.json();
                this.currentUser = userData;
                this.saveAuthInfo(userData, this.token);
                return true;
            } else {
                this.clearAuthInfo();
                return false;
            }
        } catch (error) {
            console.error('토큰 검증 실패:', error);
            this.clearAuthInfo();
            return false;
        }
    }
}

// 전역 인스턴스 생성
window.authManager = new AuthManager();

// 페이지 로드 시 토큰 검증
document.addEventListener('DOMContentLoaded', async function() {
    if (window.authManager.isLoggedIn()) {
        const isValid = await window.authManager.validateToken();
        if (!isValid) {
            alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
            window.authManager.logout();
        }
    }
}); 