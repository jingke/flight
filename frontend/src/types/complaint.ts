export interface Complaint {
  id: number;
  user_id: number;
  booking_id: number | null;
  subject: string;
  description: string;
  status: string;
  admin_response: string | null;
  created_at: string;
}

export interface ComplaintCreate {
  booking_id?: number | null;
  subject: string;
  description: string;
}

export interface ComplaintUpdate {
  status?: string;
  admin_response?: string;
}
