import { useEffect, useState, useCallback } from 'react';
import toast from 'react-hot-toast';
import Spinner from '@/components/ui/Spinner';
import {
  fetchFlights,
  fetchAirports,
  createFlight,
  updateFlight,
  deleteFlight,
} from '@/services/admin.service';
import type { Flight, FlightCreate, AirportBrief } from '@/types';

type ModalMode = 'closed' | 'create' | 'edit';

const FLIGHT_STATUSES = ['scheduled', 'delayed', 'cancelled', 'completed'] as const;

const STATUS_BADGE: Record<string, string> = {
  scheduled: 'bg-blue-100 text-blue-800',
  delayed: 'bg-yellow-100 text-yellow-800',
  cancelled: 'bg-red-100 text-red-800',
  completed: 'bg-green-100 text-green-800',
};

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

const EMPTY_FORM: FlightCreate = {
  flight_number: '',
  departure_airport_id: 0,
  arrival_airport_id: 0,
  departure_time: '',
  arrival_time: '',
  price: 0,
  total_seats: 0,
};

export default function FlightManagementPage() {
  const [flights, setFlights] = useState<Flight[]>([]);
  const [airports, setAirports] = useState<AirportBrief[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [modalMode, setModalMode] = useState<ModalMode>('closed');
  const [editingFlight, setEditingFlight] = useState<Flight | null>(null);
  const [form, setForm] = useState<FlightCreate>(EMPTY_FORM);
  const [editStatus, setEditStatus] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Flight | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('all');

  const loadData = useCallback(async () => {
    try {
      const [flightsData, airportsData] = await Promise.all([fetchFlights(), fetchAirports()]);
      setFlights(flightsData);
      setAirports(airportsData);
    } catch {
      /* handled by interceptor */
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  function openCreate(): void {
    setForm(EMPTY_FORM);
    setModalMode('create');
    setEditingFlight(null);
  }

  function openEdit(flight: Flight): void {
    setEditingFlight(flight);
    setForm({
      flight_number: flight.flight_number,
      departure_airport_id: flight.departure_airport_id,
      arrival_airport_id: flight.arrival_airport_id,
      departure_time: flight.departure_time.slice(0, 16),
      arrival_time: flight.arrival_time.slice(0, 16),
      price: flight.price,
      total_seats: flight.total_seats,
    });
    setEditStatus(flight.status);
    setModalMode('edit');
  }

  function closeModal(): void {
    setModalMode('closed');
    setEditingFlight(null);
  }

  async function handleSave(): Promise<void> {
    if (!form.flight_number || !form.departure_airport_id || !form.arrival_airport_id) {
      toast.error('Please fill in all required fields');
      return;
    }
    setIsSaving(true);
    try {
      if (modalMode === 'create') {
        await createFlight(form);
        toast.success('Flight created');
      } else if (editingFlight) {
        await updateFlight(editingFlight.id, {
          departure_time: form.departure_time,
          arrival_time: form.arrival_time,
          price: form.price,
          status: editStatus,
        });
        toast.success('Flight updated');
      }
      closeModal();
      await loadData();
    } catch {
      toast.error('Failed to save flight');
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(): Promise<void> {
    if (!deleteTarget) return;
    try {
      await deleteFlight(deleteTarget.id);
      toast.success('Flight deleted');
      setDeleteTarget(null);
      await loadData();
    } catch {
      toast.error('Failed to delete flight');
    }
  }

  const filteredFlights = statusFilter === 'all'
    ? flights
    : flights.filter((f) => f.status === statusFilter);

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner className="h-8 w-8" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Flight Management</h1>
          <p className="mt-1 text-sm text-gray-500">Create, edit, and manage flights.</p>
        </div>
        <button
          onClick={openCreate}
          className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-primary-700"
        >
          + New Flight
        </button>
      </div>

      <div className="flex gap-2">
        {['all', ...FLIGHT_STATUSES].map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`rounded-full px-3 py-1 text-xs font-medium capitalize transition-colors ${
              statusFilter === s
                ? 'bg-primary-600 text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            {s}
          </button>
        ))}
      </div>

      <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Flight</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Route</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Departure</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Arrival</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Price</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Seats</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Status</th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {filteredFlights.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-4 py-12 text-center text-sm text-gray-400">
                  No flights found
                </td>
              </tr>
            ) : (
              filteredFlights.map((flight) => (
                <tr key={flight.id} className="hover:bg-gray-50">
                  <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-gray-900">
                    {flight.flight_number}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                    {flight.departure_airport?.code ?? '—'} → {flight.arrival_airport?.code ?? '—'}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                    {formatDateTime(flight.departure_time)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                    {formatDateTime(flight.arrival_time)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                    {formatCurrency(flight.price)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                    {flight.available_seats ?? '—'} / {flight.total_seats}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[flight.status] ?? 'bg-gray-100 text-gray-800'}`}>
                      {flight.status}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right text-sm">
                    <button
                      onClick={() => openEdit(flight)}
                      className="mr-2 text-primary-600 hover:text-primary-800"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => setDeleteTarget(flight)}
                      className="text-red-600 hover:text-red-800"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Create / Edit Modal */}
      {modalMode !== 'closed' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
            <h2 className="text-lg font-semibold text-gray-900">
              {modalMode === 'create' ? 'Create Flight' : 'Edit Flight'}
            </h2>
            <div className="mt-4 grid grid-cols-2 gap-4">
              <div className="col-span-2">
                <label className="block text-sm font-medium text-gray-700">Flight Number</label>
                <input
                  type="text"
                  value={form.flight_number}
                  onChange={(e) => setForm({ ...form, flight_number: e.target.value })}
                  disabled={modalMode === 'edit'}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 disabled:bg-gray-100"
                  placeholder="e.g. AA100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Departure Airport</label>
                <select
                  value={form.departure_airport_id}
                  onChange={(e) => setForm({ ...form, departure_airport_id: Number(e.target.value) })}
                  disabled={modalMode === 'edit'}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 disabled:bg-gray-100"
                >
                  <option value={0}>Select airport</option>
                  {airports.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.code} – {a.city}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Arrival Airport</label>
                <select
                  value={form.arrival_airport_id}
                  onChange={(e) => setForm({ ...form, arrival_airport_id: Number(e.target.value) })}
                  disabled={modalMode === 'edit'}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 disabled:bg-gray-100"
                >
                  <option value={0}>Select airport</option>
                  {airports.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.code} – {a.city}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Departure Time</label>
                <input
                  type="datetime-local"
                  value={form.departure_time}
                  onChange={(e) => setForm({ ...form, departure_time: e.target.value })}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Arrival Time</label>
                <input
                  type="datetime-local"
                  value={form.arrival_time}
                  onChange={(e) => setForm({ ...form, arrival_time: e.target.value })}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Price (USD)</label>
                <input
                  type="number"
                  min={0}
                  step={0.01}
                  value={form.price || ''}
                  onChange={(e) => setForm({ ...form, price: Number(e.target.value) })}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Total Seats</label>
                <input
                  type="number"
                  min={1}
                  value={form.total_seats || ''}
                  onChange={(e) => setForm({ ...form, total_seats: Number(e.target.value) })}
                  disabled={modalMode === 'edit'}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 disabled:bg-gray-100"
                />
              </div>
              {modalMode === 'edit' && (
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-gray-700">Status</label>
                  <select
                    value={editStatus}
                    onChange={(e) => setEditStatus(e.target.value)}
                    className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                  >
                    {FLIGHT_STATUSES.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>
              )}
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={closeModal}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                disabled={isSaving}
                className="rounded-md bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-50"
              >
                {isSaving ? 'Saving…' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation */}
      {deleteTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
            <h2 className="text-lg font-semibold text-gray-900">Delete Flight</h2>
            <p className="mt-2 text-sm text-gray-600">
              Are you sure you want to delete flight <strong>{deleteTarget.flight_number}</strong>? This action cannot be undone.
            </p>
            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={() => setDeleteTarget(null)}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
