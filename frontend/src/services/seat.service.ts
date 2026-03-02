import api from './api';
import type { Seat, SeatAssignment } from '@/types';

export async function getSeatMap(flightId: number): Promise<Seat[]> {
  const { data } = await api.get<Seat[]>(`/seats/flight/${flightId}`);
  return data;
}

export async function assignSeat(payload: SeatAssignment): Promise<Seat> {
  const { data } = await api.post<Seat>('/seats/assign', payload);
  return data;
}
