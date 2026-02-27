export interface Seat {
  id: number;
  flight_id: number;
  row: number;
  column: string;
  seat_class: string;
  is_available: boolean;
}

export interface SeatAssignment {
  passenger_id: number;
  seat_id: number;
}
