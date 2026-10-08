export const ROLES = {
  CUSTOMER: 'CUSTOMER',
  ADMIN: 'ADMIN',
  FINANCE: 'FINANCE',
  SUPPORT: 'SUPPORT'
} as const;

export type ROLES = keyof typeof ROLES;
