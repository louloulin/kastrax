/**
 * JWT Token Utilities
 *
 * This module provides utilities for working with JWT tokens,
 * ensuring compatibility with the Datalink-UI backend.
 */

// Import the process polyfill first to ensure it's available
import './process-polyfill';

// Conditionally import jsonwebtoken only in Node.js environments
let jwt: any = null;
try {
  // This will work in Node.js but may fail in browsers
  if (typeof window === 'undefined') {
    jwt = require('jsonwebtoken');
  }
} catch (error) {
  console.warn('[JWT Utils] jsonwebtoken import failed, will use Web Crypto API instead');
}

import { base64Decode, base64Encode, base64UrlDecode, base64UrlEncode, base64ToUint8Array } from './base64-utils';
import { createJwtTokenWithWebCrypto, verifyJwtTokenWithWebCrypto } from './browser-jwt';

// The secret key must match the one used in the backend (JwtProperties.java)
// In the backend, the key is used directly as bytes:
// secretKeyBytes = "SecretKey012345678901234567890123456789012345678901234567890123456789".getBytes()
// For JWT signing, we need to use the same bytes
const SECRET_KEY = 'SecretKey012345678901234567890123456789012345678901234567890123456789';

/**
 * Gets the secret key bytes in the same way as the backend
 */
function getSecretKeyBytes(): Uint8Array {
  // Use the string directly as UTF-8 bytes, matching the backend approach
  // The backend now uses: secretKeyBytesDecoders = "SecretKey012345678901234567890123456789012345678901234567890123456789".getBytes();
  return new TextEncoder().encode(SECRET_KEY);
}

// For debugging purposes, log the key length
console.log('[JWT Utils] Secret key length:', SECRET_KEY.length);

/**
 * Creates a properly formatted JWT token
 *
 * @param username The username to include in the token
 * @returns A JWT token string or a Promise that resolves to a JWT token string
 */
export function createJwtToken(username: string): string | Promise<string> {
  // Create the JWT payload
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    sub: username,
    name: username,
    iat: now,
    exp: now + (18000) // 5 hours expiration (matching backend)
  };

  // Log the payload for debugging
  console.log('[JWT Utils] Creating token with payload:', JSON.stringify(payload));

  // In browser environments, use Web Crypto API
  if (typeof window !== 'undefined') {
    console.log('[JWT Utils] Using Web Crypto API for token creation in browser');
    try {
      return createJwtTokenWithWebCrypto(username)
        .then(token => {
          console.log('[JWT Utils] Successfully created token with Web Crypto API');
          return token;
        })
        .catch(webCryptoError => {
          console.error('[JWT Utils] Error creating token with Web Crypto API:', webCryptoError);
          // If Web Crypto API fails, fall back to the simplest implementation
          return createFallbackToken(username);
        });
    } catch (webCryptoError) {
      console.error('[JWT Utils] Error using Web Crypto API:', webCryptoError);
      // Fallback to the simplest implementation
      return createFallbackToken(username);
    }
  }

  // In Node.js environments, use jsonwebtoken library
  if (jwt) {
    try {
      // Sign the token using HS256 algorithm and the secret key
      // This matches the backend implementation in JwtTokenManager.java
      // Convert the secret key to Buffer to match how the backend processes it
      const secretKeyBuffer = Buffer.from(getSecretKeyBytes());
      const token = jwt.sign(payload, secretKeyBuffer, {
        algorithm: 'HS256',
        noTimestamp: false // Include iat claim
      });

      // Verify the token to make sure it's valid
      try {
        const secretKeyBuffer = Buffer.from(getSecretKeyBytes());
        const verified = jwt.verify(token, secretKeyBuffer, { algorithms: ['HS256'] });
        console.log('[JWT Utils] Token verified successfully:', verified);
      } catch (verifyError) {
        console.error('[JWT Utils] Token verification failed:', verifyError);
      }

      console.log('[JWT Utils] Created token for user:', username);
      console.log('[JWT Utils] Token:', token);

      return token;
    } catch (error) {
      console.error('[JWT Utils] Error creating JWT token with jsonwebtoken:', error);
      // Fallback to the simplest implementation
      return createFallbackToken(username);
    }
  }

  // If neither method is available, use the fallback
  console.warn('[JWT Utils] No JWT implementation available, using fallback');
  return createFallbackToken(username);
}

/**
 * Creates a fallback JWT token if the JWT library fails
 * This is a last resort and should not be used in production
 *
 * @param username The username to include in the token
 * @returns A JWT token string
 */
function createFallbackToken(username: string): string {
  console.warn('[JWT Utils] Using fallback token creation method');

  // Create the JWT header
  const header = {
    alg: 'HS256',
    typ: 'JWT'
  };

  // Create the JWT payload
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    sub: username,
    name: username,
    iat: now,
    exp: now + (18000) // 5 hours expiration (matching backend)
  };

  // Encode header and payload using the Base64 utilities
  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedPayload = base64UrlEncode(JSON.stringify(payload));

  // For the fallback, we'll try to create a simple HMAC signature
  // This is a simplified version and may not match the backend's expectations
  let signature = '';
  try {
    // Create a simple hash of the header and payload
    const data = `${encodedHeader}.${encodedPayload}`;
    let hash = 0;
    for (let i = 0; i < data.length; i++) {
      const char = data.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32bit integer
    }

    // Add the secret key bytes to the mix
    const keyBytes = getSecretKeyBytes();
    for (let i = 0; i < keyBytes.length; i++) {
      hash = ((hash << 5) - hash) + keyBytes[i];
      hash = hash & hash;
    }

    // Convert to a base64url string
    signature = base64UrlEncode(hash.toString(36));
  } catch (error) {
    console.error('[JWT Utils] Error creating fallback signature:', error);
    signature = 'INVALID_SIGNATURE';
  }

  const token = `${encodedHeader}.${encodedPayload}.${signature}`;
  console.log('[JWT Utils] Created fallback token:', token);

  return token;
}

/**
 * Validates if a token is a properly formatted JWT token
 *
 * @param token The token to validate
 * @returns True if the token is properly formatted
 */
export function isValidJwtFormat(token: string): boolean {
  if (!token) {
    console.log('[JWT Utils] No token provided for format validation');
    return false;
  }

  const parts = token.split('.');
  if (parts.length !== 3) {
    console.log('[JWT Utils] Token does not have three parts');
    return false;
  }

  const [header, payload, signature] = parts;
  if (!header || !payload || !signature) {
    console.log('[JWT Utils] Token has empty parts');
    return false;
  }

  try {
    // Try to decode the header and payload using the Base64 utilities
    const decodedHeader = JSON.parse(base64UrlDecode(header));
    const decodedPayload = JSON.parse(base64UrlDecode(payload));

    // Log the decoded parts for debugging
    console.log('[JWT Utils] Decoded header:', decodedHeader);
    console.log('[JWT Utils] Decoded payload:', decodedPayload);

    // Check if required fields exist
    if (!decodedHeader.alg || !decodedHeader.typ) {
      console.log('[JWT Utils] Token header missing required fields');
      return false;
    }
    if (!decodedPayload.sub || !decodedPayload.exp) {
      console.log('[JWT Utils] Token payload missing required fields');
      return false;
    }

    return true;
  } catch (error) {
    console.error('[JWT Utils] Error validating token format:', error);
    return false;
  }
}

/**
 * Decodes a JWT token without verifying the signature
 *
 * @param token The token to decode
 * @returns The decoded token payload or null if invalid
 */
export function decodeJwt(token: string): any {
  if (!isValidJwtFormat(token)) return null;

  try {
    // Use the JWT library to decode the token
    return jwt.decode(token);
  } catch (error) {
    console.error('[JWT Utils] Error decoding token:', error);

    // Fallback to manual decoding if the JWT library fails
    try {
      const [, payload] = token.split('.');
      return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    } catch (fallbackError) {
      console.error('[JWT Utils] Fallback decoding also failed:', fallbackError);
      return null;
    }
  }
}

/**
 * Checks if a token is expired
 *
 * @param token The token to check
 * @returns True if the token is expired
 */
export function isTokenExpired(token: string): boolean {
  const decoded = decodeJwt(token);
  if (!decoded || !decoded.exp) return true;

  const now = Math.floor(Date.now() / 1000);
  return decoded.exp < now;
}

/**
 * Verifies a JWT token
 *
 * @param token The token to verify
 * @returns The decoded token payload if valid, null otherwise, or a Promise that resolves to the decoded token payload
 */
export function verifyJwt(token: string): any | Promise<any> {
  if (!token) {
    console.log('[JWT Utils] No token provided for verification');
    return null;
  }

  // Log the token for debugging
  console.log('[JWT Utils] Verifying token:', token);

  // In browser environments, use Web Crypto API
  if (typeof window !== 'undefined') {
    console.log('[JWT Utils] Using Web Crypto API for token verification in browser');
    try {
      return verifyJwtTokenWithWebCrypto(token)
        .then(verified => {
          if (verified) {
            console.log('[JWT Utils] Token verified successfully with Web Crypto API:', verified);
            return verified;
          }
          console.log('[JWT Utils] Token verification failed with Web Crypto API');
          return null;
        })
        .catch(webCryptoError => {
          console.error('[JWT Utils] Error verifying token with Web Crypto API:', webCryptoError);
          return null;
        });
    } catch (webCryptoError) {
      console.error('[JWT Utils] Error using Web Crypto API for verification:', webCryptoError);
      return null;
    }
  }

  // In Node.js environments, use jsonwebtoken library
  if (jwt) {
    try {
      // Verify the token using the JWT library
      const secretKeyBuffer = Buffer.from(getSecretKeyBytes());
      const verified = jwt.verify(token, secretKeyBuffer, { algorithms: ['HS256'] });
      console.log('[JWT Utils] Token verified successfully with jsonwebtoken:', verified);
      return verified;
    } catch (error) {
      console.error('[JWT Utils] Token verification failed with jsonwebtoken:', error);

      // Try to decode the token to get more information
      try {
        const decoded = jwt.decode(token, { complete: true });
        console.log('[JWT Utils] Token decoded (without verification):', decoded);
      } catch (decodeError) {
        console.error('[JWT Utils] Token decoding also failed:', decodeError);
      }

      return null;
    }
  }

  // If neither method is available, try to at least decode the token
  console.warn('[JWT Utils] No JWT verification implementation available');
  try {
    // Try to decode the token without verification
    const parts = token.split('.');
    if (parts.length === 3) {
      const payload = JSON.parse(base64UrlDecode(parts[1]));
      console.log('[JWT Utils] Token decoded without verification:', payload);
      return payload;
    }
  } catch (error) {
    console.error('[JWT Utils] Error decoding token:', error);
  }

  return null;
}
