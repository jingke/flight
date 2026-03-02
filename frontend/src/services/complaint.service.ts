import api from './api';
import type { Complaint, ComplaintCreate } from '@/types';

export async function createComplaint(payload: ComplaintCreate): Promise<Complaint> {
  const { data } = await api.post<Complaint>('/complaints/', payload);
  return data;
}

export async function listComplaints(): Promise<Complaint[]> {
  const { data } = await api.get<Complaint[]>('/complaints/');
  return data;
}

export async function getComplaint(complaintId: number): Promise<Complaint> {
  const { data } = await api.get<Complaint>(`/complaints/${complaintId}`);
  return data;
}
