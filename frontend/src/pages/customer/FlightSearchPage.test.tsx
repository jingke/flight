import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import FlightSearchPage from './FlightSearchPage';
import type { Flight } from '@/types';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('@/services/flight.service', () => ({
  searchFlights: vi.fn(),
}));

vi.mock('@/services/airport.service', () => ({
  listAirports: vi.fn(),
}));

import * as flightService from '@/services/flight.service';
import * as airportService from '@/services/airport.service';

const mockSearchFlights = vi.mocked(flightService.searchFlights);
const mockListAirports = vi.mocked(airportService.listAirports);

function makeFlight(overrides: Partial<Flight> = {}): Flight {
  return {
    id: 1,
    flight_number: 'SK-100',
    departure_airport_id: 1,
    arrival_airport_id: 2,
    departure_time: '2026-03-15T08:00:00Z',
    arrival_time: '2026-03-15T11:30:00Z',
    price: 199,
    total_seats: 180,
    status: 'scheduled',
    departure_airport: { id: 1, code: 'JFK', name: 'JFK International', city: 'New York', country: 'US' },
    arrival_airport: { id: 2, code: 'LAX', name: 'Los Angeles International', city: 'Los Angeles', country: 'US' },
    available_seats: 42,
    ...overrides,
  };
}

describe('FlightSearchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockListAirports.mockResolvedValue([]);
    mockSearchFlights.mockResolvedValue([]);
  });

  it('renders the page heading', async () => {
    render(
      <MemoryRouter>
        <FlightSearchPage />
      </MemoryRouter>,
    );
    expect(screen.getByRole('heading', { name: 'Search Flights' })).toBeInTheDocument();
  });

  it('renders the search form fields', async () => {
    render(
      <MemoryRouter>
        <FlightSearchPage />
      </MemoryRouter>,
    );
    expect(screen.getByText('From')).toBeInTheDocument();
    expect(screen.getByText('To')).toBeInTheDocument();
    expect(screen.getByText('Date')).toBeInTheDocument();
    expect(screen.getByText('Min Price')).toBeInTheDocument();
    expect(screen.getByText('Max Price')).toBeInTheDocument();
  });

  it('calls searchFlights on initial load', async () => {
    mockSearchFlights.mockResolvedValue([]);
    render(
      <MemoryRouter>
        <FlightSearchPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(mockSearchFlights).toHaveBeenCalled();
    });
  });

  it('shows empty state when no flights are found', async () => {
    mockSearchFlights.mockResolvedValue([]);
    render(
      <MemoryRouter>
        <FlightSearchPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('No flights found')).toBeInTheDocument();
    });
    expect(screen.getByText('Try adjusting your search criteria.')).toBeInTheDocument();
  });

  it('renders flight results with details', async () => {
    mockSearchFlights.mockResolvedValue([makeFlight()]);
    render(
      <MemoryRouter>
        <FlightSearchPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('SK-100')).toBeInTheDocument();
    });
    expect(screen.getByText('scheduled')).toBeInTheDocument();
    expect(screen.getByText(/JFK.*→.*LAX/)).toBeInTheDocument();
    expect(screen.getByText('$199')).toBeInTheDocument();
    expect(screen.getByText('42 seats left')).toBeInTheDocument();
  });

  it('displays the correct flight count', async () => {
    mockSearchFlights.mockResolvedValue([makeFlight(), makeFlight({ id: 2, flight_number: 'SK-200' })]);
    render(
      <MemoryRouter>
        <FlightSearchPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('2 flights found')).toBeInTheDocument();
    });
  });

  it('uses singular text for a single flight', async () => {
    mockSearchFlights.mockResolvedValue([makeFlight()]);
    render(
      <MemoryRouter>
        <FlightSearchPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('1 flight found')).toBeInTheDocument();
    });
  });

  it('links each flight card to its detail page', async () => {
    mockSearchFlights.mockResolvedValue([makeFlight({ id: 77 })]);
    render(
      <MemoryRouter>
        <FlightSearchPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('SK-100')).toBeInTheDocument();
    });
    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/flights/77');
  });

  it('clears form fields when Clear button is clicked', async () => {
    mockListAirports.mockResolvedValue([
      { id: 1, code: 'JFK', name: 'JFK International', city: 'New York', country: 'US', latitude: 40.6, longitude: -73.7 },
    ]);
    mockSearchFlights.mockResolvedValue([]);
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <FlightSearchPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(mockSearchFlights).toHaveBeenCalled();
    });
    const minPriceInput = screen.getByPlaceholderText('$0');
    await user.type(minPriceInput, '100');
    expect(minPriceInput).toHaveValue(100);
    await user.click(screen.getByRole('button', { name: 'Clear' }));
    expect(minPriceInput).toHaveValue(null);
  });

  it('renders sort-by options', async () => {
    mockSearchFlights.mockResolvedValue([makeFlight()]);
    render(
      <MemoryRouter>
        <FlightSearchPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('Sort by:')).toBeInTheDocument();
    });
  });
});
