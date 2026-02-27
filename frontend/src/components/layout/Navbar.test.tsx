import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import Navbar from './Navbar';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

const mockLogout = vi.fn();

vi.mock('@/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

import { useAuthStore } from '@/stores/auth.store';

const mockedUseAuthStore = vi.mocked(useAuthStore);

function renderNavbar(overrides: Partial<ReturnType<typeof useAuthStore>> = {}) {
  const defaults = {
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: mockLogout,
    loadUser: vi.fn(),
    setToken: vi.fn(),
  };
  mockedUseAuthStore.mockReturnValue({ ...defaults, ...overrides } as ReturnType<typeof useAuthStore>);
  return render(
    <MemoryRouter>
      <Navbar />
    </MemoryRouter>,
  );
}

describe('Navbar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows the brand name SkyBooker', () => {
    renderNavbar();
    expect(screen.getByText('SkyBooker')).toBeInTheDocument();
  });

  it('shows Sign In and Get Started links when not authenticated', () => {
    renderNavbar({ isAuthenticated: false });
    expect(screen.getByText('Sign In')).toBeInTheDocument();
    expect(screen.getByText('Get Started')).toBeInTheDocument();
  });

  it('does not show navigation links when not authenticated', () => {
    renderNavbar({ isAuthenticated: false });
    expect(screen.queryByText('My Bookings')).not.toBeInTheDocument();
    expect(screen.queryByText('Flights')).not.toBeInTheDocument();
  });

  it('shows navigation links when authenticated', () => {
    renderNavbar({
      isAuthenticated: true,
      user: { id: 1, name: 'Alice', email: 'alice@test.com', role: 'customer' },
    });
    expect(screen.getByText('Flights')).toBeInTheDocument();
    expect(screen.getByText('My Bookings')).toBeInTheDocument();
    expect(screen.getByText('Route Map')).toBeInTheDocument();
    expect(screen.getByText('Loyalty')).toBeInTheDocument();
    expect(screen.getByText('Complaints')).toBeInTheDocument();
  });

  it('shows the user name when authenticated', () => {
    renderNavbar({
      isAuthenticated: true,
      user: { id: 1, name: 'Alice', email: 'alice@test.com', role: 'customer' },
    });
    expect(screen.getByText('Alice')).toBeInTheDocument();
  });

  it('shows Admin link only for admin users', () => {
    renderNavbar({
      isAuthenticated: true,
      user: { id: 1, name: 'Admin User', email: 'admin@test.com', role: 'admin' },
    });
    expect(screen.getByRole('link', { name: 'Admin' })).toBeInTheDocument();
  });

  it('does not show Admin link for customer users', () => {
    renderNavbar({
      isAuthenticated: true,
      user: { id: 1, name: 'Alice', email: 'alice@test.com', role: 'customer' },
    });
    expect(screen.queryByText('Admin')).not.toBeInTheDocument();
  });

  it('calls logout and navigates to /login on logout click', async () => {
    const user = userEvent.setup();
    renderNavbar({
      isAuthenticated: true,
      user: { id: 1, name: 'Alice', email: 'alice@test.com', role: 'customer' },
    });
    await user.click(screen.getByText('Logout'));
    expect(mockLogout).toHaveBeenCalledOnce();
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('does not show Sign In or Get Started when authenticated', () => {
    renderNavbar({
      isAuthenticated: true,
      user: { id: 1, name: 'Alice', email: 'alice@test.com', role: 'customer' },
    });
    expect(screen.queryByText('Sign In')).not.toBeInTheDocument();
    expect(screen.queryByText('Get Started')).not.toBeInTheDocument();
  });
});
