import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import Spinner from '@/components/ui/Spinner';
import * as bookingService from '@/services/booking.service';
import * as modificationService from '@/services/modification.service';
import type { BookingDetail, ModificationCreate } from '@/types';

function formatDateTime(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString([], {
    month: 'short', day: 'numeric', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

const STATUS_BADGE: Record<string, string> = {
  confirmed: 'bg-green-50 text-green-700 ring-green-600/20',
  pending: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  cancelled: 'bg-red-50 text-red-700 ring-red-600/20',
};

export default function BookingDetailPage() {
  const { bookingId } = useParams<{ bookingId: string }>();
  const navigate = useNavigate();
  const [booking, setBooking] = useState<BookingDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCancelling, setIsCancelling] = useState(false);
  const [showModifyModal, setShowModifyModal] = useState(false);
  const [modType, setModType] = useState('date_change');
  const [modDetails, setModDetails] = useState('');
  const [isSubmittingMod, setIsSubmittingMod] = useState(false);

  useEffect(() => {
    if (!bookingId) return;
    loadBooking();
  }, [bookingId]);

  async function loadBooking() {
    setIsLoading(true);
    try {
      const data = await bookingService.getBooking(Number(bookingId));
      setBooking(data);
    } catch {
      toast.error('Failed to load booking');
    } finally {
      setIsLoading(false);
    }
  }

  async function handleCancel() {
    if (!booking || !confirm('Are you sure you want to cancel this booking? This action cannot be undone.')) return;
    setIsCancelling(true);
    try {
      await bookingService.cancelBooking(booking.id);
      toast.success('Booking cancelled successfully');
      await loadBooking();
    } catch {
      toast.error('Failed to cancel booking');
    } finally {
      setIsCancelling(false);
    }
  }

  async function handleSubmitModification() {
    if (!booking || !modDetails.trim()) {
      toast.error('Please describe the modification you need');
      return;
    }
    setIsSubmittingMod(true);
    try {
      const payload: ModificationCreate = {
        booking_id: booking.id,
        type: modType,
        details: modDetails.trim(),
      };
      await modificationService.createModification(payload);
      toast.success('Modification request submitted');
      setShowModifyModal(false);
      setModDetails('');
    } catch {
      toast.error('Failed to submit modification request');
    } finally {
      setIsSubmittingMod(false);
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-24">
        <Spinner className="h-10 w-10" />
      </div>
    );
  }

  if (!booking) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16 text-center">
        <h2 className="text-xl font-semibold text-gray-900">Booking not found</h2>
        <Link to="/bookings" className="mt-4 inline-block text-primary-600 hover:underline">Back to bookings</Link>
      </div>
    );
  }

  const isCancelled = booking.status === 'cancelled';

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <Link to="/bookings" className="mb-6 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-primary-600 transition-colors">
        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
        Back to bookings
      </Link>

      {/* Header */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-3xl font-bold text-gray-900">Booking #{booking.id}</h1>
            <span className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium ring-1 ring-inset ${STATUS_BADGE[booking.status] ?? STATUS_BADGE.pending}`}>
              {booking.status}
            </span>
          </div>
          <p className="mt-1 text-sm text-gray-500">Booked on {formatDateTime(booking.created_at)}</p>
        </div>
        <p className="text-3xl font-bold text-gray-900">${booking.total_price.toFixed(2)}</p>
      </div>

      {/* Flight Info */}
      <div className="mb-6 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-gray-400">Flight Information</h2>
        <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
          <div className="flex-1">
            <p className="text-lg font-semibold text-gray-900">{booking.flight_number ?? `Flight #${booking.flight_id}`}</p>
            <p className="mt-1 text-gray-600">
              {booking.departure_airport ?? '—'} → {booking.arrival_airport ?? '—'}
            </p>
          </div>
          <div className="grid grid-cols-2 gap-6">
            <div>
              <p className="text-xs font-medium uppercase text-gray-400">Departure</p>
              <p className="mt-0.5 text-sm font-medium text-gray-900">{formatDateTime(booking.departure_time)}</p>
            </div>
            <div>
              <p className="text-xs font-medium uppercase text-gray-400">Arrival</p>
              <p className="mt-0.5 text-sm font-medium text-gray-900">{formatDateTime(booking.arrival_time)}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Passengers */}
      <div className="mb-6 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-gray-400">
          Passengers ({booking.passengers.length})
        </h2>
        <div className="divide-y divide-gray-100">
          {booking.passengers.map((p, i) => (
            <div key={p.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-50 text-sm font-semibold text-primary-600">
                  {i + 1}
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-900">{p.name}</p>
                  <p className="text-xs text-gray-500">{p.email}</p>
                </div>
              </div>
              <div className="text-right">
                {p.seat_assignment ? (
                  <span className="inline-flex items-center gap-1 rounded-lg bg-primary-50 px-3 py-1 text-sm font-medium text-primary-700">
                    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                    Seat {p.seat_assignment}
                  </span>
                ) : (
                  <span className="text-xs text-gray-400">No seat assigned</span>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Payment */}
      <div className="mb-6 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-gray-400">Payment</h2>
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm text-gray-600">
              {booking.passengers.length} passenger{booking.passengers.length !== 1 ? 's' : ''}
            </p>
            <p className="mt-1 text-lg font-bold text-gray-900">${booking.total_price.toFixed(2)}</p>
          </div>
          <span className={`rounded-full px-3 py-1 text-sm font-medium ${
            booking.payment_status === 'paid' ? 'bg-green-50 text-green-700' :
            booking.payment_status === 'refunded' ? 'bg-gray-100 text-gray-600' :
            'bg-amber-50 text-amber-700'
          }`}>
            {booking.payment_status}
          </span>
        </div>
      </div>

      {/* Actions */}
      {!isCancelled && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-gray-400">Actions</h2>
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => setShowModifyModal(true)}
              className="rounded-lg border border-gray-300 px-5 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
            >
              Request Modification
            </button>
            <button
              type="button"
              onClick={handleCancel}
              disabled={isCancelling}
              className="rounded-lg border border-red-300 px-5 py-2.5 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-50 transition-colors"
            >
              {isCancelling ? 'Cancelling...' : 'Cancel Booking'}
            </button>
            <button
              type="button"
              onClick={() => navigate(`/complaints?booking=${booking.id}`)}
              className="rounded-lg border border-gray-300 px-5 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
            >
              File Complaint
            </button>
          </div>
        </div>
      )}

      {/* Modification Modal */}
      {showModifyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
            <h3 className="text-lg font-semibold text-gray-900">Request Modification</h3>
            <p className="mt-1 text-sm text-gray-500">Describe the changes you need for booking #{booking.id}.</p>
            <div className="mt-4 space-y-3">
              <div>
                <label className="mb-1.5 block text-sm font-medium text-gray-700">Type</label>
                <select
                  value={modType}
                  onChange={(e) => setModType(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm focus:border-primary-500 focus:outline-none"
                >
                  <option value="date_change">Date Change</option>
                  <option value="seat_change">Seat Change</option>
                  <option value="passenger_change">Passenger Change</option>
                  <option value="other">Other</option>
                </select>
              </div>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-gray-700">Details</label>
                <textarea
                  rows={4}
                  value={modDetails}
                  onChange={(e) => setModDetails(e.target.value)}
                  placeholder="Describe the changes you need..."
                  className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm focus:border-primary-500 focus:outline-none"
                />
              </div>
            </div>
            <div className="mt-5 flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setShowModifyModal(false)}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSubmitModification}
                disabled={isSubmittingMod}
                className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-50"
              >
                {isSubmittingMod ? 'Submitting...' : 'Submit Request'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
