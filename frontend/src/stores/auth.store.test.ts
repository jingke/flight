import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useAuthStore } from './auth.store';

vi.mock('@/services/auth.service', () => ({
  login: vi.fn(),
  register: vi.fn(),
  fetchCurrentUser: vi.fn(),
}));

import * as authService from '@/services/auth.service';

const mockLogin = vi.mocked(authService.login);
const mockRegister = vi.mocked(authService.register);
const mockFetchCurrentUser = vi.mocked(authService.fetchCurrentUser);

describe('auth.store', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    useAuthStore.setState({
      user: null,
      token: null,
      isLoading: false,
      isAuthenticated: false,
    });
  });

  it('has the correct initial state', () => {
    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoading).toBe(false);
  });

  it('logs in and stores user + token', async () => {
    const mockResponse = {
      access_token: 'jwt-token-123',
      token_type: 'bearer',
      user: { id: 1, email: 'test@test.com', name: 'Test', role: 'customer' as const },
    };
    mockLogin.mockResolvedValue(mockResponse);
    await useAuthStore.getState().login({ email: 'test@test.com', password: 'pass' });
    const state = useAuthStore.getState();
    expect(state.user).toEqual(mockResponse.user);
    expect(state.token).toBe('jwt-token-123');
    expect(state.isAuthenticated).toBe(true);
    expect(state.isLoading).toBe(false);
    expect(localStorage.getItem('access_token')).toBe('jwt-token-123');
  });

  it('throws and resets isLoading on login failure', async () => {
    mockLogin.mockRejectedValue(new Error('bad credentials'));
    await expect(
      useAuthStore.getState().login({ email: 'bad@test.com', password: 'wrong' }),
    ).rejects.toThrow('Login failed');
    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoading).toBe(false);
  });

  it('registers and stores user + token', async () => {
    const mockResponse = {
      access_token: 'jwt-reg-456',
      token_type: 'bearer',
      user: { id: 2, email: 'new@test.com', name: 'New User', role: 'customer' as const },
    };
    mockRegister.mockResolvedValue(mockResponse);
    await useAuthStore.getState().register({ email: 'new@test.com', password: 'pass123', name: 'New User' });
    const state = useAuthStore.getState();
    expect(state.user).toEqual(mockResponse.user);
    expect(state.token).toBe('jwt-reg-456');
    expect(state.isAuthenticated).toBe(true);
    expect(localStorage.getItem('access_token')).toBe('jwt-reg-456');
  });

  it('throws and resets isLoading on registration failure', async () => {
    mockRegister.mockRejectedValue(new Error('duplicate email'));
    await expect(
      useAuthStore.getState().register({ email: 'dup@test.com', password: 'pass', name: 'Dup' }),
    ).rejects.toThrow('Registration failed');
    expect(useAuthStore.getState().isLoading).toBe(false);
  });

  it('clears state on logout', () => {
    useAuthStore.setState({
      user: { id: 1, email: 'x@x.com', name: 'X', role: 'customer' },
      token: 'tok',
      isAuthenticated: true,
    });
    localStorage.setItem('access_token', 'tok');
    useAuthStore.getState().logout();
    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.token).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(localStorage.getItem('access_token')).toBeNull();
  });

  it('loads user from token', async () => {
    const mockUser = { id: 1, email: 'a@a.com', name: 'A', role: 'customer' as const };
    mockFetchCurrentUser.mockResolvedValue(mockUser);
    useAuthStore.setState({ token: 'valid-token' });
    await useAuthStore.getState().loadUser();
    const state = useAuthStore.getState();
    expect(state.user).toEqual(mockUser);
    expect(state.isAuthenticated).toBe(true);
    expect(state.isLoading).toBe(false);
  });

  it('clears state if loadUser fails', async () => {
    mockFetchCurrentUser.mockRejectedValue(new Error('expired'));
    useAuthStore.setState({ token: 'expired-token', isAuthenticated: true });
    localStorage.setItem('access_token', 'expired-token');
    await useAuthStore.getState().loadUser();
    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.token).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(localStorage.getItem('access_token')).toBeNull();
  });

  it('skips loadUser when no token is present', async () => {
    useAuthStore.setState({ token: null });
    await useAuthStore.getState().loadUser();
    expect(mockFetchCurrentUser).not.toHaveBeenCalled();
  });

  it('setToken stores token and sets isAuthenticated', () => {
    useAuthStore.getState().setToken('new-token');
    const state = useAuthStore.getState();
    expect(state.token).toBe('new-token');
    expect(state.isAuthenticated).toBe(true);
    expect(localStorage.getItem('access_token')).toBe('new-token');
  });
});
