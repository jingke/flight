import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import GuestRoute from './GuestRoute';

vi.mock('@/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

import { useAuthStore } from '@/stores/auth.store';

const mockedUseAuthStore = vi.mocked(useAuthStore);

function renderWithRoutes(isAuthenticated: boolean) {
  mockedUseAuthStore.mockReturnValue({ isAuthenticated } as ReturnType<typeof useAuthStore>);
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <Routes>
        <Route element={<GuestRoute />}>
          <Route path="/login" element={<div>Login Form</div>} />
        </Route>
        <Route path="/" element={<div>Home Page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('GuestRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders child route for unauthenticated users', () => {
    renderWithRoutes(false);
    expect(screen.getByText('Login Form')).toBeInTheDocument();
  });

  it('redirects to / when user is already authenticated', () => {
    renderWithRoutes(true);
    expect(screen.queryByText('Login Form')).not.toBeInTheDocument();
    expect(screen.getByText('Home Page')).toBeInTheDocument();
  });
});
