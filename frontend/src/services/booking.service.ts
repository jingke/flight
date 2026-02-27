import api from './api';
import type { BookingCreate, BookingDetail } from '@/types';

export async function createBooking(payload: BookingCreate): Promise<BookingDetail> {
  const { data } = await api.post<BookingDetail>('/bookings/', payload);
  return data;
}

export async function listBookings(): Promise<BookingDetail[]> {
  const { data } = await api.get<BookingDetail[]>('/bookings/');
  return data;
}

export async function getBooking(bookingId: number): Promise<BookingDetail> {
  const { data } = await api.get<BookingDetail>(`/bookings/${bookingId}`);
  return data;
}

export async function cancelBooking(bookingId: number): Promise<BookingDetail> {
  const { data } = await api.post<BookingDetail>(`/bookings/${bookingId}/cancel`);
  return data;
}
