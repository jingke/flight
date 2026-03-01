import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import Spinner from '@/components/ui/Spinner';
import { useAuthStore } from '@/stores/auth.store';
import * as flightService from '@/services/flight.service';
import * as seatService from '@/services/seat.service';
import * as bookingService from '@/services/booking.service';
import * as passengerService from '@/services/passenger.service';
import type { Flight, Seat, PassengerInput, SavedPassenger } from '@/types';

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString([], {
    month: 'short', day: 'numeric', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

function getDuration(dep: string, arr: string): string {
  const ms = new Date(arr).getTime() - new Date(dep).getTime();
  const h = Math.floor(ms / 3_600_000);
  const m = Math.floor((ms % 3_600_000) / 60_000);
  return `${h}h ${m}m`;
}

const CLASS_COLORS: Record<string, { available: string; taken: string; label: string }> = {
  FIRST: { available: 'bg-amber-100 border-amber-400 hover:bg-amber-200', taken: 'bg-amber-800/20 border-amber-800/30', label: 'First' },
  BUSINESS: { available: 'bg-blue-100 border-blue-400 hover:bg-blue-200', taken: 'bg-blue-800/20 border-blue-800/30', label: 'Business' },
  ECONOMY: { available: 'bg-emerald-100 border-emerald-400 hover:bg-emerald-200', taken: 'bg-emerald-800/20 border-emerald-800/30', label: 'Economy' },
};

interface PassengerForm {
  name: string;
  email: string;
  selectedSeatId: number | null;
  selectedSeatLabel: string;
}

export default function FlightDetailPage() {
  const { flightId } = useParams<{ flightId: string }>();
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const [flight, setFlight] = useState<Flight | null>(null);
  const [seats, setSeats] = useState<Seat[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isBooking, setIsBooking] = useState(false);
  const [activePassenger, setActivePassenger] = useState<number>(0);
  const [passengers, setPassengers] = useState<PassengerForm[]>([
    { name: '', email: '', selectedSeatId: null, selectedSeatLabel: '' },
  ]);
  const [savedPassengers, setSavedPassengers] = useState<SavedPassenger[]>([]);
  const [bookingError, setBookingError] = useState<string | null>(null);

  useEffect(() => {
    if (!flightId) return;
    loadData();
  }, [flightId]);

  async function loadData() {
    setIsLoading(true);
    try {
      const [f, s] = await Promise.all([
        flightService.getFlight(Number(flightId)),
        seatService.getSeatMap(Number(flightId)).catch(() => [] as Seat[]),
      ]);
      setFlight(f);
      setSeats(s);
      
      if (isAuthenticated) {
        try {
          const saved = await passengerService.fetchSavedPassengers();
          setSavedPassengers(saved);
        } catch {
          console.error('Failed to load saved passengers');
        }
      }
    } catch {
      toast.error('Failed to load flight details');
    } finally {
      setIsLoading(false);
    }
  }

  const seatMap = useMemo(() => {
    const rows = new Map<number, Seat[]>();
    for (const seat of seats) {
      const row = rows.get(seat.row) ?? [];
      row.push(seat);
      rows.set(seat.row, row);
    }
    for (const row of rows.values()) {
      row.sort((a, b) => a.column.localeCompare(b.column));
    }
    return new Map([...rows.entries()].sort(([a], [b]) => a - b));
  }, [seats]);

  const allColumns = useMemo(() => {
    const cols = new Set<string>();
    for (const seat of seats) cols.add(seat.column);
    return [...cols].sort();
  }, [seats]);

  const selectedSeatIds = new Set(
    passengers.map((p) => p.selectedSeatId).filter((id): id is number => id !== null)
  );

  function handleSeatClick(seat: Seat) {
    if (!seat.is_available) return;
    if (selectedSeatIds.has(seat.id) && passengers[activePassenger]?.selectedSeatId !== seat.id) return;
    setPassengers((prev) =>
      prev.map((p, i) =>
        i === activePassenger
          ? { ...p, selectedSeatId: seat.id, selectedSeatLabel: `${seat.row}${seat.column}` }
          : p
      )
    );
  }

  function addPassenger() {
    setPassengers((prev) => [...prev, { name: '', email: '', selectedSeatId: null, selectedSeatLabel: '' }]);
    setActivePassenger(passengers.length);
  }

  function removePassenger(index: number) {
    if (passengers.length <= 1) return;
    setPassengers((prev) => prev.filter((_, i) => i !== index));
    setActivePassenger((prev) => Math.min(prev, passengers.length - 2));
  }

  function updatePassenger(index: number, field: 'name' | 'email', value: string) {
    setPassengers((prev) =>
      prev.map((p, i) => (i === index ? { ...p, [field]: value } : p))
    );
  }

  function selectSavedPassenger(passenger: SavedPassenger, index: number) {
    setPassengers((prev) =>
      prev.map((p, i) =>
        i === index
          ? { ...p, name: passenger.name, email: passenger.email, selectedSeatId: p.selectedSeatId, selectedSeatLabel: p.selectedSeatLabel }
          : p
      )
    );
  }

  async function handleBook() {
    if (!flight) return;
    if (!isAuthenticated) {
      toast.error('Please log in to book a flight');
      navigate('/login');
      return;
    }
    const hasInvalid = passengers.some((p) => !p.name.trim() || !p.email.trim());
    if (hasInvalid) {
      toast.error('Please fill in all passenger details');
      return;
    }
    setIsBooking(true);
    setBookingError(null);
    try {
      const payload: PassengerInput[] = passengers.map((p) => ({
        name: p.name.trim(),
        email: p.email.trim(),
        seat_id: p.selectedSeatId,
      }));
      const booking = await bookingService.createBooking({
        flight_id: flight.id,
        passengers: payload,
      });
      toast.success('Booking confirmed!');
      navigate(`/bookings/${booking.id}`);
    } catch (err: unknown) {
      const errorMessage = (err as { message?: string })?.message ?? 'Booking failed — please try again';
      setBookingError(errorMessage);
    } finally {
      setIsBooking(false);
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-24">
        <Spinner className="h-10 w-10" />
      </div>
    );
  }

  if (!flight) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16 text-center">
        <h2 className="text-xl font-semibold text-gray-900">Flight not found</h2>
        <Link to="/flights" className="mt-4 inline-block text-primary-600 hover:underline">Back to search</Link>
      </div>
    );
  }

  const totalPrice = flight.price * passengers.length;

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <Link to="/flights" className="mb-6 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-primary-600 transition-colors">
        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
        Back to search
      </Link>

      {/* Booking Error Modal */}
      {bookingError && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-2xl">
            <div className="mb-4 flex items-center justify-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
                <svg className="h-8 w-8 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
            <h3 className="mb-2 text-center text-lg font-semibold text-gray-900">Booking Failed</h3>
            <p className="mb-4 text-center text-sm text-gray-600">{bookingError}</p>
            
            {/* Flight and Seat Info */}
            <div className="mb-6 rounded-lg border border-gray-200 bg-gray-50 p-4">
              <div className="mb-2 flex items-center justify-between">
                <span className="text-xs text-gray-500">Flight</span>
                <span className="text-sm font-semibold text-gray-900">{flight.flight_number}</span>
              </div>
              {passengers.map((p, idx) => (
                <div key={idx} className="mb-2 flex items-center justify-between">
                  <span className="text-xs text-gray-500">Passenger {idx + 1}</span>
                  <span className="text-sm font-semibold text-gray-900">
                    {p.selectedSeatLabel || 'No seat'}
                  </span>
                </div>
              ))}
            </div>
            
            <button
              type="button"
              onClick={() => setBookingError(null)}
              className="w-full rounded-lg bg-red-600 py-2.5 text-sm font-semibold text-white hover:bg-red-700 transition-colors"
            >
              OK
            </button>
          </div>
        </div>
      )}

      {/* Flight Info Header */}
      <div className="mb-8 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-gray-900">{flight.flight_number}</h1>
              <span className="rounded-full bg-green-50 px-3 py-0.5 text-sm font-medium text-green-700 ring-1 ring-inset ring-green-600/20">
                {flight.status}
              </span>
            </div>
            <p className="mt-2 text-lg text-gray-600">
              {flight.departure_airport?.city} ({flight.departure_airport?.code})
              {' → '}
              {flight.arrival_airport?.city} ({flight.arrival_airport?.code})
            </p>
          </div>
          <div className="text-right">
            <p className="text-3xl font-bold text-gray-900">${flight.price.toFixed(0)}</p>
            <p className="text-sm text-gray-400">per person</p>
          </div>
        </div>
        <div className="mt-4 grid grid-cols-2 gap-4 border-t border-gray-100 pt-4 sm:grid-cols-4">
          <div>
            <p className="text-xs font-medium uppercase text-gray-400">Departure</p>
            <p className="mt-0.5 text-sm font-medium text-gray-900">{formatDateTime(flight.departure_time)}</p>
          </div>
          <div>
            <p className="text-xs font-medium uppercase text-gray-400">Arrival</p>
            <p className="mt-0.5 text-sm font-medium text-gray-900">{formatDateTime(flight.arrival_time)}</p>
          </div>
          <div>
            <p className="text-xs font-medium uppercase text-gray-400">Duration</p>
            <p className="mt-0.5 text-sm font-medium text-gray-900">{getDuration(flight.departure_time, flight.arrival_time)}</p>
          </div>
          <div>
            <p className="text-xs font-medium uppercase text-gray-400">Available Seats</p>
            <p className="mt-0.5 text-sm font-medium text-gray-900">{flight.available_seats ?? '—'}</p>
          </div>
        </div>
      </div>

      <div className="grid gap-8 lg:grid-cols-5">
        {/* Seat Map */}
        <div className="lg:col-span-3">
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-lg font-semibold text-gray-900">Select Seats</h2>

            <div className="mb-4 flex flex-wrap items-center gap-4 text-xs">
              {Object.entries(CLASS_COLORS).map(([cls, style]) => (
                <div key={cls} className="flex items-center gap-1.5">
                  <div className={`h-4 w-4 rounded border ${style.available}`} />
                  <span className="text-gray-600">{style.label} (available)</span>
                </div>
              ))}
              <div className="flex items-center gap-1.5">
                <div className="h-4 w-4 rounded border border-gray-300 bg-gray-200" />
                <span className="text-gray-600">Taken</span>
              </div>
              <div className="flex items-center gap-1.5">
                <div className="h-4 w-4 rounded border-2 border-primary-600 bg-primary-100" />
                <span className="text-gray-600">Selected</span>
              </div>
            </div>

            {seats.length === 0 ? (
              <p className="py-8 text-center text-sm text-gray-400">No seat map available for this flight.</p>
            ) : (
              <div className="overflow-x-auto">
                <div className="mx-auto w-fit">
                  <div className="mb-2 flex items-center gap-1">
                    <div className="w-8" />
                    {allColumns.map((col, i) => (
                      <div key={col} className="flex items-center">
                        <div className="flex h-7 w-9 items-center justify-center text-xs font-semibold text-gray-400">{col}</div>
                        {i === Math.floor(allColumns.length / 2) - 1 && <div className="w-6" />}
                      </div>
                    ))}
                  </div>
                  {[...seatMap.entries()].map(([rowNum, rowSeats]) => {
                    const midpoint = Math.floor(allColumns.length / 2);
                    return (
                      <div key={rowNum} className="mb-1 flex items-center gap-1">
                        <div className="flex h-7 w-8 items-center justify-center text-xs font-semibold text-gray-400">{rowNum}</div>
                        {rowSeats.map((seat, i) => {
                          const isSelected = selectedSeatIds.has(seat.id);
                          const isActiveSelected = passengers[activePassenger]?.selectedSeatId === seat.id;
                          const classStyle = CLASS_COLORS[seat.seat_class] ?? CLASS_COLORS.ECONOMY;
                          let seatClass: string;
                          if (isActiveSelected) {
                            seatClass = 'border-2 border-primary-600 bg-primary-100 cursor-pointer';
                          } else if (isSelected) {
                            seatClass = 'border-2 border-primary-400 bg-primary-50 cursor-not-allowed';
                          } else if (!seat.is_available) {
                            seatClass = `${classStyle.taken} cursor-not-allowed opacity-50`;
                          } else {
                            seatClass = `${classStyle.available} cursor-pointer`;
                          }
                          return (
                            <div key={seat.id} className="flex items-center">
                              <button
                                type="button"
                                onClick={() => handleSeatClick(seat)}
                                disabled={!seat.is_available && !isSelected}
                                className={`flex h-9 w-9 items-center justify-center rounded text-xs font-medium border transition-colors ${seatClass}`}
                                title={`${rowNum}${seat.column} — ${seat.seat_class}${!seat.is_available ? ' (taken)' : ''}`}
                              >
                                {!seat.is_available && !isSelected ? '×' : `${seat.column}`}
                              </button>
                              {i === midpoint - 1 && <div className="w-6" />}
                            </div>
                          );
                        })}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Booking Form */}
        <div className="lg:col-span-2">
          <div className="sticky top-8 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900">Passengers</h2>
              {isAuthenticated && (
                <Link
                  to="/passengers"
                  className="text-xs text-primary-600 hover:text-primary-700 hover:underline"
                >
                  Manage passengers →
                </Link>
              )}
            </div>

            <div className="space-y-4">
              {passengers.map((p, i) => (
                <div
                  key={i}
                  onClick={() => setActivePassenger(i)}
                  className={`rounded-lg border-2 p-4 transition-colors cursor-pointer ${
                    i === activePassenger ? 'border-primary-500 bg-primary-50/30' : 'border-gray-100 hover:border-gray-200'
                  }`}
                >
                  <div className="mb-3 flex items-center justify-between">
                    <span className="text-sm font-semibold text-gray-700">Passenger {i + 1}</span>
                    {passengers.length > 1 && (
                      <button
                        type="button"
                        onClick={(e) => { e.stopPropagation(); removePassenger(i); }}
                        className="text-xs text-red-500 hover:text-red-700"
                      >
                        Remove
                      </button>
                    )}
                  </div>
                  
                  {isAuthenticated && savedPassengers.length > 0 && (
                    <div className="mb-3">
                      <label className="mb-1 block text-xs font-medium text-gray-500">
                        Select from saved passengers
                      </label>
                      <select
                        value={p.name ? savedPassengers.find(sp => sp.name === p.name && sp.email === p.email)?.id || '' : ''}
                        onClick={(e) => e.stopPropagation()}
                        onChange={(e) => {
                          const selectedId = Number(e.target.value);
                          if (selectedId) {
                            const selected = savedPassengers.find(sp => sp.id === selectedId);
                            if (selected) {
                              selectSavedPassenger(selected, i);
                            }
                          }
                        }}
                        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 focus:outline-none bg-white"
                      >
                        <option value="">-- Select a passenger --</option>
                        {savedPassengers.map((sp) => (
                          <option key={sp.id} value={sp.id}>
                            {sp.name} ({sp.email})
                          </option>
                        ))}
                      </select>
                    </div>
                  )}
                  
                  {p.name && (
                    <div className="space-y-2">
                      <div className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2">
                        <label className="mb-0.5 block text-xs font-medium text-gray-500">Full name</label>
                        <p className="text-sm font-medium text-gray-900">{p.name}</p>
                      </div>
                      <div className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2">
                        <label className="mb-0.5 block text-xs font-medium text-gray-500">Email address</label>
                        <p className="text-sm font-medium text-gray-900">{p.email}</p>
                      </div>
                    </div>
                  )}
                  
                  <div>
                    <label className="mb-1 block text-xs font-medium text-gray-500">Seat assignment</label>
                    {p.selectedSeatLabel ? (
                      <div className="flex items-center gap-2 rounded-lg bg-primary-50 px-3 py-2 text-sm">
                        <svg className="h-4 w-4 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                        <span className="font-medium text-primary-700">Seat {p.selectedSeatLabel}</span>
                      </div>
                    ) : (
                      <p className="px-1 text-xs text-gray-400">Click a seat on the map to assign</p>
                    )}
                  </div>
                </div>
              ))}
            </div>

            <button
              type="button"
              onClick={addPassenger}
              className="mt-3 w-full rounded-lg border border-dashed border-gray-300 py-2.5 text-sm font-medium text-gray-600 hover:border-primary-400 hover:text-primary-600 transition-colors"
            >
              + Add Passenger
            </button>

            <div className="mt-6 border-t border-gray-100 pt-4">
              <div className="flex items-center justify-between text-sm text-gray-500">
                <span>{passengers.length} passenger{passengers.length !== 1 ? 's' : ''} × ${flight.price.toFixed(0)}</span>
              </div>
              <div className="mt-1 flex items-center justify-between">
                <span className="text-lg font-bold text-gray-900">Total</span>
                <span className="text-2xl font-bold text-gray-900">${totalPrice.toFixed(2)}</span>
              </div>
            </div>

            <button
              type="button"
              onClick={handleBook}
              disabled={isBooking}
              className="mt-4 w-full rounded-lg bg-primary-600 py-3 text-sm font-semibold text-white shadow-sm hover:bg-primary-700 disabled:opacity-50 transition-colors"
            >
              {isBooking ? (
                <span className="inline-flex items-center gap-2">
                  <Spinner className="h-4 w-4" /> Processing...
                </span>
              ) : (
                'Book Now'
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
