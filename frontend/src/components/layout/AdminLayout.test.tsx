import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import AdminLayout from './AdminLayout';

function renderAdminLayout(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route element={<AdminLayout />}>
          <Route path="/admin" element={<div>Dashboard Content</div>} />
          <Route path="/admin/flights" element={<div>Flights Content</div>} />
          <Route path="/admin/reservations" element={<div>Reservations Content</div>} />
          <Route path="/admin/reports" element={<div>Reports Content</div>} />
          <Route path="/admin/complaints" element={<div>Complaints Content</div>} />
          <Route path="/admin/modifications" element={<div>Modifications Content</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('AdminLayout', () => {
  it('renders the Administration sidebar heading', () => {
    renderAdminLayout('/admin');
    expect(screen.getByText('Administration')).toBeInTheDocument();
  });

  it('renders all sidebar navigation links', () => {
    renderAdminLayout('/admin');
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Flights')).toBeInTheDocument();
    expect(screen.getByText('Reservations')).toBeInTheDocument();
    expect(screen.getByText('Reports')).toBeInTheDocument();
    expect(screen.getByText('Complaints')).toBeInTheDocument();
    expect(screen.getByText('Modifications')).toBeInTheDocument();
  });

  it('renders the child route content via Outlet', () => {
    renderAdminLayout('/admin');
    expect(screen.getByText('Dashboard Content')).toBeInTheDocument();
  });

  it('renders the correct child for /admin/flights', () => {
    renderAdminLayout('/admin/flights');
    expect(screen.getByText('Flights Content')).toBeInTheDocument();
  });

  it('renders the correct child for /admin/reports', () => {
    renderAdminLayout('/admin/reports');
    expect(screen.getByText('Reports Content')).toBeInTheDocument();
  });
});
