import api from './api';
import type { LoyaltyPoints, LoyaltyRedeem } from '@/types';

export async function getLoyalty(): Promise<LoyaltyPoints> {
  const { data } = await api.get<LoyaltyPoints>('/loyalty/');
  return data;
}

export async function redeemPoints(payload: LoyaltyRedeem): Promise<LoyaltyPoints> {
  const { data } = await api.post<LoyaltyPoints>('/loyalty/redeem', payload);
  return data;
}
