import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import ComplaintsPage from './ComplaintsPage';
import type { Complaint } from '@/types';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('@/services/complaint.service', () => ({
  listComplaints: vi.fn(),
  createComplaint: vi.fn(),
}));

vi.mock('@/services/booking.service', () => ({
  listBookings: vi.fn(),
}));

import * as complaintService from '@/services/complaint.service';
import * as bookingService from '@/services/booking.service';
import toast from 'react-hot-toast';

const mockListComplaints = vi.mocked(complaintService.listComplaints);
const mockCreateComplaint = vi.mocked(complaintService.createComplaint);
const mockListBookings = vi.mocked(bookingService.listBookings);

function makeComplaint(overrides: Partial<Complaint> = {}): Complaint {
  return {
    id: 1,
    user_id: 1,
    booking_id: 10,
    subject: 'Lost Luggage',
    description: 'My bag did not arrive.',
    status: 'open',
    admin_response: null,
    created_at: '2026-02-15T10:00:00Z',
    ...overrides,
  };
}

describe('ComplaintsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockListBookings.mockResolvedValue([]);
  });

  it('shows a spinner while loading', () => {
    mockListComplaints.mockReturnValue(new Promise(() => {}));
    const { container } = render(
      <MemoryRouter>
        <ComplaintsPage />
      </MemoryRouter>,
    );
    expect(container.querySelector('svg.animate-spin')).toBeInTheDocument();
  });

  it('renders the heading', async () => {
    mockListComplaints.mockResolvedValue([]);
    render(
      <MemoryRouter>
        <ComplaintsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('Complaints')).toBeInTheDocument();
    });
  });

  it('shows empty state when there are no complaints', async () => {
    mockListComplaints.mockResolvedValue([]);
    render(
      <MemoryRouter>
        <ComplaintsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('No complaints')).toBeInTheDocument();
    });
  });

  it('renders complaint cards with subject, status, and description', async () => {
    mockListComplaints.mockResolvedValue([makeComplaint()]);
    render(
      <MemoryRouter>
        <ComplaintsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('Lost Luggage')).toBeInTheDocument();
    });
    expect(screen.getByText('open')).toBeInTheDocument();
    expect(screen.getByText('My bag did not arrive.')).toBeInTheDocument();
    expect(screen.getByText('Booking #10')).toBeInTheDocument();
  });

  it('renders admin response when present', async () => {
    mockListComplaints.mockResolvedValue([
      makeComplaint({ admin_response: 'We are looking into it.' }),
    ]);
    render(
      <MemoryRouter>
        <ComplaintsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('Admin Response')).toBeInTheDocument();
    });
    expect(screen.getByText('We are looking into it.')).toBeInTheDocument();
  });

  it('toggles the complaint form when New Complaint button is clicked', async () => {
    mockListComplaints.mockResolvedValue([]);
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <ComplaintsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('New Complaint')).toBeInTheDocument();
    });
    expect(screen.queryByText('Submit a Complaint')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /new complaint/i }));
    expect(screen.getByText('Submit a Complaint')).toBeInTheDocument();
  });

  it('submits a complaint and shows success toast', async () => {
    mockListComplaints.mockResolvedValue([]);
    mockCreateComplaint.mockResolvedValue(makeComplaint());
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <ComplaintsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('Complaints')).toBeInTheDocument();
    });
    await user.click(screen.getByRole('button', { name: /new complaint/i }));
    await user.type(screen.getByPlaceholderText('Brief summary of your complaint'), 'Bad Service');
    await user.type(screen.getByPlaceholderText('Provide details about your issue...'), 'The seat was broken.');
    await user.click(screen.getByRole('button', { name: 'Submit Complaint' }));
    expect(mockCreateComplaint).toHaveBeenCalledWith({
      subject: 'Bad Service',
      description: 'The seat was broken.',
      booking_id: null,
    });
    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith('Complaint submitted successfully');
    });
  });

  it('shows different status badges', async () => {
    mockListComplaints.mockResolvedValue([
      makeComplaint({ id: 1, status: 'open', subject: 'Issue A' }),
      makeComplaint({ id: 2, status: 'resolved', subject: 'Issue B' }),
    ]);
    render(
      <MemoryRouter>
        <ComplaintsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('Issue A')).toBeInTheDocument();
    });
    expect(screen.getByText('open')).toBeInTheDocument();
    expect(screen.getByText('resolved')).toBeInTheDocument();
  });
});
