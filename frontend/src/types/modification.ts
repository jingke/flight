export interface ModificationRequest {
  id: number;
  user_id: number;
  booking_id: number;
  type: string;
  details: string;
  status: string;
  created_at: string;
}

export interface ModificationCreate {
  booking_id: number;
  type: string;
  details: string;
}
