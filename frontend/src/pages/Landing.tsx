import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as airportService from '@/services/airport.service';
import type { Airport } from '@/types';

export default function Landing() {
  const navigate = useNavigate();
  const [airports, setAirports] = useState<Airport[]>([]);
  const [origin, setOrigin] = useState('');
  const [destination, setDestination] = useState('');
  const [date, setDate] = useState('');

  useEffect(() => {
    airportService.listAirports().then(setAirports).catch(() => {});
  }, []);

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    const params = new URLSearchParams();
    if (origin) params.set('origin', origin);
    if (destination) params.set('destination', destination);
    if (date) params.set('date', date);
    navigate(`/flights?${params.toString()}`);
  }

  return (
    <div className="flex flex-col">
      {/* Hero */}
      <div className="relative overflow-hidden bg-gradient-to-br from-primary-600 via-primary-700 to-primary-900 px-4 py-20 sm:py-28">
        <div className="absolute inset-0 opacity-10">
          <svg width="100%" height="100%"><defs><pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse"><path d="M 40 0 L 0 0 0 40" fill="none" stroke="white" strokeWidth="0.5"/></pattern></defs><rect width="100%" height="100%" fill="url(#grid)"/></svg>
        </div>
        <div className="relative mx-auto max-w-4xl text-center">
          <h1 className="text-4xl font-bold tracking-tight text-white sm:text-5xl lg:text-6xl">
            Your Journey Starts Here
          </h1>
          <p className="mx-auto mt-4 max-w-2xl text-lg text-primary-100">
            Search hundreds of destinations, compare prices, and book your next flight in seconds.
          </p>

          <form onSubmit={handleSearch} className="mx-auto mt-10 max-w-3xl rounded-2xl bg-white p-4 shadow-2xl sm:p-6">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-4">
              <div>
                <label className="mb-1 block text-left text-xs font-medium text-gray-500">From</label>
                <select
                  value={origin}
                  onChange={(e) => setOrigin(e.target.value)}
                  className="w-full rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5 text-sm text-gray-900 focus:border-primary-500 focus:bg-white focus:outline-none"
                >
                  <option value="">Any origin</option>
                  {airports.map((a) => (
                    <option key={a.id} value={a.code}>{a.code} — {a.city}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-left text-xs font-medium text-gray-500">To</label>
                <select
                  value={destination}
                  onChange={(e) => setDestination(e.target.value)}
                  className="w-full rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5 text-sm text-gray-900 focus:border-primary-500 focus:bg-white focus:outline-none"
                >
                  <option value="">Any destination</option>
                  {airports.map((a) => (
                    <option key={a.id} value={a.code}>{a.code} — {a.city}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-left text-xs font-medium text-gray-500">Date</label>
                <input
                  type="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  className="w-full rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5 text-sm text-gray-900 focus:border-primary-500 focus:bg-white focus:outline-none"
                />
              </div>
              <div className="flex items-end">
                <button
                  type="submit"
                  className="w-full rounded-lg bg-primary-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-primary-700 transition-colors"
                >
                  Search Flights
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>

      {/* Features */}
      <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="grid gap-8 sm:grid-cols-3">
          <div className="rounded-xl border border-gray-100 bg-white p-6 text-center shadow-sm">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-primary-100 text-primary-600">
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <h3 className="mt-4 text-lg font-semibold text-gray-900">Easy Search</h3>
            <p className="mt-2 text-sm text-gray-500">Find flights by destination, date, and budget with our powerful search tools.</p>
          </div>
          <div className="rounded-xl border border-gray-100 bg-white p-6 text-center shadow-sm">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-green-100 text-green-600">
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h3 className="mt-4 text-lg font-semibold text-gray-900">Seat Selection</h3>
            <p className="mt-2 text-sm text-gray-500">Choose your preferred seats with our interactive seat map before you book.</p>
          </div>
          <div className="rounded-xl border border-gray-100 bg-white p-6 text-center shadow-sm">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-amber-100 text-amber-600">
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
              </svg>
            </div>
            <h3 className="mt-4 text-lg font-semibold text-gray-900">Loyalty Rewards</h3>
            <p className="mt-2 text-sm text-gray-500">Earn points on every booking and redeem them for discounts on future flights.</p>
          </div>
        </div>

        <div className="mt-12 text-center">
          <Link
            to="/map"
            className="inline-flex items-center gap-2 text-sm font-medium text-primary-600 hover:text-primary-700 transition-colors"
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
            </svg>
            Explore our route map
          </Link>
        </div>
      </div>
    </div>
  );
}
