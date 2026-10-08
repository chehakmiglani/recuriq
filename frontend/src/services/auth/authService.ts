import type { LoginRequest, RegisterRequest } from './authTypes';
import type { User } from '../../types/auth.types';

// API base URL — proxied to backend by both CRA (setupProxy.js) and Vite (vite.config.ts)
const API_BASE_URL = '/api';

/**
 * Reads the XSRF-TOKEN cookie set by Spring Security's CookieCsrfTokenRepository.
 * Returns the token value, or null if the cookie is not present.
 */
function getCsrfToken(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}

/**
 * Native fetch wrapper that ensures:
 * 1. JSESSIONID cookies are always sent/received (credentials: 'include')
 * 2. CSRF token is attached as X-XSRF-TOKEN header on mutating requests
 *
 * Returns the Response object so callers can check status and parse body as needed.
 */
export const fetchWithSession = async (endpoint: string, options: RequestInit = {}) => {
  const method = (options.method || 'GET').toUpperCase();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  // Attach CSRF token on state-changing requests (Spring Security expects this)
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    const csrfToken = getCsrfToken();
    if (csrfToken) {
      headers['X-XSRF-TOKEN'] = csrfToken;
    }
  }

  const defaultOptions: RequestInit = {
    ...options,
    credentials: 'include', // Extremely vital for Stateful Spring Security Sessions!
    headers,
  };
  
  // If endpoint is already a full URL (starts with http), use it directly
  // Otherwise, prepend the base URL
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
  const response = await fetch(url, defaultOptions);
  
  if (!response.ok) {
    const errorText = await response.text();
    try {
      // If the backend sent our beautiful ErrorInfo JSON object, extract just the message!
      const errorJson = JSON.parse(errorText);
      const customError: any = new Error(errorJson.message || 'Authentication API error');
      customError.status = response.status;
      
      // If GlobalExceptionHandler attached field-level constraints, pass them forward
      if (errorJson.validationErrors) {
        customError.validationErrors = errorJson.validationErrors;
      }
      throw customError;
    } catch(e) {
      // If it isn't JSON, just throw the raw error message so we still catch it
      if (e instanceof SyntaxError) {
        const rawError: any = new Error(errorText || 'Authentication API error');
        rawError.status = response.status;
        throw rawError;
      }
      throw e;
    }
  }
  
  // Return the Response object so callers can use .json(), .text(), check status, etc.
  return response;
};

export const authService = {
  customerLogin: async (request: LoginRequest): Promise<User> => {
    const response = await fetchWithSession('/customer/login', {
      method: 'POST',
      body: JSON.stringify(request),
    });
    return response.json();
  },

  managerLogin: async (request: LoginRequest): Promise<User> => {
    const response = await fetchWithSession('/manager/login', {
      method: 'POST',
      body: JSON.stringify(request),
    });
    return response.json();
  },
  // NOTE: There is NO manager/register endpoint - managers are created by admin only

  customerRegister: async (request: RegisterRequest): Promise<User> => {
    const response = await fetchWithSession('/customer/register', {
      method: 'POST',
      body: JSON.stringify(request),
    });
    return response.json();
  },
  
  // Special endpoint to securely resume the session when the page refreshes
  checkSession: async (): Promise<User> => {
    const response = await fetchWithSession('/auth/me', {
      method: 'GET',
    });
    return response.json();
  },
  
  logout: async (): Promise<void> => {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    // Attach CSRF token for the logout POST request
    const csrfToken = getCsrfToken();
    if (csrfToken) {
      headers['X-XSRF-TOKEN'] = csrfToken;
    }

    const response = await fetch('/logout', {
      method: 'POST',
      credentials: 'include',
      headers,
    });
    if (!response.ok) {
      const text = await response.text();
      throw new Error(`Logout failed: ${response.status} ${text}`);
    }
  }
};
