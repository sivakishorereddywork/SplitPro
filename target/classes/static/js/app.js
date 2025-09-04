/**
 * Split PRO - Main Application JavaScript
 * Handles SPA navigation, authentication UI, and core interactions
 */

/* =========================
   Fetch + CSRF utilities
   ========================= */
function getCookie(name) {
    return document.cookie
        .split('; ')
        .find(row => row.startsWith(name + '='))
        ?.split('=')[1];
}

async function apiFetch(url, options = {}) {
    const opts = { credentials: 'include', ...options };
    const method = (opts.method || 'GET').toUpperCase();

    // Ensure headers object
    opts.headers = new Headers(opts.headers || {});

    // JSON convenience
    if (opts.body && typeof opts.body === 'object' && !(opts.body instanceof FormData)) {
        opts.headers.set('Content-Type', 'application/json');
        opts.body = JSON.stringify(opts.body);
    }

    // Add CSRF header for state-changing requests
    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
        const token = getCookie('XSRF-TOKEN');
        if (token) {
            opts.headers.set('X-XSRF-TOKEN', decodeURIComponent(token));
        }
    }

    return fetch(url, opts);
}

class SplitProApp {
    constructor() {
        this.currentUser = null;
        this.currentSection = 'home';
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.checkAuthStatus();
        this.loadSection('home');
    }

    setupEventListeners() {
        // Navigation
        document.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const section = e.target.getAttribute('data-section');
                this.loadSection(section);
            });
        });

        // Footer navigation links
        document.querySelectorAll('.footer-section a[data-section]').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const section = e.target.getAttribute('data-section');
                this.loadSection(section);
            });
        });

        // Authentication buttons
        const loginBtn = document.getElementById('loginBtn');
        const signupBtn = document.getElementById('signupBtn');
        const logoutLink = document.getElementById('logoutLink');

        if (loginBtn) loginBtn.addEventListener('click', () => this.showAuthModal('login'));
        if (signupBtn) signupBtn.addEventListener('click', () => this.showAuthModal('signup'));
        if (logoutLink) logoutLink.addEventListener('click', (e) => {
            e.preventDefault();
            this.logout();
        });

        // Modal controls
        const closeAuthModal = document.getElementById('closeAuthModal');
        const closeSupportModal = document.getElementById('closeSupportModal');
        
        if (closeAuthModal) closeAuthModal.addEventListener('click', () => this.hideModal('authModal'));
        if (closeSupportModal) closeSupportModal.addEventListener('click', () => this.hideModal('supportModal'));

        // Close modals on background click
        document.querySelectorAll('.modal').forEach(modal => {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    this.hideModal(modal.id);
                }
            });
        });

        // Support form
        const supportBtn = document.getElementById('supportBtn');
        const supportForm = document.getElementById('supportForm');
        
        if (supportBtn) supportBtn.addEventListener('click', () => this.showModal('supportModal'));
        if (supportForm) supportForm.addEventListener('submit', (e) => this.handleSupportForm(e));

        // Quick actions
        this.setupQuickActions();

        // Mobile nav toggle
        const navToggle = document.getElementById('navToggle');
        const navMenu = document.getElementById('navMenu');
        
        if (navToggle && navMenu) {
            navToggle.addEventListener('click', () => {
                navMenu.classList.toggle('active');
            });
        }
    }

    setupQuickActions() {
        const quickActions = {
            'quickExpenseBtn': () => this.quickAddExpense(),
            'settleUpBtn': () => this.showSettleUpSuggestions(),
            'balancesBtn': () => this.showBalances(),
            'exportBtn': () => this.exportData(),
            'addFriendBtn': () => this.showAddFriendModal(),
            'manageFriendsBtn': () => this.loadSection('friends'),
            'createGroupBtn': () => this.showCreateGroupModal(),
            'manageGroupsBtn': () => this.loadSection('groups'),
            'importCsvBtn': () => this.showImportModal(),
            'exportCsvBtn': () => this.exportData()
        };

        Object.entries(quickActions).forEach(([id, handler]) => {
            const element = document.getElementById(id);
            if (element) {
                element.addEventListener('click', handler);
            }
        });
    }

    loadSection(sectionName) {
        // Update URL without page reload
        history.pushState({ section: sectionName }, '', `#${sectionName}`);
        
        // Hide all sections
        document.querySelectorAll('.section').forEach(section => {
            section.classList.remove('active');
        });

        // Show target section
        const targetSection = document.getElementById(sectionName);
        if (targetSection) {
            targetSection.classList.add('active');
        }

        // Update nav links
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('data-section') === sectionName) {
                link.classList.add('active');
            }
        });

        this.currentSection = sectionName;

        // Load section-specific data
        this.loadSectionData(sectionName);
    }

    async loadSectionData(sectionName) {
        if (!this.currentUser) return;

        this.showLoading();

        try {
            switch (sectionName) {
                case 'friends':
                    await this.loadFriends();
                    break;
                case 'groups':
                    await this.loadGroups();
                    break;
                case 'history':
                    await this.loadHistory();
                    break;
            }
        } catch (error) {
            this.showError('Failed to load section data: ' + error.message);
        } finally {
            this.hideLoading();
        }
    }

    async checkAuthStatus() {
        try {
            const response = await apiFetch('/api/auth/me'); // GET -> no CSRF header required
            if (response.ok) {
                this.currentUser = await response.json();
                this.updateUIForLoggedInUser();
            } else {
                this.updateUIForLoggedOutUser();
            }
        } catch (error) {
            console.error('Auth check failed:', error);
            this.updateUIForLoggedOutUser();
        }
    }

    showAuthModal(type = 'login') {
        const modal = document.getElementById('authModal');
        const title = document.getElementById('authModalTitle');
        const formsContainer = document.getElementById('authForms');

        title.textContent = type === 'login' ? 'Sign In' : 'Sign Up';

        const formHtml = type === 'login' ? this.getLoginFormHtml() : this.getSignupFormHtml();
        formsContainer.innerHTML = formHtml;

        this.setupAuthFormHandlers(type);
        this.showModal('authModal');
    }

    getLoginFormHtml() {
        return `
            <form id="loginForm" novalidate>
                <div class="form-group">
                    <label for="loginIdentifier">Email or Phone</label>
                    <input type="text" id="loginIdentifier" name="identifier" required>
                    <div class="form-error" id="loginIdentifierError"></div>
                </div>
                <div class="form-group">
                    <label for="loginPassword">Password</label>
                    <input type="password" id="loginPassword" name="password" required>
                    <div class="form-error" id="loginPasswordError"></div>
                </div>
                <div class="form-group">
                    <button type="submit" class="btn btn-primary" style="width: 100%;">Sign In</button>
                </div>
                <div class="form-group text-center">
                    <p>Don't have an account? <a href="#" id="switchToSignup">Sign up</a></p>
                    <p><a href="#" id="forgotPassword">Forgot password?</a></p>
                </div>
            </form>
        `;
    }

    getSignupFormHtml() {
        return `
            <form id="signupForm" novalidate>
                <div class="form-group">
                    <label for="signupName">Full Name</label>
                    <input type="text" id="signupName" name="name" required>
                    <div class="form-error" id="signupNameError"></div>
                </div>
                <div class="form-group">
                    <label for="signupEmail">Email</label>
                    <input type="email" id="signupEmail" name="email">
                    <div class="form-error" id="signupEmailError"></div>
                </div>
                <div class="form-group">
                    <label for="signupPhone">Phone</label>
                    <input type="tel" id="signupPhone" name="phone">
                    <div class="form-error" id="signupPhoneError"></div>
                </div>
                <div class="form-group">
                    <label for="signupPassword">Password</label>
                    <input type="password" id="signupPassword" name="password" required>
                    <small>At least 7 characters with uppercase, number, and special character</small>
                    <div class="form-error" id="signupPasswordError"></div>
                </div>
                <div class="form-group">
                    <label for="signupPasswordConfirm">Confirm Password</label>
                    <input type="password" id="signupPasswordConfirm" name="passwordConfirm" required>
                    <div class="form-error" id="signupPasswordConfirmError"></div>
                </div>
                <div class="form-group">
                    <button type="submit" class="btn btn-primary" style="width: 100%;">Sign Up</button>
                </div>
                <div class="form-group text-center">
                    <p>Already have an account? <a href="#" id="switchToLogin">Sign in</a></p>
                </div>
            </form>
        `;
    }

    setupAuthFormHandlers(type) {
        const form = document.getElementById(type + 'Form');
        if (form) {
            form.addEventListener('submit', (e) => this.handleAuthForm(e, type));
        }

        // Switch between forms
        const switchToSignup = document.getElementById('switchToSignup');
        const switchToLogin = document.getElementById('switchToLogin');
        const forgotPassword = document.getElementById('forgotPassword');

        if (switchToSignup) switchToSignup.addEventListener('click', (e) => {
            e.preventDefault();
            this.showAuthModal('signup');
        });

        if (switchToLogin) switchToLogin.addEventListener('click', (e) => {
            e.preventDefault();
            this.showAuthModal('login');
        });

        if (forgotPassword) forgotPassword.addEventListener('click', (e) => {
            e.preventDefault();
            this.handleForgotPassword();
        });

        // Real-time validation
        this.setupFormValidation(form);
    }

    setupFormValidation(form) {
        const inputs = form.querySelectorAll('input');
        inputs.forEach(input => {
            input.addEventListener('blur', () => this.validateField(input));
            input.addEventListener('input', () => this.clearFieldError(input));
        });
    }

    validateField(input) {
        const value = input.value.trim();
        const fieldName = input.name;
        let isValid = true;
        let errorMessage = '';

        switch (fieldName) {
            case 'name':
                if (!value) {
                    isValid = false;
                    errorMessage = 'Name is required';
                }
                break;
            case 'email':
                if (value && !this.isValidEmail(value)) {
                    isValid = false;
                    errorMessage = 'Please enter a valid email address';
                }
                break;
            case 'phone':
                if (value && !this.isValidPhone(value)) {
                    isValid = false;
                    errorMessage = 'Please enter a valid phone number';
                }
                break;
            case 'password':
                if (!this.isValidPassword(value)) {
                    isValid = false;
                    errorMessage = 'Password must be at least 7 characters with uppercase, number, and special character';
                }
                break;
            case 'passwordConfirm':
                const passwordField = document.getElementById('signupPassword');
                if (passwordField && value !== passwordField.value) {
                    isValid = false;
                    errorMessage = 'Passwords do not match';
                }
                break;
            case 'identifier':
                if (!value) {
                    isValid = false;
                    errorMessage = 'Email or phone is required';
                }
                break;
        }

        const errorElement = document.getElementById(input.id + 'Error');
        if (errorElement) {
            errorElement.textContent = errorMessage;
            input.classList.toggle('error', !isValid);
        }

        return isValid;
    }

    clearFieldError(input) {
        const errorElement = document.getElementById(input.id + 'Error');
        if (errorElement) {
            errorElement.textContent = '';
            input.classList.remove('error');
        }
    }

    async handleAuthForm(e, type) {
        e.preventDefault();
        
        const formData = new FormData(e.target);
        const data = Object.fromEntries(formData.entries());

        // Validate all fields
        const inputs = e.target.querySelectorAll('input');
        let isValid = true;
        inputs.forEach(input => {
            if (!this.validateField(input)) {
                isValid = false;
            }
        });

        // Additional validation for signup
        if (type === 'signup') {
            if (!data.email && !data.phone) {
                isValid = false;
                this.showError('Either email or phone is required');
                return;
            }
        }

        if (!isValid) return;

        this.showLoading();

        try {
            const response = await apiFetch(`/api/auth/${type}`, {
                method: 'POST',
                body: data
            });

            if (response.ok) {
                this.currentUser = await response.json();
                this.updateUIForLoggedInUser();
                this.hideModal('authModal');
                this.showSuccess(`Successfully ${type === 'login' ? 'signed in' : 'signed up'}!`);
            } else {
                const error = await response.json().catch(() => ({}));
                this.showError(error.message || `${type} failed`);
            }
        } catch (error) {
            this.showError(`${type} failed: ` + error.message);
        } finally {
            this.hideLoading();
        }
    }

    async logout() {
        this.showLoading();

        try {
            const response = await apiFetch('/api/auth/logout', {
                method: 'POST'
            });

            if (response.ok) {
                this.currentUser = null;
                this.updateUIForLoggedOutUser();
                this.loadSection('home');
                this.showSuccess('Successfully signed out!');
            } else {
                this.showError('Logout failed');
            }
        } catch (error) {
            this.showError('Logout failed: ' + error.message);
        } finally {
            this.hideLoading();
        }
    }

    updateUIForLoggedInUser() {
        const authButtons = document.getElementById('authButtons');
        const userDropdown = document.getElementById('userDropdown');
        const userInitials = document.getElementById('userInitials');

        if (authButtons) authButtons.style.display = 'none';
        if (userDropdown) userDropdown.style.display = 'block';
        
        if (userInitials && this.currentUser && this.currentUser.name) {
            const initials = this.currentUser.name
                .split(' ')
                .map(name => name.charAt(0))
                .join('')
                .toUpperCase();
            userInitials.textContent = initials;
        }
    }

    updateUIForLoggedOutUser() {
        const authButtons = document.getElementById('authButtons');
        const userDropdown = document.getElementById('userDropdown');

        if (authButtons) authButtons.style.display = 'flex';
        if (userDropdown) userDropdown.style.display = 'none';
    }

    async handleSupportForm(e) {
        e.preventDefault();
        
        const formData = new FormData(e.target);
        const data = Object.fromEntries(formData.entries());

        this.showLoading();

        try {
            const response = await apiFetch('/api/support', {
                method: 'POST',
                body: data
            });

            if (response.ok) {
                this.hideModal('supportModal');
                this.showSuccess('Support message sent successfully!');
                e.target.reset();
            } else {
                const error = await response.json().catch(() => ({}));
                this.showError(error.message || 'Failed to send message');
            }
        } catch (error) {
            this.showError('Failed to send message: ' + error.message);
        } finally {
            this.hideLoading();
        }
    }

    async handleForgotPassword() {
        const email = prompt('Please enter your email address:');
        if (!email || !this.isValidEmail(email)) {
            this.showError('Please enter a valid email address');
            return;
        }

        this.showLoading();

        try {
            const response = await apiFetch('/api/auth/forgot-password', {
                method: 'POST',
                body: { email }
            });

            if (response.ok) {
                this.showSuccess('Password reset instructions sent to your email!');
            } else {
                const error = await response.json().catch(() => ({}));
                this.showError(error.message || 'Failed to send reset email');
            }
        } catch (error) {
            this.showError('Failed to send reset email: ' + error.message);
        } finally {
            this.hideLoading();
        }
    }

    // Quick Actions Handlers
    quickAddExpense() {
        if (!this.currentUser) {
            this.showAuthModal('login');
            return;
        }
        // TODO: Implement quick expense modal
        this.showInfo('Quick expense feature coming in next step!');
    }

    showSettleUpSuggestions() {
        if (!this.currentUser) {
            this.showAuthModal('login');
            return;
        }
        // TODO: Implement settle up suggestions
        this.showInfo('Settle up suggestions feature coming soon!');
    }

    showBalances() {
        if (!this.currentUser) {
            this.showAuthModal('login');
            return;
        }
        // TODO: Implement balances view
        this.showInfo('Balances view feature coming soon!');
    }

    exportData() {
        if (!this.currentUser) {
            this.showAuthModal('login');
            return;
        }
        // TODO: Implement data export
        this.showInfo('Data export feature coming soon!');
    }

    showAddFriendModal() {
        if (!this.currentUser) {
            this.showAuthModal('login');
            return;
        }
        
        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h2>Add Friend</h2>
                    <button class="modal-close" onclick="this.closest('.modal').remove()">&times;</button>
                </div>
                <div class="modal-body">
                    <form id="addFriendForm">
                        <div class="form-group">
                            <label for="friendIdentifier">Friend's Email or Phone</label>
                            <input type="text" id="friendIdentifier" name="identifier" required>
                            <div class="form-error" id="friendIdentifierError"></div>
                        </div>
                        <button type="submit" class="btn btn-primary">Add Friend</button>
                    </form>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        modal.style.display = 'flex';
        
        document.getElementById('addFriendForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(e.target);
            const identifier = formData.get('identifier');
            
            try {
                const response = await apiFetch('/api/friends', {
                    method: 'POST',
                    body: { identifier }
                });
                
                if (response.ok) {
                    modal.remove();
                    this.showSuccess('Friend added successfully!');
                    if (this.currentSection === 'friends') {
                        this.loadFriends();
                    }
                } else {
                    const error = await response.json().catch(() => ({}));
                    this.showError(error.message || 'Failed to add friend');
                }
            } catch (error) {
                this.showError('Failed to add friend: ' + error.message);
            }
        });
    }

    showCreateGroupModal() {
        if (!this.currentUser) {
            this.showAuthModal('login');
            return;
        }
        // TODO: Implement create group modal
        this.showInfo('Create group feature coming soon!');
    }

    showImportModal() {
        if (!this.currentUser) {
            this.showAuthModal('login');
            return;
        }
        // TODO: Implement CSV import
        this.showInfo('CSV import feature coming soon!');
    }

    // Data Loading Methods - UPDATED VERSIONS
    async loadFriends() {
        try {
            const response = await apiFetch('/api/friends'); // GET
            if (response.ok) {
                const friends = await response.json();
                const friendsList = document.getElementById('friendsList');
                
                if (friends.length === 0) {
                    friendsList.innerHTML = '<p class="empty-state">No friends added yet. Add your first friend to get started!</p>';
                } else {
                    friendsList.innerHTML = friends.map(friend => `
                        <div class="friend-card">
                            <div class="friend-info">
                                <h3>${friend.friendName}</h3>
                                <p>${friend.friendEmail}</p>
                            </div>
                            <div class="friend-balance ${friend.balance >= 0 ? 'positive' : 'negative'}">
                                ${friend.balance >= 0 ? 'Owes you' : 'You owe'} $${Math.abs(friend.balance).toFixed(2)}
                            </div>
                            <button class="btn btn-outline" onclick="window.app.removeFriend('${friend.friendId}')">Remove</button>
                        </div>
                    `).join('');
                }
            } else {
                this.showError('Failed to load friends');
            }
        } catch (error) {
            this.showError('Failed to load friends: ' + error.message);
        }
    }

    async loadGroups() {
        try {
            const response = await apiFetch('/api/groups'); // GET
            if (response.ok) {
                const groups = await response.json();
                const groupsList = document.getElementById('groupsList');
                
                if (groups.length === 0) {
                    groupsList.innerHTML = '<p class="empty-state">No groups created yet. Create your first group to start splitting expenses!</p>';
                } else {
                    groupsList.innerHTML = groups.map(group => `
                        <div class="group-card">
                            <div class="group-info">
                                <h3>${group.name}</h3>
                                <p>${group.description || 'No description'}</p>
                                <small>Created by ${group.createdByName} • ${group.members.length} members</small>
                            </div>
                            <div class="group-stats">
                                <span>${group.totalExpenses} expenses</span>
                                <span>$${group.totalAmount.toFixed(2)} total</span>
                            </div>
                        </div>
                    `).join('');
                }
            } else {
                this.showError('Failed to load groups');
            }
        } catch (error) {
            this.showError('Failed to load groups: ' + error.message);
        }
    }

    async loadHistory() {
        try {
            const response = await apiFetch('/api/expenses?page=0&size=20'); // GET
            if (response.ok) {
                const result = await response.json();
                const historyList = document.getElementById('historyList');
                
                if (!result.content || result.content.length === 0) {
                    historyList.innerHTML = '<p class="empty-state">No transactions yet. Add an expense to see your history!</p>';
                } else {
                    historyList.innerHTML = result.content.map(expense => `
                        <div class="history-item">
                            <div class="expense-info">
                                <h4>${expense.description}</h4>
                                <p>Paid by ${expense.payerName} • $${expense.totalAmount.toFixed(2)}</p>
                                <small>${new Date(expense.createdAt).toLocaleDateString()}</small>
                            </div>
                            <div class="expense-category">
                                ${expense.category}
                            </div>
                        </div>
                    `).join('');
                }
            } else {
                this.showError('Failed to load transaction history');
            }
        } catch (error) {
            this.showError('Failed to load history: ' + error.message);
        }
    }

    // Utility Methods
    showModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'flex';
            document.body.style.overflow = 'hidden';
        }
    }

    hideModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'none';
            document.body.style.overflow = '';
        }
    }

    showLoading() {
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) overlay.style.display = 'flex';
    }

    hideLoading() {
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) overlay.style.display = 'none';
    }

    showSuccess(message) {
        this.showNotification(message, 'success');
    }

    showError(message) {
        this.showNotification(message, 'error');
    }

    showInfo(message) {
        this.showNotification(message, 'info');
    }

    showNotification(message, type = 'info') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <span>${message}</span>
            <button class="notification-close">&times;</button>
        `;

        // Add styles
        notification.style.cssText = `
            position: fixed;
            top: 80px;
            right: 20px;
            background: ${type === 'success' ? 'var(--success-color)' : 
                         type === 'error' ? 'var(--danger-color)' : 
                         'var(--info-color)'};
            color: white;
            padding: 1rem 1.5rem;
            border-radius: var(--border-radius);
            box-shadow: var(--shadow-lg);
            z-index: 4000;
            display: flex;
            align-items: center;
            gap: 1rem;
            max-width: 400px;
            animation: slideInRight 0.3s ease;
        `;

        // Add close functionality
        const closeBtn = notification.querySelector('.notification-close');
        closeBtn.style.cssText = `
            background: none;
            border: none;
            color: white;
            font-size: 1.2rem;
            cursor: pointer;
            padding: 0;
            margin-left: 0.5rem;
        `;
        
        closeBtn.addEventListener('click', () => {
            notification.remove();
        });

        // Add to DOM
        document.body.appendChild(notification);

        // Auto remove after 5 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 5000);

        // Add animation styles if not already added
        if (!document.getElementById('notification-styles')) {
            const styles = document.createElement('style');
            styles.id = 'notification-styles';
            styles.textContent = `
                @keyframes slideInRight {
                    from {
                        transform: translateX(100%);
                        opacity: 0;
                    }
                    to {
                        transform: translateX(0);
                        opacity: 1;
                    }
                }
            `;
            document.head.appendChild(styles);
        }
    }

    async removeFriend(friendId) {
        if (!confirm('Are you sure you want to remove this friend?')) {
            return;
        }

        try {
            const response = await apiFetch(`/api/friends/${friendId}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                this.showSuccess('Friend removed successfully!');
                this.loadFriends();
            } else {
                this.showError('Failed to remove friend');
            }
        } catch (error) {
            this.showError('Failed to remove friend: ' + error.message);
        }
    }

    // Validation Helpers
    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    isValidPhone(phone) {
        // Basic phone validation - adjust regex as needed
        const phoneRegex = /^\+?[\d\s\-\(\)]{10,}$/;
        return phoneRegex.test(phone.replace(/\s/g, ''));
    }

    isValidPassword(password) {
        // At least 7 characters with uppercase, number, and special character
        const minLength = password.length >= 7;
        const hasUpper = /[A-Z]/.test(password);
        const hasNumber = /\d/.test(password);
        const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(password);
        return minLength && hasUpper && hasNumber && hasSpecial;
    }
}

// Handle browser back/forward buttons
window.addEventListener('popstate', (e) => {
    const section = e.state?.section || location.hash.slice(1) || 'home';
    if (window.app) {
        window.app.loadSection(section);
    }
});

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.app = new SplitProApp();
    
    // Handle initial hash
    const hash = location.hash.slice(1);
    if (hash && ['home', 'friends', 'groups', 'history'].includes(hash)) {
        window.app.loadSection(hash);
    }
});

// Export for potential use in other modules
export { SplitProApp };
