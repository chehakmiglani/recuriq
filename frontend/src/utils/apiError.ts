/** Error thrown by the API helpers, carrying the HTTP status and any field errors from the backend. */
export interface ApiError extends Error {
  status?: number;
  validationErrors?: Record<string, string>;
}

/** The message of a caught value, or '' when it isn't an Error. */
export const errorMessage = (e: unknown): string =>
  e instanceof Error ? e.message : typeof e === 'string' ? e : '';

/** Treats a caught value as an ApiError so its status and field errors can be read. */
export const asApiError = (e: unknown): ApiError =>
  (e instanceof Error ? e : new Error(errorMessage(e))) as ApiError;
