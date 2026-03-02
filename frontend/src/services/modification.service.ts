import api from './api';
import type { ModificationRequest, ModificationCreate } from '@/types';

export async function createModification(payload: ModificationCreate): Promise<ModificationRequest> {
  const { data } = await api.post<ModificationRequest>('/modifications/', payload);
  return data;
}

export async function listModifications(): Promise<ModificationRequest[]> {
  const { data } = await api.get<ModificationRequest[]>('/modifications/');
  return data;
}
