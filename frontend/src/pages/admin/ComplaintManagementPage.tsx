import { useEffect, useState, useCallback } from 'react';
import toast from 'react-hot-toast';
import Spinner from '@/components/ui/Spinner';
import { fetchAllComplaints, updateComplaint } from '@/services/admin.service';
import type { Complaint } from '@/types';

const COMPLAINT_STATUSES = ['open', 'in_progress', 'resolved', 'closed'] as const;

const STATUS_BADGE: Record<string, string> = {
  open: 'bg-red-100 text-red-800',
  in_progress: 'bg-yellow-100 text-yellow-800',
  resolved: 'bg-green-100 text-green-800',
  closed: 'bg-gray-100 text-gray-800',
};

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export default function ComplaintManagementPage() {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('all');
  const [selectedComplaint, setSelectedComplaint] = useState<Complaint | null>(null);
  const [responseText, setResponseText] = useState('');
  const [responseStatus, setResponseStatus] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const loadComplaints = useCallback(async () => {
    try {
      const data = await fetchAllComplaints();
      setComplaints(data);
    } catch {
      /* handled by interceptor */
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { loadComplaints(); }, [loadComplaints]);

  function openRespond(complaint: Complaint): void {
    setSelectedComplaint(complaint);
    setResponseText(complaint.admin_response ?? '');
    setResponseStatus(complaint.status);
  }

  function closeModal(): void {
    setSelectedComplaint(null);
    setResponseText('');
    setResponseStatus('');
  }

  async function handleSubmitResponse(): Promise<void> {
    if (!selectedComplaint) return;
    setIsSaving(true);
    try {
      await updateComplaint(selectedComplaint.id, {
        status: responseStatus,
        admin_response: responseText || undefined,
      });
      toast.success('Complaint updated');
      closeModal();
      await loadComplaints();
    } catch {
      toast.error('Failed to update complaint');
    } finally {
      setIsSaving(false);
    }
  }

  const filteredComplaints = statusFilter === 'all'
    ? complaints
    : complaints.filter((c) => c.status === statusFilter);

  const statusCounts = complaints.reduce<Record<string, number>>((acc, c) => {
    acc[c.status] = (acc[c.status] ?? 0) + 1;
    return acc;
  }, {});

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner className="h-8 w-8" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Complaint Management</h1>
        <p className="mt-1 text-sm text-gray-500">
          Review and respond to customer complaints ({complaints.length} total).
        </p>
      </div>

      <div className="flex gap-2">
        {['all', ...COMPLAINT_STATUSES].map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`rounded-full px-3 py-1 text-xs font-medium capitalize transition-colors ${
              statusFilter === s
                ? 'bg-primary-600 text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            {s.replace('_', ' ')} {s !== 'all' && statusCounts[s] ? `(${statusCounts[s]})` : ''}
          </button>
        ))}
      </div>

      <div className="space-y-3">
        {filteredComplaints.length === 0 ? (
          <div className="rounded-lg border border-gray-200 bg-white px-6 py-12 text-center text-sm text-gray-400">
            No complaints found
          </div>
        ) : (
          filteredComplaints.map((complaint) => (
            <div
              key={complaint.id}
              className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm transition-shadow hover:shadow-md"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-semibold text-gray-900">{complaint.subject}</h3>
                    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[complaint.status]}`}>
                      {complaint.status.replace('_', ' ')}
                    </span>
                  </div>
                  <p className="mt-1 text-sm text-gray-600">{complaint.description}</p>
                  <div className="mt-2 flex items-center gap-4 text-xs text-gray-400">
                    <span>User #{complaint.user_id}</span>
                    {complaint.booking_id && <span>Booking #{complaint.booking_id}</span>}
                    <span>{formatDate(complaint.created_at)}</span>
                  </div>
                  {complaint.admin_response && (
                    <div className="mt-3 rounded-md border-l-4 border-primary-400 bg-primary-50 px-4 py-2">
                      <p className="text-xs font-medium text-primary-700">Admin Response</p>
                      <p className="mt-0.5 text-sm text-primary-900">{complaint.admin_response}</p>
                    </div>
                  )}
                </div>
                <button
                  onClick={() => openRespond(complaint)}
                  className="shrink-0 rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
                >
                  {complaint.admin_response ? 'Update' : 'Respond'}
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {selectedComplaint && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
            <h2 className="text-lg font-semibold text-gray-900">Respond to Complaint</h2>
            <div className="mt-3 rounded-md bg-gray-50 p-3">
              <p className="text-sm font-medium text-gray-700">{selectedComplaint.subject}</p>
              <p className="mt-1 text-sm text-gray-600">{selectedComplaint.description}</p>
            </div>
            <div className="mt-4 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">Status</label>
                <select
                  value={responseStatus}
                  onChange={(e) => setResponseStatus(e.target.value)}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                >
                  {COMPLAINT_STATUSES.map((s) => (
                    <option key={s} value={s}>{s.replace('_', ' ')}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Admin Response</label>
                <textarea
                  value={responseText}
                  onChange={(e) => setResponseText(e.target.value)}
                  rows={4}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                  placeholder="Type your response to the customer…"
                />
              </div>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={closeModal}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleSubmitResponse}
                disabled={isSaving}
                className="rounded-md bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-50"
              >
                {isSaving ? 'Saving…' : 'Submit Response'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
