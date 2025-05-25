/**
 * JWT Test Utilities
 * 
 * This module provides utilities for testing JWT authentication.
 */

import { getApiClient } from '../api/client-factory';
import { apiMapping } from '../api/config/api-mapping';

/**
 * Tests JWT authentication by making a request to a protected endpoint
 * 
 * @param token The JWT token to test
 * @returns A Promise that resolves to the test result
 */
export async function testJwtAuthentication(token: string): Promise<any> {
  try {
    console.log('[JWT Test] Testing JWT authentication with token:', token);
    
    // Get the API client
    const apiClient = getApiClient();
    
    // Make a request to a protected endpoint
    const response = await apiClient.get(apiMapping.auth.info.legacy, {
      headers: {
        Authorization: `Bearer ${token}`,
        'X-Token': token
      }
    });
    
    console.log('[JWT Test] Authentication successful:', response.data);
    
    return {
      success: true,
      data: response.data,
      message: 'JWT authentication successful'
    };
  } catch (error) {
    console.error('[JWT Test] Authentication failed:', error);
    
    return {
      success: false,
      error: error,
      message: error.message || 'JWT authentication failed'
    };
  }
}

/**
 * Decodes a JWT token without verifying the signature
 * 
 * @param token The JWT token to decode
 * @returns The decoded token or null if invalid
 */
export function decodeJwtToken(token: string): any {
  try {
    console.log('[JWT Test] Decoding JWT token:', token);
    
    // Split the token into its parts
    const parts = token.split('.');
    
    if (parts.length !== 3) {
      console.error('[JWT Test] Invalid token format');
      return null;
    }
    
    // Decode the header and payload
    const header = JSON.parse(atob(parts[0].replace(/-/g, '+').replace(/_/g, '/')));
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
    
    return {
      header,
      payload,
      signature: parts[2]
    };
  } catch (error) {
    console.error('[JWT Test] Error decoding token:', error);
    return null;
  }
}

/**
 * Tests multiple authentication scenarios
 * 
 * @returns A Promise that resolves to the test results
 */
export async function runAuthenticationTests(): Promise<any> {
  const results: any = {
    storedToken: null,
    decodedToken: null,
    authenticationTest: null
  };
  
  try {
    // Get the stored token
    const token = localStorage.getItem('token');
    results.storedToken = token;
    
    if (!token) {
      results.message = 'No token available';
      return results;
    }
    
    // Decode the token
    results.decodedToken = decodeJwtToken(token);
    
    // Test authentication
    results.authenticationTest = await testJwtAuthentication(token);
    
    return results;
  } catch (error) {
    console.error('[JWT Test] Error running tests:', error);
    
    results.error = error;
    results.message = error.message || 'Error running authentication tests';
    
    return results;
  }
}
