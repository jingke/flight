import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import BookingsPage from './BookingsPage';
import type { BookingDetail } from '@/types';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('@/services/booking.service', () => ({
  listBookings: vi.fn(),
}));

import * as bookingService from '@/services/booking.service';

const mockListBookings = vi.mocked(bookingService.listBookings);

function makeBooking(overrides: Partial<BookingDetail> = {}): BookingDetail {
  return {
    id: 1,
    user_id: 1,
    flight_id: 10,
    status: 'confirmed',
    total_price: 299.99,
    payment_status: 'paid',
    created_at: '2026-01-15T10:00:00Z',
    passengers: [{ id: 1, name: 'Alice', email: 'alice@test.com', seat_assignment: '12A' }],
    flight_number: 'SK-101',
    departure_airport: 'JFK',
    arrival_airport: 'LAX',
    departure_time: '2026-02-20T08:00:00Z',
    arrival_time: '2026-02-20T11:30:00Z',
    ...overrides,
  };
}

describe('BookingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows a spinner while loading', () => {
    mockListBookings.mockReturnValue(new Promise(() => {}));
    const { container } = render(
      <MemoryRouter>
        <BookingsPage />
      </MemoryRouter>,
    );
    expect(container.querySelector('svg.animate-spin')).toBeInTheDocument();
  });

  it('renders the heading and booking count after loading', async () => {
    mockListBookings.mockResolvedValue([makeBooking(), makeBooking({ id: 2 })]);
    render(
      <MemoryRouter>
        <BookingsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('My Bookings')).toBeInTheDocument();
    });
    expect(screen.getByText('2 bookings total')).toBeInTheDocument();
  });

  it('shows empty state when there are no bookings', async () => {
    mockListBookings.mockResolvedValue([]);
    render(
      <MemoryRouter>
        <BookingsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('No bookings yet')).toBeInTheDocument();
    });
    expect(screen.getByText('Search flights')).toBeInTheDocument();
  });

  it('renders booking cards with flight details', async () => {
    mockListBookings.mockResolvedValue([makeBooking()]);
    render(
      <MemoryRouter>
        <BookingsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('SK-101')).toBeInTheDocument();
    });
    expect(screen.getByText('confirmed')).toBeInTheDocument();
    expect(screen.getByText('paid')).toBeInTheDocument();
    expect(screen.getByText('JFK → LAX')).toBeInTheDocument();
    expect(screen.getByText('$299.99')).toBeInTheDocument();
  });

  it('filters bookings by status when filter buttons are clicked', async () => {
    mockListBookings.mockResolvedValue([
      makeBooking({ id: 1, status: 'confirmed', flight_number: 'SK-101' }),
      makeBooking({ id: 2, status: 'cancelled', flight_number: 'SK-202' }),
    ]);
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <BookingsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('SK-101')).toBeInTheDocument();
    });
    expect(screen.getByText('SK-202')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Confirmed' }));
    expect(screen.getByText('SK-101')).toBeInTheDocument();
    expect(screen.queryByText('SK-202')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cancelled' }));
    expect(screen.queryByText('SK-101')).not.toBeInTheDocument();
    expect(screen.getByText('SK-202')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'All' }));
    expect(screen.getByText('SK-101')).toBeInTheDocument();
    expect(screen.getByText('SK-202')).toBeInTheDocument();
  });

  it('shows "No cancelled bookings" when filter returns empty', async () => {
    mockListBookings.mockResolvedValue([
      makeBooking({ id: 1, status: 'confirmed' }),
    ]);
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <BookingsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('My Bookings')).toBeInTheDocument();
    });
    await user.click(screen.getByRole('button', { name: 'Cancelled' }));
    expect(screen.getByText('No cancelled bookings')).toBeInTheDocument();
  });

  it('links each booking to its detail page', async () => {
    mockListBookings.mockResolvedValue([makeBooking({ id: 42 })]);
    render(
      <MemoryRouter>
        <BookingsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('SK-101')).toBeInTheDocument();
    });
    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/bookings/42');
  });
});
