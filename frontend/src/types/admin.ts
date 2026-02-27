export interface AirportBrief {
  id: number;
  code: string;
  name: string;
  city: string;
  country: string;
}

export interface Flight {
  id: number;
  flight_number: string;
  departure_airport_id: number;
  arrival_airport_id: number;
  departure_time: string;
  arrival_time: string;
  price: number;
  total_seats: number;
  status: 'scheduled' | 'delayed' | 'cancelled' | 'completed';
  departure_airport: AirportBrief | null;
  arrival_airport: AirportBrief | null;
  available_seats: number | null;
}

export interface FlightCreate {
  flight_number: string;
  departure_airport_id: number;
  arrival_airport_id: number;
  departure_time: string;
  arrival_time: string;
  price: number;
  total_seats: number;
}

export interface FlightUpdate {
  departure_time?: string;
  arrival_time?: string;
  price?: number;
  status?: string;
}

export interface Passenger {
  id: number;
  name: string;
  email: string;
  seat_assignment: string | null;
}

export interface Booking {
  id: number;
  user_id: number;
  flight_id: number;
  status: string;
  total_price: number;
  payment_status: string;
  created_at: string;
  passengers: Passenger[];
  flight_number: string | null;
  departure_airport: string | null;
  arrival_airport: string | null;
  departure_time: string | null;
  arrival_time: string | null;
}

export interface Complaint {
  id: number;
  user_id: number;
  booking_id: number | null;
  subject: string;
  description: string;
  status: 'open' | 'in_progress' | 'resolved' | 'closed';
  admin_response: string | null;
  created_at: string;
}

export interface ComplaintUpdate {
  status?: string;
  admin_response?: string;
}

export interface ModificationRequest {
  id: number;
  user_id: number;
  booking_id: number;
  type: 'date_change' | 'seat_change' | 'passenger_change' | 'cancellation';
  details: string;
  status: 'pending' | 'approved' | 'rejected';
  created_at: string;
}

export interface BookingsPerFlightReport {
  flight_id: number;
  flight_number: string;
  departure: string;
  arrival: string;
  booking_count: number;
}

export interface PopularRouteReport {
  origin_code: string;
  origin_city: string;
  destination_code: string;
  destination_city: string;
  booking_count: number;
}

export interface PeakTimeReport {
  hour: number;
  booking_count: number;
}
