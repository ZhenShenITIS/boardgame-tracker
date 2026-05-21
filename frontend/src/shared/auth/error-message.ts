type ApiErrorPayload = {
  error?: {
    message?: string;
  };
};

export function getAuthErrorMessage(error: unknown, fallback: string) {
  if (
    typeof error === 'object' &&
    error &&
    'response' in error &&
    typeof error.response === 'object' &&
    error.response &&
    'data' in error.response
  ) {
    const data = error.response.data as ApiErrorPayload;

    if (data?.error?.message) {
      return data.error.message;
    }
  }

  return fallback;
}
