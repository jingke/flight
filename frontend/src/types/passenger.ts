export interface SavedPassenger {
  id: number;
  user_id: number;
  name: string;
  email: string;
}

export interface SavedPassengerCreate {
  name: string;
  email: string;
}

export interface SavedPassengerUpdate {
  name?: string;
  email?: string;
}
