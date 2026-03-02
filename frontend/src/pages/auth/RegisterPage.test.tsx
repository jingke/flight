import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import RegisterPage from './RegisterPage';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const mockRegister = vi.fn();

vi.mock('@/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

import { useAuthStore } from '@/stores/auth.store';
import toast from 'react-hot-toast';

const mockedUseAuthStore = vi.mocked(useAuthStore);

function renderRegisterPage(overrides: Partial<ReturnType<typeof useAuthStore>> = {}) {
  const defaults = {
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: false,
    login: vi.fn(),
    register: mockRegister,
    logout: vi.fn(),
    loadUser: vi.fn(),
    setToken: vi.fn(),
  };
  mockedUseAuthStore.mockReturnValue({ ...defaults, ...overrides } as ReturnType<typeof useAuthStore>);
  return render(
    <MemoryRouter>
      <RegisterPage />
    </MemoryRouter>,
  );
}

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the create account heading', () => {
    renderRegisterPage();
    expect(screen.getByText('Create your account')).toBeInTheDocument();
  });

  it('renders name, email, and password inputs', () => {
    renderRegisterPage();
    expect(screen.getByLabelText('Full Name')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
  });

  it('renders a submit button', () => {
    renderRegisterPage();
    expect(screen.getByRole('button', { name: 'Create Account' })).toBeInTheDocument();
  });

  it('renders a link to the login page', () => {
    renderRegisterPage();
    const link = screen.getByRole('link', { name: 'Sign in' });
    expect(link).toHaveAttribute('href', '/login');
  });

  it('shows loading text when isLoading is true', () => {
    renderRegisterPage({ isLoading: true });
    expect(screen.getByRole('button', { name: /creating account/i })).toBeInTheDocument();
  });

  it('calls register with form data and navigates on success', async () => {
    mockRegister.mockResolvedValueOnce(undefined);
    const user = userEvent.setup();
    renderRegisterPage();
    await user.type(screen.getByLabelText('Full Name'), 'Alice');
    await user.type(screen.getByLabelText('Email'), 'alice@test.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Create Account' }));
    expect(mockRegister).toHaveBeenCalledWith({
      name: 'Alice',
      email: 'alice@test.com',
      password: 'password123',
    });
    expect(toast.success).toHaveBeenCalledWith('Account created successfully!');
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('shows error toast on registration failure', async () => {
    mockRegister.mockRejectedValueOnce(new Error('fail'));
    const user = userEvent.setup();
    renderRegisterPage();
    await user.type(screen.getByLabelText('Full Name'), 'Bob');
    await user.type(screen.getByLabelText('Email'), 'bob@test.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Create Account' }));
    expect(toast.error).toHaveBeenCalledWith('Registration failed — please try again');
  });

  it('disables the submit button when isLoading is true', () => {
    renderRegisterPage({ isLoading: true });
    expect(screen.getByRole('button', { name: /creating account/i })).toBeDisabled();
  });
});
