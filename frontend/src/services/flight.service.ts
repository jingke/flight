import api from './api';
import type { Flight, FlightSearchParams } from '@/types';

export async function searchFlights(params: FlightSearchParams): Promise<Flight[]> {
  const query = new URLSearchParams();
  if (params.origin) query.set('origin', params.origin);
  if (params.destination) query.set('destination', params.destination);
  if (params.date) query.set('date', params.date);
  if (params.min_price !== undefined) query.set('min_price', String(params.min_price));
  if (params.max_price !== undefined) query.set('max_price', String(params.max_price));
  const { data } = await api.get<Flight[]>(`/flights/search?${query.toString()}`);
  return data;
}

export async function listFlights(): Promise<Flight[]> {
  const { data } = await api.get<Flight[]>('/flights/');
  return data;
}

export async function getFlight(flightId: number): Promise<Flight> {
  const { data } = await api.get<Flight>(`/flights/${flightId}`);
  return data;
}
