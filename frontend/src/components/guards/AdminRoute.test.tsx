import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import AdminRoute from './AdminRoute';

vi.mock('@/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

import { useAuthStore } from '@/stores/auth.store';

const mockedUseAuthStore = vi.mocked(useAuthStore);

function renderWithRoutes(isAuthenticated: boolean, role?: 'admin' | 'customer') {
  mockedUseAuthStore.mockReturnValue({
    isAuthenticated,
    user: isAuthenticated ? { id: 1, name: 'Test', email: 'test@test.com', role: role ?? 'customer' } : null,
  } as ReturnType<typeof useAuthStore>);

  return render(
    <MemoryRouter initialEntries={['/admin']}>
      <Routes>
        <Route element={<AdminRoute />}>
          <Route path="/admin" element={<div>Admin Content</div>} />
        </Route>
        <Route path="/login" element={<div>Login Page</div>} />
        <Route path="/" element={<div>Home Page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('AdminRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders child route for authenticated admin users', () => {
    renderWithRoutes(true, 'admin');
    expect(screen.getByText('Admin Content')).toBeInTheDocument();
  });

  it('redirects to /login when user is not authenticated', () => {
    renderWithRoutes(false);
    expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
    expect(screen.getByText('Login Page')).toBeInTheDocument();
  });

  it('redirects to / when user is authenticated but not admin', () => {
    renderWithRoutes(true, 'customer');
    expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
    expect(screen.getByText('Home Page')).toBeInTheDocument();
  });
});
