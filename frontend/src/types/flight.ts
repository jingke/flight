export interface AirportBrief {
  id: number;
  code: string;
  name: string;
  city: string;
  country: string;
}

export interface Airport extends AirportBrief {
  latitude: number;
  longitude: number;
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
  status: string;
  departure_airport: AirportBrief | null;
  arrival_airport: AirportBrief | null;
  available_seats: number | null;
}

export interface FlightSearchParams {
  origin?: string;
  destination?: string;
  date?: string;
  min_price?: number;
  max_price?: number;
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
