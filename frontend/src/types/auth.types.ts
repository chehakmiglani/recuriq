import { ROLES } from '../constants/roles';

export interface User {
  id?: number; // Might not be known initially
  email: string;
  role: ROLES;
  fullName?: string;
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
