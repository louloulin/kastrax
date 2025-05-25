/**
 * Authentication Proxy
 * 
 * This module provides a proxy to the authentication API that handles
 * JWT token generation and validation on the server side.
 */

import { getApiClient } from './client-factory';
import { apiMapping } from './config/api-mapping';
import { transformResponse } from './adapter/response';

// Get the API client
const apiClient = getApiClient();

/**
 * Authenticates a user and gets a valid JWT token from the server
 * 
 * @param username The username
 * @param password The password
 * @returns A Promise that resolves to the authentication response
 */
export async function authenticateUser(username: string, password: string): Promise<any> {
  try {
    console.log('[Auth Proxy] Authenticating user:', username);
    
    // Make a direct request to the authentication endpoint
    const response = await apiClient.post(apiMapping.auth.login.legacy, {
      username,
      password
    });
    
    // Extract the token from the response
    const result = transformResponse(response.data);
    const token = result.token || result.accessToken || null;
    
    if (token) {
      console.log('[Auth Proxy] Authentication successful, token received');
      
      // Store the token and username in localStorage
      localStorage.setItem('token', token);
      localStorage.setItem('username', username);
      
      return {
        success: true,
        token,
        username,
        ...result
      };
    } else {
      console.error('[Auth Proxy] Authentication successful but no token received');
      return {
        success: false,
        message: 'No token received from server'
      };
    }
  } catch (error) {
    console.error('[Auth Proxy] Authentication failed:', error);
    return {
      success: false,
      message: error.message || 'Authentication failed'
    };
  }
}

/**
 * Gets user information using the stored token
 * 
 * @returns A Promise that resolves to the user information
 */
export async function getUserInfo(): Promise<any> {
  try {
    console.log('[Auth Proxy] Getting user info');
    
    // Get the token from localStorage
    const token = localStorage.getItem('token');
    if (!token) {
      console.error('[Auth Proxy] No token available');
      return {
        success: false,
        message: 'No authentication token available'
      };
    }
    
    // Make a request to the user info endpoint with the token
    const response = await apiClient.get(apiMapping.auth.info.legacy, {
      headers: {
        Authorization: `Bearer ${token}`,
        'X-Token': token
      }
    });
    
    // Transform the response
    const result = transformResponse(response.data);
    console.log('[Auth Proxy] User info received:', result);
    
    return {
      success: true,
      ...result
    };
  } catch (error) {
    console.error('[Auth Proxy] Failed to get user info:', error);
    
    // If the error is related to token validation, clear the token
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      console.warn('[Auth Proxy] Token validation failed, clearing token');
      localStorage.removeItem('token');
    }
    
    return {
      success: false,
      message: error.message || 'Failed to get user information'
    };
  }
}
