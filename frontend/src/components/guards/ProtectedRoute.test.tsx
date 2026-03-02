import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';

vi.mock('@/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

import { useAuthStore } from '@/stores/auth.store';

const mockedUseAuthStore = vi.mocked(useAuthStore);

function renderWithRoutes(isAuthenticated: boolean) {
  mockedUseAuthStore.mockReturnValue({ isAuthenticated } as ReturnType<typeof useAuthStore>);
  return render(
    <MemoryRouter initialEntries={['/protected']}>
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/protected" element={<div>Protected Content</div>} />
        </Route>
        <Route path="/login" element={<div>Login Page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders child route when user is authenticated', () => {
    renderWithRoutes(true);
    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  it('redirects to /login when user is not authenticated', () => {
    renderWithRoutes(false);
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    expect(screen.getByText('Login Page')).toBeInTheDocument();
  });
});
