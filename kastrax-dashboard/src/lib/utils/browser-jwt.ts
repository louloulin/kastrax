/**
 * Browser JWT Utilities
 *
 * This module provides utilities for working with JWT tokens in the browser environment,
 * using the Web Crypto API for HMAC-SHA256 signing.
 */

import { base64UrlEncode, base64UrlDecode, base64ToUint8Array } from './base64-utils';

// The secret key must match the one used in the backend (JwtProperties.java)
// In the backend, this key is converted to bytes directly:
// secretKeyBytes = "SecretKey012345678901234567890123456789012345678901234567890123456789".getBytes()
const SECRET_KEY = 'SecretKey012345678901234567890123456789012345678901234567890123456789';

/**
 * Converts a string to a Uint8Array
 *
 * @param str The string to convert
 * @returns The Uint8Array
 */
function stringToUint8Array(str: string): Uint8Array {
  const encoder = new TextEncoder();
  return encoder.encode(str);
}

/**
 * Creates a JWT token using the Web Crypto API
 *
 * @param username The username to include in the token
 * @returns A Promise that resolves to a JWT token string
 */
export async function createJwtTokenWithWebCrypto(username: string): Promise<string> {
  try {
    // Create the JWT header
    const header = {
      alg: 'HS256',
      typ: 'JWT'
    };

    // Create the JWT payload
    const now = Math.floor(Date.now() / 1000);
    const payload = {
      sub: username,
      exp: now + (18000) // 5 hours expiration (matching backend)
    };

    // Encode header and payload
    const encodedHeader = base64UrlEncode(JSON.stringify(header));
    const encodedPayload = base64UrlEncode(JSON.stringify(payload));

    // Data to sign
    const dataToSign = `${encodedHeader}.${encodedPayload}`;

    // Convert the secret key to a Uint8Array
    // 直接使用字符串的UTF-8编码，不尝试Base64解码
    const keyData = stringToUint8Array(SECRET_KEY);

    // Import the key for HMAC-SHA256
    const key = await window.crypto.subtle.importKey(
      'raw',
      keyData,
      { name: 'HMAC', hash: { name: 'SHA-256' } },
      false,
      ['sign']
    );

    // Sign the data
    const signature = await window.crypto.subtle.sign(
      'HMAC',
      key,
      stringToUint8Array(dataToSign)
    );

    // Convert the signature to a Base64Url string
    const signatureArray = new Uint8Array(signature);

    // 打印签名字节以便调试
    let signatureHex = '';
    for (let i = 0; i < Math.min(signatureArray.length, 16); i++) {
      signatureHex += signatureArray[i].toString(16).padStart(2, '0') + ' ';
    }
    console.log(`[Browser JWT] Signature bytes (first 16): ${signatureHex}...`);

    let signatureString = '';
    for (let i = 0; i < signatureArray.length; i++) {
      signatureString += String.fromCharCode(signatureArray[i]);
    }
    const signatureBase64 = base64UrlEncode(signatureString);

    // Return the complete JWT token
    const token = `${encodedHeader}.${encodedPayload}.${signatureBase64}`;
    console.log(`[Browser JWT] Created token: ${token}`);
    return token;
  } catch (error) {
    console.error('[Browser JWT] Error creating JWT token with Web Crypto API:', error);
    throw error;
  }
}

/**
 * Verifies a JWT token using the Web Crypto API
 *
 * @param token The token to verify
 * @returns A Promise that resolves to the decoded token payload if valid, null otherwise
 */
export async function verifyJwtTokenWithWebCrypto(token: string): Promise<any> {
  if (!token) return null;

  try {
    // Split the token
    const parts = token.split('.');
    if (parts.length !== 3) return null;

    const [encodedHeader, encodedPayload, encodedSignature] = parts;

    // Data that was signed
    const dataToVerify = `${encodedHeader}.${encodedPayload}`;

    // Convert the secret key to a Uint8Array
    // 直接使用字符串的UTF-8编码，不尝试Base64解码
    const keyData = stringToUint8Array(SECRET_KEY);

    // Import the key for HMAC-SHA256
    const key = await window.crypto.subtle.importKey(
      'raw',
      keyData,
      { name: 'HMAC', hash: { name: 'SHA-256' } },
      false,
      ['verify']
    );

    // Convert the signature from Base64Url to Uint8Array
    const signatureString = base64UrlDecode(encodedSignature);
    const signatureArray = new Uint8Array(signatureString.length);
    for (let i = 0; i < signatureString.length; i++) {
      signatureArray[i] = signatureString.charCodeAt(i);
    }

    // Verify the signature
    const isValid = await window.crypto.subtle.verify(
      'HMAC',
      key,
      signatureArray,
      stringToUint8Array(dataToVerify)
    );

    if (!isValid) return null;

    // Decode the payload
    const payload = JSON.parse(base64UrlDecode(encodedPayload));

    // Check if the token is expired
    const now = Math.floor(Date.now() / 1000);
    if (payload.exp && payload.exp < now) return null;

    return payload;
  } catch (error) {
    console.error('[Browser JWT] Error verifying JWT token with Web Crypto API:', error);
    return null;
  }
}
