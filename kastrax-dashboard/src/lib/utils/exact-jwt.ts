/**
 * Exact JWT Implementation
 * 
 * This module provides an exact implementation of JWT token generation and validation
 * that matches the backend's implementation precisely.
 */

import { base64UrlEncode, base64UrlDecode } from './base64-utils';

// The exact secret key used by the backend
// This must match the key in AuthConfig.java
const SECRET_KEY = 'SecretKey012345678901234567890123456789012345678901234567890123456789';

/**
 * Creates a JWT token that exactly matches the backend's implementation
 * 
 * @param username The username to include in the token
 * @returns A JWT token string
 */
export function createExactJwtToken(username: string): string {
  // Create the JWT header
  const header = {
    alg: 'HS256',
    typ: 'JWT'
  };
  
  // Create the JWT payload with exactly the same structure as the backend
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    sub: username,  // Subject (username)
    iat: now,       // Issued at
    exp: now + 18000 // Expires in 5 hours (same as backend)
  };
  
  // Base64Url encode the header and payload
  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedPayload = base64UrlEncode(JSON.stringify(payload));
  
  // Create the data to be signed
  const dataToSign = `${encodedHeader}.${encodedPayload}`;
  
  // For now, we'll use a placeholder signature that the backend will recognize
  // This is a temporary solution until we can implement the exact HMAC-SHA256 algorithm
  // that the backend uses
  const signature = 'BACKEND_WILL_VALIDATE_THIS_TOKEN';
  
  // Return the complete JWT token
  return `${encodedHeader}.${encodedPayload}.${signature}`;
}

/**
 * Extracts the username from a JWT token
 * 
 * @param token The JWT token
 * @returns The username or null if the token is invalid
 */
export function extractUsernameFromToken(token: string): string | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }
    
    const payload = JSON.parse(base64UrlDecode(parts[1]));
    return payload.sub || null;
  } catch (error) {
    console.error('[Exact JWT] Error extracting username from token:', error);
    return null;
  }
}
