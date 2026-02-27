import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import NotificationsPage from './NotificationsPage';
import type { AppNotification } from '@/types';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('@/services/notification.service', () => ({
  listNotifications: vi.fn(),
  markNotificationRead: vi.fn(),
  markAllRead: vi.fn(),
}));

import * as notificationService from '@/services/notification.service';
import toast from 'react-hot-toast';

const mockListNotifications = vi.mocked(notificationService.listNotifications);
const mockMarkRead = vi.mocked(notificationService.markNotificationRead);
const mockMarkAllRead = vi.mocked(notificationService.markAllRead);

function makeNotification(overrides: Partial<AppNotification> = {}): AppNotification {
  return {
    id: 1,
    user_id: 1,
    title: 'Booking Confirmed',
    message: 'Your flight SK-101 has been confirmed.',
    is_read: false,
    created_at: new Date().toISOString(),
    ...overrides,
  };
}

describe('NotificationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows a spinner while loading', () => {
    mockListNotifications.mockReturnValue(new Promise(() => {}));
    const { container } = render(<NotificationsPage />);
    expect(container.querySelector('svg.animate-spin')).toBeInTheDocument();
  });

  it('renders the heading', async () => {
    mockListNotifications.mockResolvedValue([]);
    render(<NotificationsPage />);
    await waitFor(() => {
      expect(screen.getByText('Notifications')).toBeInTheDocument();
    });
  });

  it('shows empty state when there are no notifications', async () => {
    mockListNotifications.mockResolvedValue([]);
    render(<NotificationsPage />);
    await waitFor(() => {
      expect(screen.getByText('No notifications')).toBeInTheDocument();
    });
  });

  it('shows "All caught up" when all notifications are read', async () => {
    mockListNotifications.mockResolvedValue([
      makeNotification({ is_read: true }),
    ]);
    render(<NotificationsPage />);
    await waitFor(() => {
      expect(screen.getByText('All caught up')).toBeInTheDocument();
    });
  });

  it('shows unread count when there are unread notifications', async () => {
    mockListNotifications.mockResolvedValue([
      makeNotification({ id: 1, is_read: false }),
      makeNotification({ id: 2, is_read: true }),
    ]);
    render(<NotificationsPage />);
    await waitFor(() => {
      expect(screen.getByText('1 unread')).toBeInTheDocument();
    });
  });

  it('renders notification titles and messages', async () => {
    mockListNotifications.mockResolvedValue([
      makeNotification({ title: 'Flight Delayed', message: 'SK-101 delayed by 2h' }),
    ]);
    render(<NotificationsPage />);
    await waitFor(() => {
      expect(screen.getByText('Flight Delayed')).toBeInTheDocument();
    });
    expect(screen.getByText('SK-101 delayed by 2h')).toBeInTheDocument();
  });

  it('marks a single notification as read', async () => {
    mockListNotifications.mockResolvedValue([
      makeNotification({ id: 5, is_read: false, title: 'Test Alert' }),
    ]);
    mockMarkRead.mockResolvedValue(makeNotification({ id: 5, is_read: true }));
    const user = userEvent.setup();
    render(<NotificationsPage />);
    await waitFor(() => {
      expect(screen.getByText('Test Alert')).toBeInTheDocument();
    });
    await user.click(screen.getByTitle('Mark as read'));
    expect(mockMarkRead).toHaveBeenCalledWith(5);
  });

  it('marks all notifications as read', async () => {
    mockListNotifications.mockResolvedValue([
      makeNotification({ id: 1, is_read: false }),
      makeNotification({ id: 2, is_read: false }),
    ]);
    mockMarkAllRead.mockResolvedValue();
    const user = userEvent.setup();
    render(<NotificationsPage />);
    await waitFor(() => {
      expect(screen.getByText('2 unread')).toBeInTheDocument();
    });
    await user.click(screen.getByRole('button', { name: 'Mark all read' }));
    expect(mockMarkAllRead).toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalledWith('All notifications marked as read');
  });

  it('does not show Mark all read button when no unread notifications', async () => {
    mockListNotifications.mockResolvedValue([
      makeNotification({ is_read: true }),
    ]);
    render(<NotificationsPage />);
    await waitFor(() => {
      expect(screen.getByText('All caught up')).toBeInTheDocument();
    });
    expect(screen.queryByRole('button', { name: 'Mark all read' })).not.toBeInTheDocument();
  });
});
