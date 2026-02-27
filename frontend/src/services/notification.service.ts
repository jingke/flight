import api from './api';
import type { AppNotification } from '@/types';

export async function listNotifications(): Promise<AppNotification[]> {
  const { data } = await api.get<AppNotification[]>('/notifications/');
  return data;
}

export async function markNotificationRead(notificationId: number): Promise<AppNotification> {
  const { data } = await api.put<AppNotification>(`/notifications/${notificationId}/read`);
  return data;
}

export async function markAllRead(): Promise<void> {
  await api.put('/notifications/read-all');
}
