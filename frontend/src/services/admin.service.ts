import api from './api';
import type {
  Flight,
  FlightCreate,
  FlightUpdate,
  BookingDetail,
  Complaint,
  ComplaintUpdate,
  ModificationRequest,
  AirportBrief,
  BookingsPerFlightReport,
  PopularRouteReport,
  PeakTimeReport,
} from '@/types';

export async function fetchFlights(): Promise<Flight[]> {
  const { data } = await api.get<Flight[]>('/flights/');
  return data;
}

export async function createFlight(payload: FlightCreate): Promise<Flight> {
  const { data } = await api.post<Flight>('/flights/', payload);
  return data;
}

export async function updateFlight(id: number, payload: FlightUpdate): Promise<Flight> {
  const { data } = await api.put<Flight>(`/flights/${id}`, payload);
  return data;
}

export async function deleteFlight(id: number): Promise<void> {
  await api.delete(`/flights/${id}`);
}

export async function fetchAllBookings(): Promise<BookingDetail[]> {
  const { data } = await api.get<BookingDetail[]>('/bookings/');
  return data;
}

export async function cancelBooking(id: number): Promise<BookingDetail> {
  const { data } = await api.post<BookingDetail>(`/bookings/${id}/cancel`);
  return data;
}

export async function fetchAllComplaints(): Promise<Complaint[]> {
  const { data } = await api.get<Complaint[]>('/complaints/');
  return data;
}

export async function updateComplaint(id: number, payload: ComplaintUpdate): Promise<Complaint> {
  const { data } = await api.put<Complaint>(`/complaints/${id}`, payload);
  return data;
}

export async function fetchModificationRequests(): Promise<ModificationRequest[]> {
  const { data } = await api.get<ModificationRequest[]>('/modifications/');
  return data;
}

export async function updateModificationRequest(id: number, status: string): Promise<ModificationRequest> {
  const { data } = await api.put<ModificationRequest>(`/modifications/${id}`, { status });
  return data;
}

export async function fetchAirports(): Promise<AirportBrief[]> {
  const { data } = await api.get<AirportBrief[]>('/airports/');
  return data;
}

export async function fetchBookingsPerFlight(): Promise<BookingsPerFlightReport[]> {
  const { data } = await api.get<BookingsPerFlightReport[]>('/reports/bookings-per-flight');
  return data;
}

export async function fetchPopularRoutes(): Promise<PopularRouteReport[]> {
  const { data } = await api.get<PopularRouteReport[]>('/reports/popular-routes');
  return data;
}

export async function fetchPeakTimes(): Promise<PeakTimeReport[]> {
  const { data } = await api.get<PeakTimeReport[]>('/reports/peak-times');
  return data;
}
