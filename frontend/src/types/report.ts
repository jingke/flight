export interface BookingsPerFlightReport {
  flight_number: string;
  departure: string;
  arrival: string;
  booking_count: number;
  total_revenue: number;
}

export interface PopularRouteReport {
  origin: string;
  destination: string;
  origin_code: string;
  destination_code: string;
  origin_city: string;
  destination_city: string;
  total_bookings: number;
  booking_count: number;
}

export interface PeakTimeReport {
  hour: number;
  total_bookings: number;
  booking_count: number;
}
