import React, { createContext, useContext, useState, useEffect } from 'react';
import type { User, AuthState } from '../types/auth.types';
import { authService } from '../services/auth/authService';
import * as CustomerService from '../services/customer/customerService';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (user: User) => Promise<{ isCustomer: boolean }>;
  logout: () => void;
  isCustomer: boolean;
  hasDraftSubscription: boolean;
  refreshUserStatus: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    token: null, // Left for legacy types support, but completely unused
    isAuthenticated: false,
    isLoading: true,
  });
  // Initialize isCustomer from sessionStorage if available (default to false, never undefined)
  const [isCustomer, setIsCustomer] = useState<boolean>(() => {
    try {
      const cached = sessionStorage.getItem('isCustomer');
      return cached === 'true';
    } catch {
      return false;
    }
  });

  const [hasDraftSubscription, setHasDraftSubscription] = useState<boolean>(() => {
    try {
      const cached = sessionStorage.getItem('hasDraftSubscription');
      return cached === 'true';
    } catch {
      return false;
    }
  });

  useEffect(() => {
    // On page load, ask the Spring Boot server if our JSESSIONID cookie is still valid
    const verifySession = async () => {
      try {
        const user = await authService.checkSession();
        if (user && user.email) {
          // Check if user is a customer FIRST (before setting auth state)
          try {
            const customerStatus = await CustomerService.getCustomerStatus();
            setIsCustomer(customerStatus.isCustomer);
            setHasDraftSubscription(customerStatus.hasDraftSubscription);
            sessionStorage.setItem('isCustomer', String(customerStatus.isCustomer));
            sessionStorage.setItem('hasDraftSubscription', String(customerStatus.hasDraftSubscription));
          } catch {
            // If customer status check fails, use cached value or assume not a customer
            const cachedCustomer = sessionStorage.getItem('isCustomer');
            const cachedDraft = sessionStorage.getItem('hasDraftSubscription');
            setIsCustomer(cachedCustomer === 'true');
            setHasDraftSubscription(cachedDraft === 'true');
          }
          
          // NOW set auth state after customer status is known
          setAuthState({
            user,
            token: null,
            isAuthenticated: true,
            isLoading: false,
          });
          // Store auth flag for cross-tab sync
          sessionStorage.setItem('isAuthenticated', 'true');
        } else {
          setAuthState(prev => ({ ...prev, isLoading: false }));
          setIsCustomer(false);
          setHasDraftSubscription(false);
          sessionStorage.removeItem('isAuthenticated');
          sessionStorage.removeItem('isCustomer');
          sessionStorage.removeItem('hasDraftSubscription');
        }
      } catch {
        setAuthState(prev => ({ ...prev, isLoading: false }));
        setIsCustomer(false);
        sessionStorage.removeItem('isAuthenticated');
        sessionStorage.removeItem('isCustomer');
      }
    };

    // Always verify session on page load, regardless of current page
    // This ensures consistent auth state across reloads and tabs
    verifySession();
  }, []);

  // Listen for storage changes (cross-tab sync)
  useEffect(() => {
    const handleStorageChange = (event: StorageEvent) => {
      if (event.key === 'isAuthenticated') {
        // Another tab logged in or out, reload to sync state
        window.location.reload();
      }
    };
    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  const login = async (user: User) => {
    // Set sessionStorage for cross-tab sync FIRST
    sessionStorage.setItem('isAuthenticated', 'true');
    
    // Check if user is a customer BEFORE setting auth state
    let customerStatusValue = false;
    try {
      const customerStatus = await CustomerService.getCustomerStatus();
      customerStatusValue = customerStatus.isCustomer;
      setIsCustomer(customerStatusValue);
      setHasDraftSubscription(customerStatus.hasDraftSubscription);
      sessionStorage.setItem('isCustomer', String(customerStatusValue));
      sessionStorage.setItem('hasDraftSubscription', String(customerStatus.hasDraftSubscription));
    } catch {
      // If customer status check fails, assume not a customer
      setIsCustomer(false);
      setHasDraftSubscription(false);
      sessionStorage.setItem('isCustomer', 'false');
      sessionStorage.setItem('hasDraftSubscription', 'false');
    }
    
    // NOW set auth state after customer status is known
    setAuthState({
      user,
      token: null,
      isAuthenticated: true,
      isLoading: false,
    });
    
    return { isCustomer: customerStatusValue };
  };

  const logout = async () => {
    try {
      // Try to call backend logout, but don't fail if session already expired
      await authService.logout();
    } catch {
      // 401 means session was already invalid - that's fine, we still want to logout client-side
    }
    // Clear sessionStorage flags
    sessionStorage.removeItem('isAuthenticated');
    sessionStorage.removeItem('isCustomer');
    sessionStorage.removeItem('hasDraftSubscription');
    // Immediate hard redirect - page reload will reset auth state naturally
    window.location.href = '/';
  };

  const refreshUserStatus = async () => {
    try {
      const customerStatus = await CustomerService.getCustomerStatus();
      setIsCustomer(customerStatus.isCustomer);
      setHasDraftSubscription(customerStatus.hasDraftSubscription);
      sessionStorage.setItem('isCustomer', String(customerStatus.isCustomer));
      sessionStorage.setItem('hasDraftSubscription', String(customerStatus.hasDraftSubscription));
    } catch {
      // ignore
    }
  };

  const contextValue: AuthContextType = {
    user: authState.user,
    token: authState.token,
    isAuthenticated: authState.isAuthenticated,
    isLoading: authState.isLoading,
    login,
    logout,
    isCustomer,
    hasDraftSubscription,
    refreshUserStatus
  };
  
  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuthContext = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuthContext must be used within an AuthProvider');
  }
  return context;
};
