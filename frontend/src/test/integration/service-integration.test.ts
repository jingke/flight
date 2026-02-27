/**
 * Frontend service integration tests.
 *
 * These tests verify that the service layer correctly orchestrates
 * API calls in the same sequences used by both the web frontend
 * and the Android client (which mirrors the same REST contract).
 *
 * Each test exercises a realistic multi-step workflow, mocking the
 * axios-based API layer to validate request payloads, response
 * handling, and inter-service data flow.
 */
import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest';

vi.mock('@/services/api', () => {
  const mockApi = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  };
  return { default: mockApi };
});

import api from '@/services/api';
import { login, register, fetchCurrentUser } from '@/services/auth.service';
import { searchFlights, listFlights, getFlight } from '@/services/flight.service';
import { createBooking, listBookings, getBooking, cancelBooking } from '@/services/booking.service';
import { getSeatMap, assignSeat } from '@/services/seat.service';
import { createComplaint, listComplaints, getComplaint } from '@/services/complaint.service';
import { listNotifications, markNotificationRead } from '@/services/notification.service';
import { getLoyalty, redeemPoints } from '@/services/loyalty.service';
import { createModification, listModifications } from '@/services/modification.service';
import {
  fetchFlights,
  createFlight,
  updateFlight,
  deleteFlight,
  fetchAllBookings,
  fetchAllComplaints,
  updateComplaint,
  fetchModificationRequests,
  updateModificationRequest,
  fetchBookingsPerFlight,
  fetchPopularRoutes,
  fetchPeakTimes,
} from '@/services/admin.service';
import type {
  AuthResponse,
  User,
  Flight,
  BookingDetail,
  Seat,
  Complaint,
  AppNotification,
  LoyaltyPoints,
  ModificationRequest,
} from '@/types';

const mockApi = api as unknown as {
  get: Mock;
  post: Mock;
  put: Mock;
  delete: Mock;
};

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

const mockUser: User = { id: 1, email: 'test@test.com', name: 'Test', role: 'customer' };
const mockAuthResponse: AuthResponse = {
  access_token: 'jwt-token-123',
  token_type: 'bearer',
  user: mockUser,
};
const mockFlight: Flight = {
  id: 1,
  flight_number: 'FB101',
  departure_airport_id: 1,
  arrival_airport_id: 2,
  departure_time: '2026-04-01T08:00:00',
  arrival_time: '2026-04-01T13:30:00',
  price: 320,
  total_seats: 180,
  status: 'scheduled',
  departure_airport: { id: 1, code: 'JFK', name: 'JFK International', city: 'New York', country: 'US' },
  arrival_airport: { id: 2, code: 'LAX', name: 'LAX International', city: 'Los Angeles', country: 'US' },
  available_seats: 178,
};
const mockBooking: BookingDetail = {
  id: 1,
  user_id: 1,
  flight_id: 1,
  status: 'confirmed',
  total_price: 320,
  payment_status: 'paid',
  created_at: '2026-03-10T08:00:00',
  passengers: [{ id: 1, name: 'Test', email: 'test@test.com', seat_assignment: '1A' }],
  flight_number: 'FB101',
  departure_airport: 'JFK - New York',
  arrival_airport: 'LAX - Los Angeles',
  departure_time: '2026-04-01T08:00:00',
  arrival_time: '2026-04-01T13:30:00',
};
const mockSeat: Seat = { id: 10, flight_id: 1, row: 1, column: 'A', seat_class: 'economy', is_available: true };
const mockComplaint: Complaint = {
  id: 1,
  user_id: 1,
  booking_id: 1,
  subject: 'Delay',
  description: 'Flight delayed',
  status: 'open',
  admin_response: null,
  created_at: '2026-03-10T10:00:00',
};
const mockNotification: AppNotification = {
  id: 1,
  user_id: 1,
  title: 'Booking Confirmed',
  message: 'Your booking was confirmed',
  is_read: false,
  created_at: '2026-03-10T10:00:00',
};
const mockLoyalty: LoyaltyPoints = { id: 1, user_id: 1, earned: 500, redeemed: 0, balance: 500 };
const mockModification: ModificationRequest = {
  id: 1,
  user_id: 1,
  booking_id: 1,
  type: 'date_change',
  details: 'Move to April 20',
  status: 'pending',
  created_at: '2026-03-10T10:00:00',
};

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('Customer Booking Journey Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('executes full register → search → book → view → cancel flow', async () => {
    // Step 1: Register
    mockApi.post.mockResolvedValueOnce({ data: mockAuthResponse });
    const authResult = await register({
      email: 'new@test.com',
      password: 'pass123',
      name: 'New User',
    });
    expect(mockApi.post).toHaveBeenCalledWith('/auth/register', {
      email: 'new@test.com',
      password: 'pass123',
      name: 'New User',
    });
    expect(authResult.access_token).toBe('jwt-token-123');

    // Step 2: Fetch user profile
    mockApi.get.mockResolvedValueOnce({ data: mockUser });
    const user = await fetchCurrentUser();
    expect(mockApi.get).toHaveBeenCalledWith('/auth/me');
    expect(user.email).toBe('test@test.com');

    // Step 3: Search flights
    mockApi.get.mockResolvedValueOnce({ data: [mockFlight] });
    const flights = await searchFlights({ origin: 'JFK' });
    expect(flights).toHaveLength(1);
    expect(flights[0].departure_airport?.code).toBe('JFK');

    // Step 4: Get seat map
    mockApi.get.mockResolvedValueOnce({ data: [mockSeat, { ...mockSeat, id: 11, column: 'B' }] });
    const seats = await getSeatMap(mockFlight.id);
    expect(seats).toHaveLength(2);

    // Step 5: Create booking
    mockApi.post.mockResolvedValueOnce({ data: mockBooking });
    const booking = await createBooking({
      flight_id: mockFlight.id,
      passengers: [{ name: 'Test', email: 'test@test.com', seat_id: seats[0].id }],
    });
    expect(booking.status).toBe('confirmed');
    expect(booking.passengers[0].seat_assignment).toBe('1A');

    // Step 6: View booking
    mockApi.get.mockResolvedValueOnce({ data: mockBooking });
    const detail = await getBooking(booking.id);
    expect(detail.flight_number).toBe('FB101');

    // Step 7: List bookings
    mockApi.get.mockResolvedValueOnce({ data: [mockBooking] });
    const allBookings = await listBookings();
    expect(allBookings).toHaveLength(1);

    // Step 8: Check notifications
    mockApi.get.mockResolvedValueOnce({ data: [mockNotification] });
    const notifs = await listNotifications();
    expect(notifs[0].title).toBe('Booking Confirmed');

    // Step 9: Cancel booking
    const cancelledBooking = { ...mockBooking, status: 'cancelled', payment_status: 'refunded' };
    mockApi.post.mockResolvedValueOnce({ data: cancelledBooking });
    const cancelled = await cancelBooking(booking.id);
    expect(cancelled.status).toBe('cancelled');
    expect(cancelled.payment_status).toBe('refunded');
  });
});

describe('Login and Token Flow Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('login returns token and user data for API calls', async () => {
    mockApi.post.mockResolvedValueOnce({ data: mockAuthResponse });
    const result = await login({ email: 'test@test.com', password: 'pass123' });
    expect(result.access_token).toBeDefined();
    expect(result.user.role).toBe('customer');
    expect(mockApi.post).toHaveBeenCalledWith('/auth/login', {
      email: 'test@test.com',
      password: 'pass123',
    });
  });

  it('login failure propagates error', async () => {
    mockApi.post.mockRejectedValueOnce({ status: 401, message: 'Invalid credentials' });
    await expect(login({ email: 'bad@test.com', password: 'wrong' })).rejects.toBeDefined();
  });
});

describe('Complaint Lifecycle Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('submit → list → admin resolve → check notification', async () => {
    // Customer creates complaint
    mockApi.post.mockResolvedValueOnce({ data: mockComplaint });
    const complaint = await createComplaint({
      booking_id: 1,
      subject: 'Delay',
      description: 'Flight delayed',
    });
    expect(complaint.status).toBe('open');

    // Customer lists complaints
    mockApi.get.mockResolvedValueOnce({ data: [mockComplaint] });
    const complaints = await listComplaints();
    expect(complaints).toHaveLength(1);

    // Admin resolves (via admin service)
    const resolved = { ...mockComplaint, status: 'resolved', admin_response: 'Fixed' };
    mockApi.put.mockResolvedValueOnce({ data: resolved });
    const updated = await updateComplaint(complaint.id, {
      status: 'resolved',
      admin_response: 'Fixed',
    });
    expect(updated.status).toBe('resolved');
    expect(updated.admin_response).toBe('Fixed');

    // Customer checks notification
    const complaintNotif: AppNotification = {
      ...mockNotification,
      id: 2,
      title: 'Complaint Updated',
      message: "Your complaint 'Delay' has been updated.",
    };
    mockApi.get.mockResolvedValueOnce({ data: [complaintNotif] });
    const notifs = await listNotifications();
    expect(notifs[0].title).toBe('Complaint Updated');
  });
});

describe('Modification Request Flow Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('request → admin approve → notification', async () => {
    mockApi.post.mockResolvedValueOnce({ data: mockModification });
    const mod = await createModification({
      booking_id: 1,
      type: 'date_change',
      details: 'Move to April 20',
    });
    expect(mod.status).toBe('pending');

    mockApi.get.mockResolvedValueOnce({ data: [mockModification] });
    const mods = await listModifications();
    expect(mods).toHaveLength(1);

    const approved = { ...mockModification, status: 'approved' };
    mockApi.put.mockResolvedValueOnce({ data: approved });
    const result = await updateModificationRequest(mod.id, 'approved');
    expect(result.status).toBe('approved');
  });
});

describe('Loyalty Points Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('check balance → redeem → verify updated balance', async () => {
    mockApi.get.mockResolvedValueOnce({ data: mockLoyalty });
    const loyalty = await getLoyalty();
    expect(loyalty.balance).toBe(500);

    const afterRedeem = { ...mockLoyalty, redeemed: 100, balance: 400 };
    mockApi.post.mockResolvedValueOnce({ data: afterRedeem });
    const redeemed = await redeemPoints({ points: 100 });
    expect(redeemed.balance).toBe(400);
    expect(redeemed.redeemed).toBe(100);
  });
});

describe('Admin Dashboard Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches all admin resources in parallel', async () => {
    mockApi.get
      .mockResolvedValueOnce({ data: [mockFlight] })              // flights
      .mockResolvedValueOnce({ data: [mockBooking] })              // bookings
      .mockResolvedValueOnce({ data: [mockComplaint] })            // complaints
      .mockResolvedValueOnce({ data: [mockModification] })         // modifications
      .mockResolvedValueOnce({ data: [{ flight_id: 1, flight_number: 'FB101', departure: 'JFK', arrival: 'LAX', booking_count: 5 }] })
      .mockResolvedValueOnce({ data: [{ origin_code: 'JFK', origin_city: 'New York', destination_code: 'LAX', destination_city: 'LA', booking_count: 10 }] })
      .mockResolvedValueOnce({ data: [{ hour: 10, booking_count: 15 }] });

    const [flights, bookings, complaints, mods, bpf, routes, peaks] = await Promise.all([
      fetchFlights(),
      fetchAllBookings(),
      fetchAllComplaints(),
      fetchModificationRequests(),
      fetchBookingsPerFlight(),
      fetchPopularRoutes(),
      fetchPeakTimes(),
    ]);

    expect(flights).toHaveLength(1);
    expect(bookings).toHaveLength(1);
    expect(complaints).toHaveLength(1);
    expect(mods).toHaveLength(1);
    expect(bpf).toHaveLength(1);
    expect(routes).toHaveLength(1);
    expect(peaks).toHaveLength(1);
  });

  it('creates and updates a flight', async () => {
    const newFlight = { ...mockFlight, id: 99, flight_number: 'NEW-01' };
    mockApi.post.mockResolvedValueOnce({ data: newFlight });
    const created = await createFlight({
      flight_number: 'NEW-01',
      departure_airport_id: 1,
      arrival_airport_id: 2,
      departure_time: '2026-06-01T10:00:00',
      arrival_time: '2026-06-01T15:00:00',
      price: 400,
      total_seats: 100,
    });
    expect(created.flight_number).toBe('NEW-01');

    const updatedFlight = { ...newFlight, price: 450 };
    mockApi.put.mockResolvedValueOnce({ data: updatedFlight });
    const updated = await updateFlight(99, { price: 450 });
    expect(updated.price).toBe(450);

    mockApi.delete.mockResolvedValueOnce({});
    await deleteFlight(99);
    expect(mockApi.delete).toHaveBeenCalledWith('/flights/99');
  });
});

describe('Notification Read Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('lists notifications and marks one as read', async () => {
    mockApi.get.mockResolvedValueOnce({
      data: [mockNotification, { ...mockNotification, id: 2, title: 'Booking Cancelled' }],
    });
    const notifs = await listNotifications();
    expect(notifs).toHaveLength(2);
    expect(notifs[0].is_read).toBe(false);

    const readNotif = { ...mockNotification, is_read: true };
    mockApi.put.mockResolvedValueOnce({ data: readNotif });
    const result = await markNotificationRead(1);
    expect(result.is_read).toBe(true);
    expect(mockApi.put).toHaveBeenCalledWith('/notifications/1/read');
  });
});

describe('Seat Assignment Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches seat map and assigns a seat', async () => {
    const seats: Seat[] = [
      mockSeat,
      { ...mockSeat, id: 11, column: 'B' },
      { ...mockSeat, id: 12, column: 'C', is_available: false },
    ];
    mockApi.get.mockResolvedValueOnce({ data: seats });
    const seatMap = await getSeatMap(1);
    const available = seatMap.filter((s) => s.is_available);
    expect(available).toHaveLength(2);

    const assigned = { ...mockSeat, is_available: false };
    mockApi.post.mockResolvedValueOnce({ data: assigned });
    const result = await assignSeat({ passenger_id: 1, seat_id: 10 });
    expect(result.is_available).toBe(false);
    expect(mockApi.post).toHaveBeenCalledWith('/seats/assign', {
      passenger_id: 1,
      seat_id: 10,
    });
  });
});
