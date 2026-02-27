import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import Spinner from '@/components/ui/Spinner';
import * as complaintService from '@/services/complaint.service';
import * as bookingService from '@/services/booking.service';
import type { Complaint, ComplaintCreate, BookingDetail } from '@/types';

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
}

const STATUS_BADGE: Record<string, { bg: string; text: string }> = {
  open: { bg: 'bg-blue-50', text: 'text-blue-700' },
  in_progress: { bg: 'bg-amber-50', text: 'text-amber-700' },
  resolved: { bg: 'bg-green-50', text: 'text-green-700' },
  closed: { bg: 'bg-gray-100', text: 'text-gray-600' },
};

export default function ComplaintsPage() {
  const [searchParams] = useSearchParams();
  const prefilledBookingId = searchParams.get('booking');
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [bookings, setBookings] = useState<BookingDetail[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showForm, setShowForm] = useState(!!prefilledBookingId);
  const [bookingId, setBookingId] = useState(prefilledBookingId ?? '');
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setIsLoading(true);
    try {
      const [c, b] = await Promise.all([
        complaintService.listComplaints(),
        bookingService.listBookings().catch(() => [] as BookingDetail[]),
      ]);
      setComplaints(c);
      setBookings(b);
    } catch {
      toast.error('Failed to load complaints');
    } finally {
      setIsLoading(false);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!subject.trim() || !description.trim()) {
      toast.error('Please fill in subject and description');
      return;
    }
    setIsSubmitting(true);
    try {
      const payload: ComplaintCreate = {
        subject: subject.trim(),
        description: description.trim(),
        booking_id: bookingId ? Number(bookingId) : null,
      };
      await complaintService.createComplaint(payload);
      toast.success('Complaint submitted successfully');
      setShowForm(false);
      setSubject('');
      setDescription('');
      setBookingId('');
      await loadData();
    } catch {
      toast.error('Failed to submit complaint');
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-24">
        <Spinner className="h-10 w-10" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Complaints</h1>
          <p className="mt-1 text-gray-500">Submit and track your complaints.</p>
        </div>
        <button
          type="button"
          onClick={() => setShowForm(!showForm)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-primary-700 transition-colors"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={showForm ? "M6 18L18 6M6 6l12 12" : "M12 4v16m8-8H4"} />
          </svg>
          {showForm ? 'Close' : 'New Complaint'}
        </button>
      </div>

      {/* New Complaint Form */}
      {showForm && (
        <form onSubmit={handleSubmit} className="mb-8 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">Submit a Complaint</h2>
          <div className="space-y-4">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-gray-700">Related Booking (optional)</label>
              <select
                value={bookingId}
                onChange={(e) => setBookingId(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm focus:border-primary-500 focus:outline-none"
              >
                <option value="">No specific booking</option>
                {bookings.map((b) => (
                  <option key={b.id} value={b.id}>
                    Booking #{b.id} — {b.flight_number ?? `Flight #${b.flight_id}`}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-gray-700">Subject</label>
              <input
                type="text"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                placeholder="Brief summary of your complaint"
                className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm focus:border-primary-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-gray-700">Description</label>
              <textarea
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Provide details about your issue..."
                className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm focus:border-primary-500 focus:outline-none"
              />
            </div>
          </div>
          <div className="mt-4 flex justify-end">
            <button
              type="submit"
              disabled={isSubmitting}
              className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-50 transition-colors"
            >
              {isSubmitting && <Spinner className="h-4 w-4" />}
              Submit Complaint
            </button>
          </div>
        </form>
      )}

      {/* Complaints List */}
      {complaints.length === 0 ? (
        <div className="rounded-xl border-2 border-dashed border-gray-200 py-16 text-center">
          <svg className="mx-auto h-12 w-12 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
          </svg>
          <h3 className="mt-3 text-sm font-semibold text-gray-900">No complaints</h3>
          <p className="mt-1 text-sm text-gray-500">You haven't submitted any complaints yet.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {complaints.map((complaint) => {
            const badge = STATUS_BADGE[complaint.status] ?? STATUS_BADGE.open;
            return (
              <div
                key={complaint.id}
                className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="font-semibold text-gray-900">{complaint.subject}</h3>
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${badge.bg} ${badge.text}`}>
                        {complaint.status.replace('_', ' ')}
                      </span>
                    </div>
                    {complaint.booking_id && (
                      <p className="mt-0.5 text-xs text-gray-400">Booking #{complaint.booking_id}</p>
                    )}
                    <p className="mt-2 text-sm text-gray-600">{complaint.description}</p>
                    {complaint.admin_response && (
                      <div className="mt-3 rounded-lg bg-blue-50 p-3">
                        <p className="text-xs font-semibold text-blue-800">Admin Response</p>
                        <p className="mt-0.5 text-sm text-blue-700">{complaint.admin_response}</p>
                      </div>
                    )}
                  </div>
                  <span className="ml-4 text-xs text-gray-400 whitespace-nowrap">{formatDate(complaint.created_at)}</span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
