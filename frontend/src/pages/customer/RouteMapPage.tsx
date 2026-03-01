import { useState, useEffect, useCallback } from 'react';
import toast from 'react-hot-toast';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import Spinner from '@/components/ui/Spinner';
import * as airportService from '@/services/airport.service';
import * as flightService from '@/services/flight.service';
import type { Airport, Flight } from '@/types';

// 自定义 TileLayer 组件，带错误处理和备用源
function CustomTileLayer() {
  const map = useMap();
  const [tileError, setTileError] = useState(false);

  const handleTileError = useCallback(() => {
    if (!tileError) {
      console.warn('OpenStreetMap tile failed, switching to backup');
      setTileError(true);
    }
  }, [tileError]);

  // 备用地图源：CartoDB Positron
  const backupUrl = 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png';

  return (
    <TileLayer
      attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/attributions">CARTO</a>'
      url={tileError ? backupUrl : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"}
      eventHandlers={{
        tileerror: handleTileError
      }}
    />
  );
}

const airportIcon = new L.DivIcon({
  html: `<div style="width:24px;height:24px;background:#2563eb;border:3px solid #fff;border-radius:50%;box-shadow:0 2px 6px rgba(0,0,0,.3);"></div>`,
  iconSize: [24, 24],
  iconAnchor: [12, 12],
  className: '',
});

const ROUTE_COLORS = [
  '#2563eb', '#dc2626', '#16a34a', '#d97706', '#7c3aed',
  '#0891b2', '#be185d', '#4f46e5', '#059669', '#ea580c',
];

export default function RouteMapPage() {
  const [airports, setAirports] = useState<Airport[]>([]);
  const [flights, setFlights] = useState<Flight[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedFlight, setSelectedFlight] = useState<Flight | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setIsLoading(true);
    try {
      const [a, f] = await Promise.all([
        airportService.listAirports(),
        flightService.listFlights(),
      ]);
      setAirports(a);
      setFlights(f);
    } catch {
      toast.error('Failed to load map data');
    } finally {
      setIsLoading(false);
    }
  }

  const airportMap = new Map(airports.map((a) => [a.id, a]));

  const routes = flights
    .map((flight, i) => {
      const dep = airportMap.get(flight.departure_airport_id);
      const arr = airportMap.get(flight.arrival_airport_id);
      if (!dep || !arr) return null;
      return {
        flight,
        from: [dep.latitude, dep.longitude] as [number, number],
        to: [arr.latitude, arr.longitude] as [number, number],
        color: ROUTE_COLORS[i % ROUTE_COLORS.length],
      };
    })
    .filter((r): r is NonNullable<typeof r> => r !== null);

  const center: [number, number] = airports.length > 0
    ? [
        airports.reduce((s, a) => s + a.latitude, 0) / airports.length,
        airports.reduce((s, a) => s + a.longitude, 0) / airports.length,
      ]
    : [39.8283, -98.5795];

  if (isLoading) {
    return (
      <div className="flex justify-center py-24">
        <Spinner className="h-10 w-10" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Route Map</h1>
        <p className="mt-1 text-gray-500">
          Explore {routes.length} flight route{routes.length !== 1 ? 's' : ''} across {airports.length} airport{airports.length !== 1 ? 's' : ''}.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-4">
        {/* Map */}
        <div className="lg:col-span-3 overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          <div style={{ height: '560px' }}>
            <MapContainer
              center={center}
              zoom={4}
              scrollWheelZoom={true}
              style={{ height: '100%', width: '100%' }}
            >
              <CustomTileLayer />
              {airports.map((airport) => (
                <Marker
                  key={airport.id}
                  position={[airport.latitude, airport.longitude]}
                  icon={airportIcon}
                >
                  <Popup>
                    <div className="text-sm">
                      <p className="font-bold">{airport.code}</p>
                      <p>{airport.name}</p>
                      <p className="text-gray-500">{airport.city}, {airport.country}</p>
                    </div>
                  </Popup>
                </Marker>
              ))}
              {routes.map((route) => (
                <Polyline
                  key={route.flight.id}
                  positions={[route.from, route.to]}
                  pathOptions={{
                    color: selectedFlight?.id === route.flight.id ? '#1d4ed8' : route.color,
                    weight: selectedFlight?.id === route.flight.id ? 4 : 2,
                    opacity: selectedFlight && selectedFlight.id !== route.flight.id ? 0.25 : 0.8,
                    dashArray: selectedFlight?.id === route.flight.id ? undefined : '8 4',
                  }}
                  eventHandlers={{
                    click: () => setSelectedFlight(
                      selectedFlight?.id === route.flight.id ? null : route.flight
                    ),
                  }}
                />
              ))}
            </MapContainer>
          </div>
        </div>

        {/* Flight List */}
        <div className="rounded-xl border border-gray-200 bg-white shadow-sm">
          <div className="border-b border-gray-100 px-4 py-3">
            <h2 className="text-sm font-semibold text-gray-900">Routes</h2>
          </div>
          <div className="max-h-[500px] overflow-y-auto divide-y divide-gray-50">
            {routes.length === 0 ? (
              <p className="px-4 py-8 text-center text-sm text-gray-400">No routes available</p>
            ) : (
              routes.map((route) => {
                const isActive = selectedFlight?.id === route.flight.id;
                return (
                  <button
                    key={route.flight.id}
                    type="button"
                    onClick={() => setSelectedFlight(isActive ? null : route.flight)}
                    className={`w-full px-4 py-3 text-left transition-colors ${
                      isActive ? 'bg-primary-50' : 'hover:bg-gray-50'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <div className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: route.color }} />
                      <span className="text-sm font-medium text-gray-900">{route.flight.flight_number}</span>
                    </div>
                    <p className="mt-0.5 pl-5 text-xs text-gray-500">
                      {route.flight.departure_airport?.code} → {route.flight.arrival_airport?.code}
                    </p>
                    <p className="pl-5 text-xs text-gray-400">${route.flight.price.toFixed(0)}</p>
                  </button>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Selected Flight Detail */}
      {selectedFlight && (
        <div className="mt-4 rounded-xl border border-primary-200 bg-primary-50/50 p-5">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h3 className="text-lg font-semibold text-gray-900">{selectedFlight.flight_number}</h3>
              <p className="text-sm text-gray-600">
                {selectedFlight.departure_airport?.city} ({selectedFlight.departure_airport?.code})
                {' → '}
                {selectedFlight.arrival_airport?.city} ({selectedFlight.arrival_airport?.code})
              </p>
            </div>
            <div className="flex items-center gap-6">
              <div className="text-sm">
                <p className="text-gray-400">Price</p>
                <p className="font-bold text-gray-900">${selectedFlight.price.toFixed(0)}</p>
              </div>
              <div className="text-sm">
                <p className="text-gray-400">Seats</p>
                <p className="font-bold text-gray-900">{selectedFlight.available_seats ?? '—'} left</p>
              </div>
              <a
                href={`/flights/${selectedFlight.id}`}
                className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-semibold text-white hover:bg-primary-700 transition-colors"
              >
                View Details
              </a>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
