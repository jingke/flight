import { useEffect, useState, useCallback } from 'react';
import toast from 'react-hot-toast';
import Spinner from '@/components/ui/Spinner';
import { fetchAllBookings, cancelBooking } from '@/services/admin.service';
import type { BookingDetail } from '@/types';
type Booking = BookingDetail;

const STATUS_BADGE: Record<string, string> = {
  confirmed: 'bg-green-100 text-green-800',
  cancelled: 'bg-red-100 text-red-800',
  pending: 'bg-yellow-100 text-yellow-800',
};

const PAYMENT_BADGE: Record<string, string> = {
  paid: 'bg-green-100 text-green-800',
  refunded: 'bg-blue-100 text-blue-800',
  pending: 'bg-yellow-100 text-yellow-800',
};

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
}

export default function ReservationsPage() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [cancelTarget, setCancelTarget] = useState<Booking | null>(null);

  const loadBookings = useCallback(async () => {
    try {
      const data = await fetchAllBookings();
      setBookings(data);
    } catch {
      /* handled by interceptor */
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { loadBookings(); }, [loadBookings]);

  async function handleCancel(): Promise<void> {
    if (!cancelTarget) return;
    try {
      await cancelBooking(cancelTarget.id);
      toast.success('Booking cancelled');
      setCancelTarget(null);
      await loadBookings();
    } catch {
      toast.error('Failed to cancel booking');
    }
  }

  const filteredBookings = bookings.filter((b) => {
    const matchesStatus = statusFilter === 'all' || b.status === statusFilter;
    const query = searchQuery.toLowerCase();
    const matchesSearch =
      !query ||
      b.flight_number?.toLowerCase().includes(query) ||
      b.departure_airport?.toLowerCase().includes(query) ||
      b.arrival_airport?.toLowerCase().includes(query) ||
      String(b.id).includes(query);
    return matchesStatus && matchesSearch;
  });

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner className="h-8 w-8" />
      </div>
    );
  }

  const statusCounts = bookings.reduce<Record<string, number>>((acc, b) => {
    acc[b.status] = (acc[b.status] ?? 0) + 1;
    return acc;
  }, {});

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">All Reservations</h1>
        <p className="mt-1 text-sm text-gray-500">
          View and manage all customer bookings ({bookings.length} total).
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="flex gap-2">
          {['all', 'confirmed', 'pending', 'cancelled'].map((s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`rounded-full px-3 py-1 text-xs font-medium capitalize transition-colors ${
                statusFilter === s
                  ? 'bg-primary-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {s} {s !== 'all' && statusCounts[s] ? `(${statusCounts[s]})` : ''}
            </button>
          ))}
        </div>
        <input
          type="text"
          placeholder="Search by flight, route, or ID…"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="ml-auto rounded-md border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
        />
      </div>

      <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">ID</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Flight</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Route</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Departure</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Total</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Payment</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Status</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Booked</th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {filteredBookings.length === 0 ? (
              <tr>
                <td colSpan={9} className="px-4 py-12 text-center text-sm text-gray-400">
                  No reservations found
                </td>
              </tr>
            ) : (
              filteredBookings.map((booking) => (
                <>
                  <tr key={booking.id} className="hover:bg-gray-50">
                    <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-gray-900">
                      #{booking.id}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                      {booking.flight_number ?? '—'}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                      {booking.departure_airport ?? '—'} → {booking.arrival_airport ?? '—'}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                      {booking.departure_time ? formatDateTime(booking.departure_time) : '—'}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-gray-900">
                      {formatCurrency(booking.total_price)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${PAYMENT_BADGE[booking.payment_status] ?? 'bg-gray-100 text-gray-800'}`}>
                        {booking.payment_status}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[booking.status] ?? 'bg-gray-100 text-gray-800'}`}>
                        {booking.status}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-500">
                      {formatDate(booking.created_at)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-sm">
                      <button
                        onClick={() => setExpandedId(expandedId === booking.id ? null : booking.id)}
                        className="mr-2 text-primary-600 hover:text-primary-800"
                      >
                        {expandedId === booking.id ? 'Hide' : 'Details'}
                      </button>
                      {booking.status !== 'cancelled' && (
                        <button
                          onClick={() => setCancelTarget(booking)}
                          className="text-red-600 hover:text-red-800"
                        >
                          Cancel
                        </button>
                      )}
                    </td>
                  </tr>
                  {expandedId === booking.id && (
                    <tr key={`${booking.id}-detail`}>
                      <td colSpan={9} className="bg-gray-50 px-8 py-4">
                        <h4 className="text-sm font-medium text-gray-700">
                          Passengers ({booking.passengers.length})
                        </h4>
                        {booking.passengers.length === 0 ? (
                          <p className="mt-1 text-sm text-gray-400">No passengers</p>
                        ) : (
                          <div className="mt-2 grid grid-cols-3 gap-3">
                            {booking.passengers.map((p) => (
                              <div key={p.id} className="rounded-md border border-gray-200 bg-white px-3 py-2">
                                <p className="text-sm font-medium text-gray-900">{p.name}</p>
                                <p className="text-xs text-gray-500">{p.email}</p>
                                {p.seat_assignment && (
                                  <p className="mt-1 text-xs text-gray-400">Seat: {p.seat_assignment}</p>
                                )}
                              </div>
                            ))}
                          </div>
                        )}
                      </td>
                    </tr>
                  )}
                </>
              ))
            )}
          </tbody>
        </table>
      </div>

      {cancelTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
            <h2 className="text-lg font-semibold text-gray-900">Cancel Booking</h2>
            <p className="mt-2 text-sm text-gray-600">
              Cancel booking <strong>#{cancelTarget.id}</strong> for flight{' '}
              <strong>{cancelTarget.flight_number}</strong>?
            </p>
            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={() => setCancelTarget(null)}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                Keep
              </button>
              <button
                onClick={handleCancel}
                className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
              >
                Cancel Booking
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
