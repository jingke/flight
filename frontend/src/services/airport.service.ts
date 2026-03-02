import api from './api';
import type { Airport } from '@/types';

export async function listAirports(): Promise<Airport[]> {
  const { data } = await api.get<Airport[]>('/airports/');
  return data;
}

export async function getAirport(airportId: number): Promise<Airport> {
  const { data } = await api.get<Airport>(`/airports/${airportId}`);
  return data;
}
