import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Spinner from '@/components/ui/Spinner';
import { fetchFlights, fetchAllBookings, fetchAllComplaints, fetchModificationRequests } from '@/services/admin.service';
import type { Flight, BookingDetail, Complaint, ModificationRequest } from '@/types';
type Booking = BookingDetail;

interface DashboardStats {
  totalFlights: number;
  activeFlights: number;
  totalBookings: number;
  confirmedBookings: number;
  openComplaints: number;
  pendingModifications: number;
  revenue: number;
}

const STATUS_COLORS: Record<string, string> = {
  confirmed: 'bg-green-100 text-green-800',
  cancelled: 'bg-red-100 text-red-800',
  pending: 'bg-yellow-100 text-yellow-800',
  completed: 'bg-blue-100 text-blue-800',
};

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentBookings, setRecentBookings] = useState<Booking[]>([]);
  const [recentComplaints, setRecentComplaints] = useState<Complaint[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function loadDashboard(): Promise<void> {
      try {
        const [flights, bookings, complaints, modifications] = await Promise.all([
          fetchFlights(),
          fetchAllBookings(),
          fetchAllComplaints(),
          fetchModificationRequests().catch(() => [] as ModificationRequest[]),
        ]);
        const confirmedBookings = bookings.filter((b: Booking) => b.status === 'confirmed');
        setStats({
          totalFlights: flights.length,
          activeFlights: flights.filter((f: Flight) => f.status === 'scheduled' || f.status === 'delayed').length,
          totalBookings: bookings.length,
          confirmedBookings: confirmedBookings.length,
          openComplaints: complaints.filter((c: Complaint) => c.status === 'open' || c.status === 'in_progress').length,
          pendingModifications: modifications.filter((m: ModificationRequest) => m.status === 'pending').length,
          revenue: confirmedBookings.reduce((sum: number, b: Booking) => sum + b.total_price, 0),
        });
        setRecentBookings(bookings.slice(0, 5));
        setRecentComplaints(complaints.slice(0, 5));
      } catch {
        /* errors handled by axios interceptor */
      } finally {
        setIsLoading(false);
      }
    }
    loadDashboard();
  }, []);

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner className="h-8 w-8" />
      </div>
    );
  }

  if (!stats) return null;

  const cards = [
    { label: 'Total Flights', value: stats.totalFlights, sub: `${stats.activeFlights} active`, color: 'bg-blue-500', link: '/admin/flights' },
    { label: 'Bookings', value: stats.totalBookings, sub: `${stats.confirmedBookings} confirmed`, color: 'bg-green-500', link: '/admin/reservations' },
    { label: 'Revenue', value: formatCurrency(stats.revenue), sub: 'from confirmed', color: 'bg-purple-500', link: '/admin/reports' },
    { label: 'Open Complaints', value: stats.openComplaints, sub: 'need attention', color: 'bg-orange-500', link: '/admin/complaints' },
    { label: 'Pending Modifications', value: stats.pendingModifications, sub: 'awaiting review', color: 'bg-amber-500', link: '/admin/modifications' },
  ];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Admin Dashboard</h1>
        <p className="mt-1 text-sm text-gray-500">Overview of system metrics and activity.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {cards.map((card) => (
          <Link
            key={card.label}
            to={card.link}
            className="group rounded-lg border border-gray-200 bg-white p-5 shadow-sm transition-shadow hover:shadow-md"
          >
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-gray-500">{card.label}</p>
                <p className="mt-2 text-2xl font-bold text-gray-900">{card.value}</p>
                <p className="mt-1 text-xs text-gray-400">{card.sub}</p>
              </div>
              <span className={`inline-block h-3 w-3 rounded-full ${card.color}`} />
            </div>
          </Link>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-gray-200 px-5 py-4">
            <h2 className="font-semibold text-gray-900">Recent Bookings</h2>
            <Link to="/admin/reservations" className="text-sm text-primary-600 hover:text-primary-700">
              View all
            </Link>
          </div>
          <div className="divide-y divide-gray-100">
            {recentBookings.length === 0 ? (
              <p className="px-5 py-8 text-center text-sm text-gray-400">No bookings yet</p>
            ) : (
              recentBookings.map((booking) => (
                <div key={booking.id} className="flex items-center justify-between px-5 py-3">
                  <div>
                    <p className="text-sm font-medium text-gray-900">
                      {booking.flight_number ?? `Flight #${booking.flight_id}`}
                    </p>
                    <p className="text-xs text-gray-500">
                      {booking.departure_airport} → {booking.arrival_airport}
                    </p>
                  </div>
                  <div className="text-right">
                    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_COLORS[booking.status] ?? 'bg-gray-100 text-gray-800'}`}>
                      {booking.status}
                    </span>
                    <p className="mt-1 text-xs text-gray-400">{formatDate(booking.created_at)}</p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-gray-200 px-5 py-4">
            <h2 className="font-semibold text-gray-900">Recent Complaints</h2>
            <Link to="/admin/complaints" className="text-sm text-primary-600 hover:text-primary-700">
              View all
            </Link>
          </div>
          <div className="divide-y divide-gray-100">
            {recentComplaints.length === 0 ? (
              <p className="px-5 py-8 text-center text-sm text-gray-400">No complaints yet</p>
            ) : (
              recentComplaints.map((complaint) => (
                <div key={complaint.id} className="flex items-center justify-between px-5 py-3">
                  <div>
                    <p className="text-sm font-medium text-gray-900">{complaint.subject}</p>
                    <p className="max-w-xs truncate text-xs text-gray-500">{complaint.description}</p>
                  </div>
                  <div className="text-right">
                    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_COLORS[complaint.status] ?? 'bg-gray-100 text-gray-800'}`}>
                      {complaint.status}
                    </span>
                    <p className="mt-1 text-xs text-gray-400">{formatDate(complaint.created_at)}</p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
