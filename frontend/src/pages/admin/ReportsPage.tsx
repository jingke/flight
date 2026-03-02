import { useEffect, useState } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import Spinner from '@/components/ui/Spinner';
import {
  fetchBookingsPerFlight,
  fetchPopularRoutes,
  fetchPeakTimes,
} from '@/services/admin.service';
import type {
  BookingsPerFlightReport,
  PopularRouteReport,
  PeakTimeReport,
} from '@/types';

type TabKey = 'bookings' | 'routes' | 'peak';

const TABS: { key: TabKey; label: string }[] = [
  { key: 'bookings', label: 'Bookings per Flight' },
  { key: 'routes', label: 'Popular Routes' },
  { key: 'peak', label: 'Peak Times' },
];

const CHART_COLORS = [
  '#2563eb', '#7c3aed', '#059669', '#d97706', '#dc2626',
  '#0891b2', '#4f46e5', '#16a34a', '#ea580c', '#be185d',
];

function formatHour(hour: number): string {
  const ampm = hour >= 12 ? 'PM' : 'AM';
  const h = hour % 12 || 12;
  return `${h} ${ampm}`;
}

export default function ReportsPage() {
  const [activeTab, setActiveTab] = useState<TabKey>('bookings');
  const [bookingsData, setBookingsData] = useState<BookingsPerFlightReport[]>([]);
  const [routesData, setRoutesData] = useState<PopularRouteReport[]>([]);
  const [peakData, setPeakData] = useState<PeakTimeReport[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    async function loadReports(): Promise<void> {
      setIsLoading(true);
      setHasError(false);
      try {
        const [b, r, p] = await Promise.all([
          fetchBookingsPerFlight(),
          fetchPopularRoutes(),
          fetchPeakTimes(),
        ]);
        setBookingsData(b);
        setRoutesData(r);
        setPeakData(p);
      } catch {
        setHasError(true);
      } finally {
        setIsLoading(false);
      }
    }
    loadReports();
  }, []);

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner className="h-8 w-8" />
      </div>
    );
  }

  if (hasError) {
    return (
      <div className="space-y-4">
        <h1 className="text-2xl font-bold text-gray-900">Reports</h1>
        <div className="rounded-lg border border-amber-200 bg-amber-50 px-5 py-8 text-center">
          <p className="text-sm text-amber-800">
            Unable to load reports. The reports API may not be available yet.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Reports</h1>
        <p className="mt-1 text-sm text-gray-500">Bookings per flight, popular routes, and peak times.</p>
      </div>

      <div className="flex gap-2 border-b border-gray-200">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`border-b-2 px-4 py-2 text-sm font-medium transition-colors ${
              activeTab === tab.key
                ? 'border-primary-600 text-primary-600'
                : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        {activeTab === 'bookings' && <BookingsPerFlightChart data={bookingsData} />}
        {activeTab === 'routes' && <PopularRoutesChart data={routesData} />}
        {activeTab === 'peak' && <PeakTimesChart data={peakData} />}
      </div>
    </div>
  );
}

function BookingsPerFlightChart({ data }: { data: BookingsPerFlightReport[] }) {
  if (data.length === 0) {
    return <EmptyChart message="No booking data available" />;
  }

  const chartData = data.map((d) => ({
    name: d.flight_number,
    route: `${d.departure} → ${d.arrival}`,
    bookings: d.booking_count,
  }));

  return (
    <div>
      <h3 className="mb-4 text-base font-semibold text-gray-900">Bookings per Flight</h3>
      <ResponsiveContainer width="100%" height={400}>
        <BarChart data={chartData} margin={{ top: 5, right: 20, bottom: 60, left: 20 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis
            dataKey="name"
            tick={{ fontSize: 12 }}
            angle={-45}
            textAnchor="end"
            height={80}
          />
          <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
          <Tooltip
            content={({ active, payload }) => {
              if (!active || !payload?.[0]) return null;
              const item = payload[0].payload as typeof chartData[number];
              return (
                <div className="rounded-lg border border-gray-200 bg-white px-3 py-2 shadow-md">
                  <p className="text-sm font-semibold">{item.name}</p>
                  <p className="text-xs text-gray-500">{item.route}</p>
                  <p className="mt-1 text-sm text-primary-600">{item.bookings} bookings</p>
                </div>
              );
            }}
          />
          <Bar dataKey="bookings" radius={[4, 4, 0, 0]}>
            {chartData.map((_, i) => (
              <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
      <SummaryTable
        headers={['Flight', 'Route', 'Bookings']}
        rows={data.map((d) => [d.flight_number, `${d.departure} → ${d.arrival}`, String(d.booking_count)])}
      />
    </div>
  );
}

function PopularRoutesChart({ data }: { data: PopularRouteReport[] }) {
  if (data.length === 0) {
    return <EmptyChart message="No route data available" />;
  }

  const chartData = data.map((d) => ({
    name: `${d.origin_code}→${d.destination_code}`,
    full: `${d.origin_city} → ${d.destination_city}`,
    bookings: d.booking_count,
  }));

  return (
    <div>
      <h3 className="mb-4 text-base font-semibold text-gray-900">Popular Routes</h3>
      <ResponsiveContainer width="100%" height={400}>
        <BarChart data={chartData} layout="vertical" margin={{ top: 5, right: 20, bottom: 5, left: 80 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis type="number" tick={{ fontSize: 12 }} allowDecimals={false} />
          <YAxis dataKey="name" type="category" tick={{ fontSize: 12 }} width={80} />
          <Tooltip
            content={({ active, payload }) => {
              if (!active || !payload?.[0]) return null;
              const item = payload[0].payload as typeof chartData[number];
              return (
                <div className="rounded-lg border border-gray-200 bg-white px-3 py-2 shadow-md">
                  <p className="text-sm font-semibold">{item.full}</p>
                  <p className="mt-1 text-sm text-primary-600">{item.bookings} bookings</p>
                </div>
              );
            }}
          />
          <Bar dataKey="bookings" radius={[0, 4, 4, 0]}>
            {chartData.map((_, i) => (
              <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
      <SummaryTable
        headers={['Route', 'Cities', 'Bookings']}
        rows={data.map((d) => [
          `${d.origin_code} → ${d.destination_code}`,
          `${d.origin_city} → ${d.destination_city}`,
          String(d.booking_count),
        ])}
      />
    </div>
  );
}

function PeakTimesChart({ data }: { data: PeakTimeReport[] }) {
  if (data.length === 0) {
    return <EmptyChart message="No peak time data available" />;
  }

  const chartData = data.map((d) => ({
    name: formatHour(d.hour),
    hour: d.hour,
    bookings: d.booking_count,
  }));

  return (
    <div>
      <h3 className="mb-4 text-base font-semibold text-gray-900">Peak Booking Times</h3>
      <ResponsiveContainer width="100%" height={400}>
        <BarChart data={chartData} margin={{ top: 5, right: 20, bottom: 5, left: 20 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis dataKey="name" tick={{ fontSize: 12 }} />
          <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
          <Tooltip
            content={({ active, payload }) => {
              if (!active || !payload?.[0]) return null;
              const item = payload[0].payload as typeof chartData[number];
              return (
                <div className="rounded-lg border border-gray-200 bg-white px-3 py-2 shadow-md">
                  <p className="text-sm font-semibold">{item.name}</p>
                  <p className="mt-1 text-sm text-primary-600">{item.bookings} bookings</p>
                </div>
              );
            }}
          />
          <Bar dataKey="bookings" radius={[4, 4, 0, 0]} fill="#2563eb" />
        </BarChart>
      </ResponsiveContainer>
      <SummaryTable
        headers={['Time', 'Bookings']}
        rows={data.map((d) => [formatHour(d.hour), String(d.booking_count)])}
      />
    </div>
  );
}

function EmptyChart({ message }: { message: string }) {
  return (
    <div className="flex h-64 items-center justify-center text-sm text-gray-400">
      {message}
    </div>
  );
}

function SummaryTable({ headers, rows }: { headers: string[]; rows: string[][] }) {
  return (
    <div className="mt-6 overflow-hidden rounded-md border border-gray-200">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <thead className="bg-gray-50">
          <tr>
            {headers.map((h) => (
              <th key={h} className="px-4 py-2 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {rows.map((row, i) => (
            <tr key={i} className="hover:bg-gray-50">
              {row.map((cell, j) => (
                <td key={j} className="whitespace-nowrap px-4 py-2 text-gray-700">
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
