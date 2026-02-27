import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import LoyaltyPage from './LoyaltyPage';
import type { LoyaltyPoints } from '@/types';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('recharts', () => ({
  BarChart: ({ children }: { children: React.ReactNode }) => <div data-testid="bar-chart">{children}</div>,
  Bar: () => <div data-testid="bar" />,
  XAxis: () => <div data-testid="x-axis" />,
  YAxis: () => <div data-testid="y-axis" />,
  CartesianGrid: () => <div data-testid="cartesian-grid" />,
  Tooltip: () => <div data-testid="tooltip" />,
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('@/services/loyalty.service', () => ({
  getLoyalty: vi.fn(),
  redeemPoints: vi.fn(),
}));

import * as loyaltyService from '@/services/loyalty.service';
import toast from 'react-hot-toast';

const mockGetLoyalty = vi.mocked(loyaltyService.getLoyalty);
const mockRedeemPoints = vi.mocked(loyaltyService.redeemPoints);

const sampleLoyalty: LoyaltyPoints = {
  id: 1,
  user_id: 1,
  earned: 5000,
  redeemed: 1000,
  balance: 4000,
};

describe('LoyaltyPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows a spinner while loading', () => {
    mockGetLoyalty.mockReturnValue(new Promise(() => {}));
    const { container } = render(<LoyaltyPage />);
    expect(container.querySelector('svg.animate-spin')).toBeInTheDocument();
  });

  it('renders the heading', async () => {
    mockGetLoyalty.mockResolvedValue(sampleLoyalty);
    render(<LoyaltyPage />);
    await waitFor(() => {
      expect(screen.getByText('Loyalty Dashboard')).toBeInTheDocument();
    });
  });

  it('shows empty state when no loyalty data', async () => {
    mockGetLoyalty.mockRejectedValue(new Error('not found'));
    render(<LoyaltyPage />);
    await waitFor(() => {
      expect(screen.getByText('No loyalty data yet')).toBeInTheDocument();
    });
  });

  it('displays points summary cards', async () => {
    mockGetLoyalty.mockResolvedValue(sampleLoyalty);
    render(<LoyaltyPage />);
    await waitFor(() => {
      expect(screen.getByText('5,000')).toBeInTheDocument();
    });
    expect(screen.getByText('1,000')).toBeInTheDocument();
    expect(screen.getByText('4,000')).toBeInTheDocument();
    expect(screen.getByText('Total Earned')).toBeInTheDocument();
    expect(screen.getByText('Redeemed')).toBeInTheDocument();
    expect(screen.getByText('Available Balance')).toBeInTheDocument();
  });

  it('renders the chart', async () => {
    mockGetLoyalty.mockResolvedValue(sampleLoyalty);
    render(<LoyaltyPage />);
    await waitFor(() => {
      expect(screen.getByText('Points Overview')).toBeInTheDocument();
    });
    expect(screen.getByTestId('bar-chart')).toBeInTheDocument();
  });

  it('renders the redeem form', async () => {
    mockGetLoyalty.mockResolvedValue(sampleLoyalty);
    render(<LoyaltyPage />);
    expect(await screen.findByText('Points to Redeem')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Redeem Points' })).toBeInTheDocument();
  });

  it('redeems points successfully', async () => {
    mockGetLoyalty.mockResolvedValue(sampleLoyalty);
    mockRedeemPoints.mockResolvedValue({ ...sampleLoyalty, redeemed: 1500, balance: 3500 });
    const user = userEvent.setup();
    render(<LoyaltyPage />);
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Redeem Points' })).toBeInTheDocument();
    });
    const input = screen.getByPlaceholderText('Up to 4,000');
    await user.type(input, '500');
    await user.click(screen.getByRole('button', { name: 'Redeem Points' }));
    expect(mockRedeemPoints).toHaveBeenCalledWith({ points: 500 });
    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith('500 points redeemed successfully!');
    });
  });

  it('shows discount value preview when entering points', async () => {
    mockGetLoyalty.mockResolvedValue(sampleLoyalty);
    const user = userEvent.setup();
    render(<LoyaltyPage />);
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Redeem Points' })).toBeInTheDocument();
    });
    const input = screen.getByPlaceholderText('Up to 4,000');
    await user.type(input, '200');
    expect(screen.getByText('≈ $2.00 discount value')).toBeInTheDocument();
  });

  it('renders "How it works" section', async () => {
    mockGetLoyalty.mockResolvedValue(sampleLoyalty);
    render(<LoyaltyPage />);
    await waitFor(() => {
      expect(screen.getByText('How it works')).toBeInTheDocument();
    });
    expect(screen.getByText('Points never expire')).toBeInTheDocument();
  });
});
