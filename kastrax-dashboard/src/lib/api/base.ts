/**
 * Returns the base URL for API requests.
 * In a production environment, this would typically be configured to point to the API server.
 * For development, it can use a local environment variable or a default value.
 */
export function getBaseUrl(): string {
  // Always return an empty path for tests
  if (process.env.NODE_ENV === 'test') {
    return '';
  }

  // For non-test environments, return an empty string to avoid path duplication
  // The actual API paths are defined in the apiMapping configuration
  return '';
}