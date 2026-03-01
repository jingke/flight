import api from './api';
import type { SavedPassenger, SavedPassengerCreate, SavedPassengerUpdate } from '@/types';

export async function fetchSavedPassengers(): Promise<SavedPassenger[]> {
  const { data } = await api.get<SavedPassenger[]>('/passengers');
  return data;
}

export async function createSavedPassenger(payload: SavedPassengerCreate): Promise<SavedPassenger> {
  const { data } = await api.post<SavedPassenger>('/passengers', payload);
  return data;
}

export async function updateSavedPassenger(
  id: number,
  payload: SavedPassengerUpdate
): Promise<SavedPassenger> {
  const { data } = await api.put<SavedPassenger>(`/passengers/${id}`, payload);
  return data;
}

export async function deleteSavedPassenger(id: number): Promise<void> {
  await api.delete(`/passengers/${id}`);
}
