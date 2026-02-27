import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import Spinner from '@/components/ui/Spinner';
import * as flightService from '@/services/flight.service';
import * as airportService from '@/services/airport.service';
import type { Flight, Airport, FlightSearchParams } from '@/types';

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
}

function getDuration(dep: string, arr: string): string {
  const ms = new Date(arr).getTime() - new Date(dep).getTime();
  const h = Math.floor(ms / 3_600_000);
  const m = Math.floor((ms % 3_600_000) / 60_000);
  return `${h}h ${m}m`;
}

const STATUS_STYLES: Record<string, string> = {
  scheduled: 'bg-green-50 text-green-700 ring-green-600/20',
  delayed: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  cancelled: 'bg-red-50 text-red-700 ring-red-600/20',
  completed: 'bg-gray-50 text-gray-600 ring-gray-500/20',
};

export default function FlightSearchPage() {
  const [airports, setAirports] = useState<Airport[]>([]);
  const [flights, setFlights] = useState<Flight[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [origin, setOrigin] = useState('');
  const [destination, setDestination] = useState('');
  const [date, setDate] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [sortBy, setSortBy] = useState<'price' | 'departure' | 'duration'>('price');

  useEffect(() => {
    airportService.listAirports().then(setAirports).catch(() => {});
    handleSearch();
  }, []);

  async function handleSearch(e?: React.FormEvent) {
    e?.preventDefault();
    setIsLoading(true);
    setHasSearched(true);
    try {
      const params: FlightSearchParams = {};
      if (origin) params.origin = origin;
      if (destination) params.destination = destination;
      if (date) params.date = date;
      if (minPrice) params.min_price = Number(minPrice);
      if (maxPrice) params.max_price = Number(maxPrice);
      const results = await flightService.searchFlights(params);
      setFlights(results);
    } catch {
      toast.error('Failed to search flights');
    } finally {
      setIsLoading(false);
    }
  }

  function handleClear() {
    setOrigin('');
    setDestination('');
    setDate('');
    setMinPrice('');
    setMaxPrice('');
  }

  const sorted = [...flights].sort((a, b) => {
    if (sortBy === 'price') return a.price - b.price;
    if (sortBy === 'departure') return new Date(a.departure_time).getTime() - new Date(b.departure_time).getTime();
    const durA = new Date(a.arrival_time).getTime() - new Date(a.departure_time).getTime();
    const durB = new Date(b.arrival_time).getTime() - new Date(b.departure_time).getTime();
    return durA - durB;
  });

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Search Flights</h1>
        <p className="mt-1 text-gray-500">Find and compare available flights across destinations.</p>
      </div>

      <form onSubmit={handleSearch} className="mb-8 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-gray-700">From</label>
            <select
              value={origin}
              onChange={(e) => setOrigin(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 focus:outline-none"
            >
              <option value="">Any origin</option>
              {airports.map((a) => (
                <option key={a.id} value={a.code}>{a.code} — {a.city}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-gray-700">To</label>
            <select
              value={destination}
              onChange={(e) => setDestination(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 focus:outline-none"
            >
              <option value="">Any destination</option>
              {airports.map((a) => (
                <option key={a.id} value={a.code}>{a.code} — {a.city}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-gray-700">Date</label>
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 focus:outline-none"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-gray-700">Min Price</label>
            <input
              type="number"
              min="0"
              placeholder="$0"
              value={minPrice}
              onChange={(e) => setMinPrice(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 focus:outline-none"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-gray-700">Max Price</label>
            <input
              type="number"
              min="0"
              placeholder="No limit"
              value={maxPrice}
              onChange={(e) => setMaxPrice(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 focus:outline-none"
            />
          </div>
        </div>
        <div className="mt-4 flex items-center gap-3">
          <button
            type="submit"
            disabled={isLoading}
            className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-primary-700 disabled:opacity-50 transition-colors"
          >
            {isLoading && <Spinner className="h-4 w-4" />}
            Search Flights
          </button>
          <button
            type="button"
            onClick={handleClear}
            className="rounded-lg border border-gray-300 px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
          >
            Clear
          </button>
        </div>
      </form>

      {hasSearched && (
        <div>
          <div className="mb-4 flex items-center justify-between">
            <p className="text-sm text-gray-600">
              {flights.length} flight{flights.length !== 1 ? 's' : ''} found
            </p>
            <div className="flex items-center gap-2">
              <span className="text-sm text-gray-500">Sort by:</span>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as typeof sortBy)}
                className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-primary-500 focus:outline-none"
              >
                <option value="price">Price</option>
                <option value="departure">Departure Time</option>
                <option value="duration">Duration</option>
              </select>
            </div>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-16">
              <Spinner className="h-8 w-8" />
            </div>
          ) : flights.length === 0 ? (
            <div className="rounded-xl border-2 border-dashed border-gray-200 py-16 text-center">
              <svg className="mx-auto h-12 w-12 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" />
              </svg>
              <h3 className="mt-3 text-sm font-semibold text-gray-900">No flights found</h3>
              <p className="mt-1 text-sm text-gray-500">Try adjusting your search criteria.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {sorted.map((flight) => (
                <Link
                  key={flight.id}
                  to={`/flights/${flight.id}`}
                  className="group block rounded-xl border border-gray-200 bg-white p-5 shadow-sm hover:border-primary-300 hover:shadow-md transition-all"
                >
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex items-center gap-5">
                      <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary-50 text-primary-600">
                        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" />
                        </svg>
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-gray-900">{flight.flight_number}</span>
                          <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${STATUS_STYLES[flight.status] ?? STATUS_STYLES.scheduled}`}>
                            {flight.status}
                          </span>
                        </div>
                        <p className="mt-0.5 text-sm text-gray-500">
                          {flight.departure_airport?.code ?? '—'} ({flight.departure_airport?.city})
                          {' → '}
                          {flight.arrival_airport?.code ?? '—'} ({flight.arrival_airport?.city})
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-8 sm:gap-10">
                      <div className="text-center">
                        <p className="text-sm font-medium text-gray-900">{formatTime(flight.departure_time)}</p>
                        <p className="text-xs text-gray-400">{formatDate(flight.departure_time)}</p>
                      </div>
                      <div className="flex flex-col items-center">
                        <span className="text-xs text-gray-400">{getDuration(flight.departure_time, flight.arrival_time)}</span>
                        <div className="mt-1 flex items-center gap-1">
                          <div className="h-px w-12 bg-gray-300" />
                          <svg className="h-3 w-3 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z" />
                          </svg>
                          <div className="h-px w-12 bg-gray-300" />
                        </div>
                      </div>
                      <div className="text-center">
                        <p className="text-sm font-medium text-gray-900">{formatTime(flight.arrival_time)}</p>
                        <p className="text-xs text-gray-400">{formatDate(flight.arrival_time)}</p>
                      </div>

                      <div className="text-right">
                        <p className="text-xl font-bold text-gray-900">${flight.price.toFixed(0)}</p>
                        <p className="text-xs text-gray-400">
                          {flight.available_seats != null ? `${flight.available_seats} seats left` : '—'}
                        </p>
                      </div>

                      <svg className="h-5 w-5 text-gray-300 group-hover:text-primary-600 transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                      </svg>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
