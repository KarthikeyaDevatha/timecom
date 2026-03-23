/**
 * Session Tracker Frontend - Application Logic
 * Vanilla JS, Fetch API, JWT Management
 */

const API_BASE_URL = 'http://localhost:8080/api';

// --- State Management ---
const AppState = {
    token: localStorage.getItem('jwt_token') || null,
    user: JSON.parse(localStorage.getItem('user_data')) || null,
    
    setAuth(response) {
        this.token = response.token;
        this.user = {
            username: response.username,
            role: response.role,
            sessionId: response.sessionId
        };
        localStorage.setItem('jwt_token', this.token);
        localStorage.setItem('user_data', JSON.stringify(this.user));
    },

    clearAuth() {
        this.token = null;
        this.user = null;
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('user_data');
    },

    isAuthenticated() {
        return !!this.token;
    },
    
    isAdmin() {
        return this.user && this.user.role === 'ADMIN';
    }
};

// --- API Client ---
async function apiFetch(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (AppState.isAuthenticated()) {
        headers['Authorization'] = `Bearer ${AppState.token}`;
    }

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            ...options,
            headers
        });

        // Handle 401 Unauthorized (Expired token or forced logout)
        if (response.status === 401) {
            AppState.clearAuth();
            router.navigate('#login');
            showToast('Session expired. Please log in again.', 'error');
            throw new Error('Unauthorized');
        }

        // Handle 202 Accepted (No Content body)
        if (response.status === 202 || response.status === 204) {
             return null;
        }

        const data = await response.json().catch(() => null);
        
        if (!response.ok) {
            throw new Error(data?.message || data?.error || 'API Request Failed');
        }
        
        return data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// --- UI Utilities ---
function $(selector) { return document.querySelector(selector); }
function $$(selector) { return document.querySelectorAll(selector); }

function showView(viewId) {
    $$('.view-section').forEach(el => el.classList.add('hidden'));
    $(`#${viewId}`).classList.remove('hidden');
    
    if (AppState.isAuthenticated()) {
        $('#navbar').classList.remove('hidden');
        $('#nav-user-info').textContent = `${AppState.user.username} (${AppState.user.role})`;
    } else {
        $('#navbar').classList.add('hidden');
    }
}

function showToast(message, type = 'success') {
    const container = $('#toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <span>${message}</span>
        <button style="background:transparent;border:none;color:#fff;cursor:pointer" onclick="this.parentElement.remove()">✕</button>
    `;
    container.appendChild(toast);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
        toast.style.animation = 'fadeOut 0.3s ease forwards';
        setTimeout(() => toast.remove(), 300);
    }, 5000);
}

// --- Routing ---
const router = {
    init() {
        window.addEventListener('hashchange', () => this.handleRoute());
        this.handleRoute();
    },
    navigate(hash) {
        window.location.hash = hash;
    },
    handleRoute() {
        const hash = window.location.hash || '#dashboard';
        
        if (!AppState.isAuthenticated() && hash !== '#login' && hash !== '#register') {
            this.navigate('#login');
            return;
        }

        switch(hash) {
            case '#login':
            case '#register':
                if (AppState.isAuthenticated()) {
                    this.navigate('#dashboard');
                    return;
                }
                showView('view-auth');
                if (hash === '#register') {
                    $('#form-login').classList.add('hidden');
                    $('#form-register').classList.remove('hidden');
                } else {
                    $('#form-register').classList.add('hidden');
                    $('#form-login').classList.remove('hidden');
                }
                break;
            case '#dashboard':
                if (AppState.isAdmin()) {
                    this.navigate('#admin');
                    return;
                }
                showView('view-dashboard');
                Views.Dashboard.load();
                break;
            case '#admin':
                if (!AppState.isAdmin()) {
                    this.navigate('#dashboard');
                    return;
                }
                showView('view-admin');
                Views.Admin.load();
                break;
            default:
                this.navigate('#dashboard');
        }
    }
};

// --- View Controllers ---
const Views = {
    Auth: {
        init() {
            // Switch forms
            $('#link-to-register').addEventListener('click', (e) => { e.preventDefault(); router.navigate('#register'); });
            $('#link-to-login').addEventListener('click', (e) => { e.preventDefault(); router.navigate('#login'); });

            // Login Submit
            $('#form-login').addEventListener('submit', async (e) => {
                e.preventDefault();
                const btn = e.target.querySelector('button');
                btn.disabled = true;
                btn.textContent = 'Authenticating...';
                $('#login-error').classList.add('hidden');

                try {
                    const req = {
                        username: $('#login-username').value,
                        password: $('#login-password').value
                    };
                    const res = await apiFetch('/auth/login', { method: 'POST', body: JSON.stringify(req) });
                    AppState.setAuth(res);
                    showToast(`Welcome back, ${res.username}!`);
                    router.navigate(AppState.isAdmin() ? '#admin' : '#dashboard');
                } catch (err) {
                    $('#login-error').textContent = err.message;
                    $('#login-error').classList.remove('hidden');
                } finally {
                    btn.disabled = false;
                    btn.textContent = 'Sign In';
                }
            });

            // Register Submit
            $('#form-register').addEventListener('submit', async (e) => {
                e.preventDefault();
                const btn = e.target.querySelector('button');
                btn.disabled = true;
                $('#reg-error').classList.add('hidden');

                try {
                    const req = {
                        username: $('#reg-username').value,
                        email: $('#reg-email').value,
                        fullName: $('#reg-fullname').value,
                        password: $('#reg-password').value
                    };
                    const res = await apiFetch('/auth/register', { method: 'POST', body: JSON.stringify(req) });
                    AppState.setAuth(res);
                    showToast('Registration successful!');
                    router.navigate('#dashboard');
                } catch (err) {
                    $('#reg-error').textContent = err.message;
                    $('#reg-error').classList.remove('hidden');
                } finally {
                    btn.disabled = false;
                }
            });

            // Logout
            $('#btn-logout').addEventListener('click', async () => {
                try {
                    await apiFetch('/auth/logout', { method: 'POST' });
                } catch (e) { console.error('Logout error UI', e); }
                AppState.clearAuth();
                router.navigate('#login');
                showToast('Logged out successfully');
            });
        }
    },

    Dashboard: {
        init() {
            // Activity Triggers
            $$('.btn-action').forEach(btn => {
                btn.addEventListener('click', async (e) => {
                    const target = e.currentTarget;
                    const originalHTML = target.innerHTML;
                    target.innerHTML = `<span class="icon">⏳</span> Processing...`;
                    target.disabled = true;

                    try {
                        const payload = {
                            actionType: target.dataset.action,
                            resourcePath: target.dataset.path,
                            resourceId: "demo-" + Math.floor(Math.random() * 1000)
                        };
                        await apiFetch('/track', { method: 'POST', body: JSON.stringify(payload) });
                        showToast(`Event tracked: ${payload.actionType}`, 'success');
                        
                        // Optionally refresh sessions to update last activity
                        this.loadSessions();
                    } catch (err) {
                        showToast(err.message, 'error');
                    } finally {
                        target.innerHTML = originalHTML;
                        target.disabled = false;
                    }
                });
            });

            $('#btn-refresh-sessions').addEventListener('click', () => this.loadSessions());
        },

        load() {
            $('#welcome-text').textContent = `Welcome, ${AppState.user.username}`;
            this.loadSessions();
        },

        async loadSessions() {
            const list = $('#sessions-list');
            try {
                const sessions = await apiFetch('/sessions');
                
                if (sessions.length === 0) {
                    list.innerHTML = `<p class="text-muted">No active sessions found.</p>`;
                    return;
                }

                list.innerHTML = sessions.map(s => {
                    const isCurrent = s.id === AppState.user.sessionId;
                    return `
                    <div class="session-item ${isCurrent ? 'current' : ''}">
                        <div class="session-info">
                            <div class="session-ip">
                                <span class="status-dot ${s.active ? 'active' : ''}"></span>
                                ${s.deviceType} - ${s.ipAddress}
                                ${isCurrent ? '<span class="badge" style="margin-left:8px">Current Session</span>' : ''}
                            </div>
                            <div class="session-meta">
                                Created: ${new Date(s.createdAt).toLocaleString()} <br>
                                Last Active: ${s.lastActivityAt ? new Date(s.lastActivityAt).toLocaleString() : 'N/A'}
                            </div>
                        </div>
                        ${!isCurrent ? `<button class="btn btn-danger-outline btn-sm" onclick="Views.Dashboard.terminateSession(${s.id})">Terminate</button>` : ''}
                    </div>
                    `;
                }).join('');
            } catch (err) {
                if(err.message !== 'Unauthorized') showToast('Failed to load sessions', 'error');
            }
        },

        async terminateSession(sessionId) {
            if(!confirm('Terminate this session? The device will be logged out.')) return;
            try {
                await apiFetch(`/sessions/${sessionId}`, { method: 'DELETE' });
                showToast('Session terminated');
                this.loadSessions();
            } catch (err) {
                showToast(err.message, 'error');
            }
        }
    },

    Admin: {
        init() {
            $('#btn-refresh-admin').addEventListener('click', () => this.load());
        },

        async load() {
            this.renderSkeletons();
            try {
                const [analytics, sessions] = await Promise.all([
                    apiFetch('/admin/analytics/summary'),
                    apiFetch('/admin/sessions')
                ]);

                this.renderKPIs(analytics);
                this.renderSessions(sessions);
            } catch (err) {
                if(err.message !== 'Unauthorized') showToast('Failed to load admin data', 'error');
            }
        },

        renderSkeletons() {
            $('#admin-kpis').innerHTML = `
                <div class="glass-panel kpi-card skeleton-card"></div>
                <div class="glass-panel kpi-card skeleton-card"></div>
                <div class="glass-panel kpi-card skeleton-card"></div>
                <div class="glass-panel kpi-card skeleton-card"></div>
            `;
            $('#admin-sessions-table').innerHTML = `<tr><td colspan="6" class="text-center">Loading live data...</td></tr>`;
        },

        renderKPIs(data) {
            $('#admin-kpis').innerHTML = `
                <div class="glass-panel kpi-card" style="border-top: 3px solid var(--primary)">
                    <div class="kpi-label">Active Sessions</div>
                    <div class="kpi-value">${data.totalActiveSessions || 0}</div>
                </div>
                <div class="glass-panel kpi-card" style="border-top: 3px solid var(--secondary)">
                    <div class="kpi-label">Unique Users</div>
                    <div class="kpi-value">${data.totalActiveUsers || 0}</div>
                </div>
                <div class="glass-panel kpi-card" style="border-top: 3px solid var(--success)">
                    <div class="kpi-label">Events Today</div>
                    <div class="kpi-value">${data.totalActivitiesToday || 0}</div>
                </div>
                <div class="glass-panel kpi-card" style="border-top: 3px solid var(--warning)">
                    <div class="kpi-label">Events This Week</div>
                    <div class="kpi-value">${data.totalActivitiesThisWeek || 0}</div>
                </div>
            `;
        },

        renderSessions(sessions) {
            const tbody = $('#admin-sessions-table');
            if (!sessions || sessions.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">No active sessions found across the platform.</td></tr>`;
                return;
            }

            tbody.innerHTML = sessions.map(s => `
                <tr>
                    <td>#${s.id}</td>
                    <td><strong>${s.username}</strong></td>
                    <td><span class="status-dot ${s.active ? 'active' : ''}"></span> ${s.ipAddress} (${s.deviceType})</td>
                    <td>${new Date(s.createdAt).toLocaleString()}</td>
                    <td>${s.lastActivityAt ? new Date(s.lastActivityAt).toLocaleString() : 'N/A'}</td>
                    <td>
                        <button class="btn btn-danger-outline btn-sm" onclick="Views.Admin.terminateUserSession(${s.id})">Terminate</button>
                    </td>
                </tr>
            `).join('');
        },

        async terminateUserSession(sessionId) {
            if(!confirm(`Terminate session #${sessionId}?`)) return;
            try {
                await apiFetch(`/admin/sessions/${sessionId}`, { method: 'DELETE' });
                showToast('Session terminated globally', 'success');
                this.load();
            } catch (err) {
                showToast(err.message, 'error');
            }
        }
    }
};

// --- App Initialization ---
document.addEventListener('DOMContentLoaded', () => {
    Views.Auth.init();
    Views.Dashboard.init();
    Views.Admin.init();
    router.init();
});
