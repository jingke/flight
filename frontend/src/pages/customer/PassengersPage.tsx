import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import Spinner from '@/components/ui/Spinner';
import * as passengerService from '@/services/passenger.service';
import type { SavedPassenger } from '@/types';

export default function PassengersPage() {
  const [passengers, setPassengers] = useState<SavedPassenger[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [isEditing, setIsEditing] = useState<number | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({ name: '', email: '' });

  useEffect(() => {
    loadPassengers();
  }, []);

  async function loadPassengers() {
    setIsLoading(true);
    try {
      const data = await passengerService.fetchSavedPassengers();
      setPassengers(data);
    } catch {
      toast.error('Failed to load passengers');
    } finally {
      setIsLoading(false);
    }
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setIsCreating(true);
    try {
      await passengerService.createSavedPassenger(formData);
      toast.success('Passenger saved successfully!');
      setFormData({ name: '', email: '' });
      setShowForm(false);
      loadPassengers();
    } catch {
      toast.error('Failed to save passenger');
    } finally {
      setIsCreating(false);
    }
  }

  async function handleUpdate(e: React.FormEvent, id: number) {
    e.preventDefault();
    setIsCreating(true);
    try {
      await passengerService.updateSavedPassenger(id, formData);
      toast.success('Passenger updated successfully!');
      setFormData({ name: '', email: '' });
      setIsEditing(null);
      loadPassengers();
    } catch {
      toast.error('Failed to update passenger');
    } finally {
      setIsCreating(false);
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Are you sure you want to delete this passenger?')) return;
    try {
      await passengerService.deleteSavedPassenger(id);
      toast.success('Passenger deleted successfully!');
      loadPassengers();
    } catch {
      toast.error('Failed to delete passenger');
    }
  }

  function startEdit(passenger: SavedPassenger) {
    setIsEditing(passenger.id);
    setFormData({ name: passenger.name, email: passenger.email });
    setShowForm(false);
  }

  function cancelEdit() {
    setIsEditing(null);
    setFormData({ name: '', email: '' });
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
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Saved Passengers</h1>
          <p className="mt-1 text-sm text-gray-500">
            Manage your frequently traveled passengers for quick booking
          </p>
        </div>
        {!showForm && !isEditing && (
          <button
            type="button"
            onClick={() => {
              setShowForm(true);
              setFormData({ name: '', email: '' });
            }}
            className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-primary-700 transition-colors"
          >
            + Add Passenger
          </button>
        )}
      </div>

      {(showForm || isEditing !== null) && (
        <div className="mb-8 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">
            {isEditing !== null ? 'Edit Passenger' : 'Add New Passenger'}
          </h2>
          <form
            onSubmit={(e) => {
              if (isEditing !== null) {
                handleUpdate(e, isEditing);
              } else {
                handleCreate(e);
              }
            }}
            className="space-y-4"
          >
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700">
                Full Name
              </label>
              <input
                id="name"
                type="text"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                placeholder="John Doe"
              />
            </div>
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                Email Address
              </label>
              <input
                id="email"
                type="email"
                required
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                placeholder="john@example.com"
              />
            </div>
            <div className="flex gap-3">
              <button
                type="submit"
                disabled={isCreating}
                className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-primary-700 disabled:opacity-50 transition-colors"
              >
                {isCreating ? 'Saving...' : isEditing !== null ? 'Update' : 'Save'}
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowForm(false);
                  cancelEdit();
                }}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {passengers.length === 0 ? (
        <div className="rounded-xl border border-dashed border-gray-300 bg-white p-12 text-center">
          <svg
            className="mx-auto h-12 w-12 text-gray-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"
            />
          </svg>
          <h3 className="mt-2 text-sm font-semibold text-gray-900">No saved passengers</h3>
          <p className="mt-1 text-sm text-gray-500">
            Get started by adding your first passenger.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {passengers.map((passenger) => (
            <div
              key={passenger.id}
              className="flex items-center justify-between rounded-lg border border-gray-200 bg-white p-4 shadow-sm"
            >
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary-50 text-sm font-semibold text-primary-600">
                  {passenger.name.charAt(0).toUpperCase()}
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-900">{passenger.name}</p>
                  <p className="text-xs text-gray-500">{passenger.email}</p>
                </div>
              </div>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => startEdit(passenger)}
                  className="rounded-md border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50 transition-colors"
                >
                  Edit
                </button>
                <button
                  type="button"
                  onClick={() => handleDelete(passenger.id)}
                  className="rounded-md border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 transition-colors"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
