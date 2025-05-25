/**
 * Token Refresh Utility
 *
 * This module provides utilities for checking if a token needs to be refreshed
 * and refreshing it if necessary.
 */

import { createJwtToken, isValidJwtFormat, decodeJwt, isTokenExpired, verifyJwt } from './jwt-utils';

/**
 * Checks if the token needs to be refreshed and refreshes it if necessary
 *
 * @returns The current valid token or a Promise that resolves to the token
 */
export function checkAndRefreshToken(): string | null | Promise<string | null> {
  // Get the current token from localStorage
  const token = typeof localStorage !== 'undefined' ? localStorage.getItem('token') : null;

  // If there's no token, return null
  if (!token) {
    console.log('[Token Refresh] No token found');
    return null;
  }

  // First, verify the token using the JWT library
  const verifyResult = verifyJwt(token);

  // Handle both synchronous and asynchronous verification results
  if (verifyResult instanceof Promise) {
    // Handle asynchronous verification (Web Crypto API)
    return verifyResult.then(verified => {
      if (verified) {
        console.log('[Token Refresh] Token is valid and verified (async)');
        return token;
      }

      // If verification fails, check if the token is at least valid in format and not expired
      if (isValidJwtFormat(token) && !isTokenExpired(token)) {
        console.log('[Token Refresh] Token format is valid but signature verification failed (async), creating new token');
      } else {
        console.log('[Token Refresh] Token is invalid or expired (async)');
      }

      // Create a new token
      const username = localStorage.getItem('username') || 'unknown';
      const newTokenResult = createJwtToken(username);

      if (newTokenResult instanceof Promise) {
        // Handle asynchronous token creation
        return newTokenResult.then(newToken => {
          // Store the new token in localStorage
          localStorage.setItem('token', newToken);
          console.log('[Token Refresh] Token refreshed (async)');
          return newToken;
        });
      } else {
        // Handle synchronous token creation
        localStorage.setItem('token', newTokenResult);
        console.log('[Token Refresh] Token refreshed (sync from async)');
        return newTokenResult;
      }
    });
  } else {
    // Handle synchronous verification (jsonwebtoken library)
    if (verifyResult) {
      console.log('[Token Refresh] Token is valid and verified (sync)');
      return token;
    }

    // If verification fails, check if the token is at least valid in format and not expired
    if (isValidJwtFormat(token) && !isTokenExpired(token)) {
      console.log('[Token Refresh] Token format is valid but signature verification failed (sync), creating new token');
    } else {
      console.log('[Token Refresh] Token is invalid or expired (sync)');
    }

    // Create a new token
    const username = localStorage.getItem('username') || 'unknown';
    const newTokenResult = createJwtToken(username);

    if (newTokenResult instanceof Promise) {
      // Handle asynchronous token creation
      return newTokenResult.then(newToken => {
        // Store the new token in localStorage
        localStorage.setItem('token', newToken);
        console.log('[Token Refresh] Token refreshed (async from sync)');
        return newToken;
      });
    } else {
      // Handle synchronous token creation
      localStorage.setItem('token', newTokenResult);
      console.log('[Token Refresh] Token refreshed (sync)');
      return newTokenResult;
    }
  }
}

/**
 * Refreshes the token if it's about to expire (within the next hour)
 *
 * @returns The current valid token or a Promise that resolves to the token
 */
export function refreshTokenIfNeeded(): string | null | Promise<string | null> {
  // Get the current token from localStorage
  const token = typeof localStorage !== 'undefined' ? localStorage.getItem('token') : null;

  // If there's no token, return null
  if (!token) {
    console.log('[Token Refresh] No token found');
    return null;
  }

  // First, verify the token using the JWT library
  const verifyResult = verifyJwt(token);

  // Handle both synchronous and asynchronous verification results
  if (verifyResult instanceof Promise) {
    // Handle asynchronous verification (Web Crypto API)
    return verifyResult.then(verified => {
      if (!verified) {
        console.log('[Token Refresh] Token verification failed (async), refreshing token');
        return checkAndRefreshToken();
      }

      // Check if the token is about to expire (within the next hour)
      const now = Math.floor(Date.now() / 1000);
      const oneHour = 60 * 60;

      if (verified.exp - now < oneHour) {
        console.log('[Token Refresh] Token is about to expire (async), refreshing');
        return checkAndRefreshToken();
      }

      console.log('[Token Refresh] Token is valid and not about to expire (async)');
      return token;
    });
  } else {
    // Handle synchronous verification (jsonwebtoken library)
    if (!verifyResult) {
      console.log('[Token Refresh] Token verification failed (sync), refreshing token');
      return checkAndRefreshToken();
    }

    // Check if the token is about to expire (within the next hour)
    const now = Math.floor(Date.now() / 1000);
    const oneHour = 60 * 60;

    if (verifyResult.exp - now < oneHour) {
      console.log('[Token Refresh] Token is about to expire (sync), refreshing');
      return checkAndRefreshToken();
    }

    console.log('[Token Refresh] Token is valid and not about to expire (sync)');
    return token;
  }
}
