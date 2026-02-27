export interface Passenger {
  id: number;
  name: string;
  email: string;
  seat_assignment: string | null;
}

export interface PassengerInput {
  name: string;
  email: string;
  seat_id?: number | null;
}

export interface BookingCreate {
  flight_id: number;
  passengers: PassengerInput[];
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
}

export interface BookingDetail extends Booking {
  flight_number: string | null;
  departure_airport: string | null;
  arrival_airport: string | null;
  departure_time: string | null;
  arrival_time: string | null;
}
