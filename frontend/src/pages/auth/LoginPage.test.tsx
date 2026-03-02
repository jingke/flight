import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from './LoginPage';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const mockLogin = vi.fn();

vi.mock('@/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

import { useAuthStore } from '@/stores/auth.store';
import toast from 'react-hot-toast';

const mockedUseAuthStore = vi.mocked(useAuthStore);

function renderLoginPage(overrides: Partial<ReturnType<typeof useAuthStore>> = {}) {
  const defaults = {
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: false,
    login: mockLogin,
    register: vi.fn(),
    logout: vi.fn(),
    loadUser: vi.fn(),
    setToken: vi.fn(),
  };
  mockedUseAuthStore.mockReturnValue({ ...defaults, ...overrides } as ReturnType<typeof useAuthStore>);
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the sign-in heading', () => {
    renderLoginPage();
    expect(screen.getByText('Sign in to SkyBooker')).toBeInTheDocument();
  });

  it('renders email and password inputs', () => {
    renderLoginPage();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
  });

  it('renders a submit button', () => {
    renderLoginPage();
    expect(screen.getByRole('button', { name: 'Sign In' })).toBeInTheDocument();
  });

  it('renders a link to the registration page', () => {
    renderLoginPage();
    const link = screen.getByRole('link', { name: 'Sign up' });
    expect(link).toHaveAttribute('href', '/register');
  });

  it('shows loading text when isLoading is true', () => {
    renderLoginPage({ isLoading: true });
    expect(screen.getByRole('button', { name: /signing in/i })).toBeInTheDocument();
  });

  it('calls login with credentials and navigates on success', async () => {
    mockLogin.mockResolvedValueOnce(undefined);
    const user = userEvent.setup();
    renderLoginPage();
    await user.type(screen.getByLabelText('Email'), 'alice@test.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign In' }));
    expect(mockLogin).toHaveBeenCalledWith({
      email: 'alice@test.com',
      password: 'password123',
    });
    expect(toast.success).toHaveBeenCalledWith('Welcome back!');
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('shows error toast on login failure', async () => {
    mockLogin.mockRejectedValueOnce(new Error('fail'));
    const user = userEvent.setup();
    renderLoginPage();
    await user.type(screen.getByLabelText('Email'), 'bad@test.com');
    await user.type(screen.getByLabelText('Password'), 'wrong');
    await user.click(screen.getByRole('button', { name: 'Sign In' }));
    expect(toast.error).toHaveBeenCalledWith('Invalid email or password');
  });

  it('disables the submit button when isLoading is true', () => {
    renderLoginPage({ isLoading: true });
    expect(screen.getByRole('button', { name: /signing in/i })).toBeDisabled();
  });
});
