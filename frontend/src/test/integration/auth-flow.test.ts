/**
 * Auth store integration tests.
 *
 * Validates the full authentication flow as used by both the web
 * frontend and mirrored by the Android client: login → token storage →
 * user load → logout → token removal.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useAuthStore } from '@/stores/auth.store';
import type { AuthResponse, User } from '@/types';

const mockUser: User = { id: 1, email: 'test@test.com', name: 'Test User', role: 'customer' };
const mockAuthResp: AuthResponse = {
  access_token: 'tok-123',
  token_type: 'bearer',
  user: mockUser,
};

vi.mock('@/services/auth.service', () => ({
  login: vi.fn(),
  register: vi.fn(),
  fetchCurrentUser: vi.fn(),
}));

import * as authService from '@/services/auth.service';

const mockedLogin = vi.mocked(authService.login);
const mockedRegister = vi.mocked(authService.register);
const mockedFetchUser = vi.mocked(authService.fetchCurrentUser);

describe('Auth Store Integration Flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    useAuthStore.setState({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
    });
  });

  it('login stores token and sets authenticated state', async () => {
    mockedLogin.mockResolvedValueOnce(mockAuthResp);
    const store = useAuthStore.getState();
    await store.login({ email: 'test@test.com', password: 'pass' });

    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(true);
    expect(state.token).toBe('tok-123');
    expect(state.user).toEqual(mockUser);
    expect(localStorage.getItem('access_token')).toBe('tok-123');
  });

  it('login failure keeps unauthenticated state', async () => {
    mockedLogin.mockRejectedValueOnce(new Error('401'));
    const store = useAuthStore.getState();

    await expect(store.login({ email: 'bad@test.com', password: 'x' })).rejects.toThrow();

    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(false);
    expect(state.token).toBeNull();
    expect(state.isLoading).toBe(false);
  });

  it('register stores token and authenticates user', async () => {
    mockedRegister.mockResolvedValueOnce(mockAuthResp);
    const store = useAuthStore.getState();
    await store.register({ email: 'new@test.com', password: 'pass', name: 'New' });

    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(true);
    expect(state.token).toBe('tok-123');
    expect(localStorage.getItem('access_token')).toBe('tok-123');
  });

  it('loadUser populates user from stored token', async () => {
    localStorage.setItem('access_token', 'existing-token');
    useAuthStore.setState({ token: 'existing-token', isAuthenticated: true });
    mockedFetchUser.mockResolvedValueOnce(mockUser);

    await useAuthStore.getState().loadUser();

    const state = useAuthStore.getState();
    expect(state.user).toEqual(mockUser);
    expect(state.isAuthenticated).toBe(true);
    expect(state.isLoading).toBe(false);
  });

  it('loadUser with expired token clears auth state', async () => {
    localStorage.setItem('access_token', 'expired-token');
    useAuthStore.setState({ token: 'expired-token', isAuthenticated: true });
    mockedFetchUser.mockRejectedValueOnce(new Error('401'));

    await useAuthStore.getState().loadUser();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.token).toBeNull();
    expect(localStorage.getItem('access_token')).toBeNull();
  });

  it('logout clears all auth state and token', async () => {
    mockedLogin.mockResolvedValueOnce(mockAuthResp);
    await useAuthStore.getState().login({ email: 'test@test.com', password: 'pass' });
    expect(useAuthStore.getState().isAuthenticated).toBe(true);

    useAuthStore.getState().logout();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.token).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(localStorage.getItem('access_token')).toBeNull();
  });

  it('full lifecycle: register → loadUser → logout → login → loadUser', async () => {
    // Register
    mockedRegister.mockResolvedValueOnce(mockAuthResp);
    await useAuthStore.getState().register({ email: 'full@test.com', password: 'pass', name: 'Full' });
    expect(useAuthStore.getState().isAuthenticated).toBe(true);

    // Load user
    mockedFetchUser.mockResolvedValueOnce(mockUser);
    await useAuthStore.getState().loadUser();
    expect(useAuthStore.getState().user?.email).toBe('test@test.com');

    // Logout
    useAuthStore.getState().logout();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);

    // Login again
    mockedLogin.mockResolvedValueOnce(mockAuthResp);
    await useAuthStore.getState().login({ email: 'test@test.com', password: 'pass' });
    expect(useAuthStore.getState().isAuthenticated).toBe(true);

    // Load user again
    mockedFetchUser.mockResolvedValueOnce(mockUser);
    await useAuthStore.getState().loadUser();
    expect(useAuthStore.getState().user).toEqual(mockUser);
  });
});
