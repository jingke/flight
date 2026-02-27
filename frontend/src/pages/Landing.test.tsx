import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import Landing from './Landing';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('@/services/airport.service', () => ({
  listAirports: vi.fn(),
}));

import * as airportService from '@/services/airport.service';

const mockListAirports = vi.mocked(airportService.listAirports);

describe('Landing', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockListAirports.mockResolvedValue([
      { id: 1, code: 'JFK', name: 'JFK International', city: 'New York', country: 'US', latitude: 40.6, longitude: -73.7 },
      { id: 2, code: 'LAX', name: 'Los Angeles International', city: 'Los Angeles', country: 'US', latitude: 33.9, longitude: -118.4 },
    ]);
  });

  it('renders the hero heading', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>,
    );
    expect(screen.getByText('Your Journey Starts Here')).toBeInTheDocument();
  });

  it('renders the search form with From, To, and Date fields', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>,
    );
    expect(screen.getByText('From')).toBeInTheDocument();
    expect(screen.getByText('To')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Search Flights' })).toBeInTheDocument();
  });

  it('loads airports and populates select options', async () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getAllByText('JFK — New York')).toHaveLength(2);
      expect(screen.getAllByText('LAX — Los Angeles')).toHaveLength(2);
    });
  });

  it('renders the three feature cards', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>,
    );
    expect(screen.getByText('Easy Search')).toBeInTheDocument();
    expect(screen.getByText('Seat Selection')).toBeInTheDocument();
    expect(screen.getByText('Loyalty Rewards')).toBeInTheDocument();
  });

  it('renders the route map link', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>,
    );
    const link = screen.getByRole('link', { name: /explore our route map/i });
    expect(link).toHaveAttribute('href', '/map');
  });

  it('navigates to /flights with search params on form submit', async () => {
    mockListAirports.mockResolvedValue([
      { id: 1, code: 'JFK', name: 'JFK International', city: 'New York', country: 'US', latitude: 40.6, longitude: -73.7 },
    ]);
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getAllByText('JFK — New York')).toHaveLength(2);
    });
    await user.click(screen.getByRole('button', { name: 'Search Flights' }));
    expect(mockNavigate).toHaveBeenCalledWith('/flights?');
  });
});
