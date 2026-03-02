import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import Spinner from '@/components/ui/Spinner';
import * as bookingService from '@/services/booking.service';
import type { BookingDetail } from '@/types';

function formatDate(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
}

function formatTime(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

const STATUS_BADGE: Record<string, string> = {
  confirmed: 'bg-green-50 text-green-700 ring-green-600/20',
  pending: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  cancelled: 'bg-red-50 text-red-700 ring-red-600/20',
};

const PAYMENT_BADGE: Record<string, string> = {
  paid: 'bg-green-50 text-green-700',
  pending: 'bg-amber-50 text-amber-700',
  refunded: 'bg-gray-50 text-gray-600',
};

export default function BookingsPage() {
  const [bookings, setBookings] = useState<BookingDetail[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'confirmed' | 'cancelled'>('all');

  useEffect(() => {
    loadBookings();
  }, []);

  async function loadBookings() {
    setIsLoading(true);
    try {
      const data = await bookingService.listBookings();
      setBookings(data);
    } catch {
      toast.error('Failed to load bookings');
    } finally {
      setIsLoading(false);
    }
  }

  const filtered = filter === 'all' ? bookings : bookings.filter((b) => b.status === filter);

  if (isLoading) {
    return (
      <div className="flex justify-center py-24">
        <Spinner className="h-10 w-10" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">My Bookings</h1>
          <p className="mt-1 text-gray-500">{bookings.length} booking{bookings.length !== 1 ? 's' : ''} total</p>
        </div>
        <div className="flex gap-2">
          {(['all', 'confirmed', 'cancelled'] as const).map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
                filter === f
                  ? 'bg-primary-600 text-white shadow-sm'
                  : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
              }`}
            >
              {f.charAt(0).toUpperCase() + f.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="rounded-xl border-2 border-dashed border-gray-200 py-16 text-center">
          <svg className="mx-auto h-12 w-12 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          <h3 className="mt-3 text-sm font-semibold text-gray-900">
            {filter === 'all' ? 'No bookings yet' : `No ${filter} bookings`}
          </h3>
          <p className="mt-1 text-sm text-gray-500">
            {filter === 'all'
              ? 'Your bookings will appear here after you book a flight.'
              : 'Try a different filter to see more results.'}
          </p>
          {filter === 'all' && (
            <Link to="/flights" className="mt-4 inline-block text-sm font-medium text-primary-600 hover:underline">
              Search flights
            </Link>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map((booking) => (
            <Link
              key={booking.id}
              to={`/bookings/${booking.id}`}
              className="group block rounded-xl border border-gray-200 bg-white p-5 shadow-sm hover:border-primary-300 hover:shadow-md transition-all"
            >
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-center gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary-50 text-primary-600 font-bold text-sm">
                    #{booking.id}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-gray-900">{booking.flight_number ?? `Flight #${booking.flight_id}`}</span>
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${STATUS_BADGE[booking.status] ?? STATUS_BADGE.pending}`}>
                        {booking.status}
                      </span>
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${PAYMENT_BADGE[booking.payment_status] ?? PAYMENT_BADGE.pending}`}>
                        {booking.payment_status}
                      </span>
                    </div>
                    <p className="mt-0.5 text-sm text-gray-500">
                      {booking.departure_airport ?? '—'} → {booking.arrival_airport ?? '—'}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-6">
                  <div className="text-sm">
                    <p className="text-gray-400">Departure</p>
                    <p className="font-medium text-gray-900">{formatDate(booking.departure_time)}</p>
                    <p className="text-gray-500">{formatTime(booking.departure_time)}</p>
                  </div>
                  <div className="text-sm">
                    <p className="text-gray-400">Passengers</p>
                    <p className="font-medium text-gray-900">{booking.passengers.length}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-bold text-gray-900">${booking.total_price.toFixed(2)}</p>
                    <p className="text-xs text-gray-400">
                      Booked {formatDate(booking.created_at)}
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
  );
}
