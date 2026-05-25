import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

if (!supabaseUrl || !supabaseAnonKey) {
  throw new Error('Missing Supabase environment variables');
}

export const supabase = createClient(supabaseUrl, supabaseAnonKey);

export type User = {
  id: string;
  name: string;
  email: string;
  phone?: string;
  is_caregiver: boolean;
  created_at: string;
};

export type Medicine = {
  id: string;
  user_id: string;
  name: string;
  dosage: string;
  form: string;
  pill_count: number;
  refill_threshold: number;
  is_active: boolean;
  created_at: string;
};

export type Schedule = {
  id: string;
  medicine_id: string;
  frequency: string;
  dose_times: string[];
  days_of_week: number[];
  doses_per_day: number;
};

export type DoseLog = {
  id: string;
  user_id: string;
  medicine_id: string;
  scheduled_at: string;
  taken_at?: string;
  status: 'pending' | 'taken' | 'missed' | 'skipped';
  created_at: string;
};

export type FamilyMember = {
  id: string;
  caregiver_id: string;
  member_id: string;
  relationship: string;
  permission: 'view' | 'edit' | 'full';
  notify_missed: boolean;
  created_at: string;
};
