/**
 * Base64 Utilities
 *
 * This module provides utilities for Base64 encoding and decoding,
 * ensuring compatibility with the backend's Base64 implementation.
 */

/**
 * Encodes a string to Base64
 *
 * @param str The string to encode
 * @returns The Base64 encoded string
 */
export function base64Encode(str: string): string {
  try {
    // Use the browser's btoa function
    return btoa(str);
  } catch (error) {
    console.error('[Base64 Utils] Error encoding to Base64:', error);

    // Fallback for non-ASCII characters
    return btoa(encodeURIComponent(str).replace(/%([0-9A-F]{2})/g, (_, p1) => {
      return String.fromCharCode(parseInt(p1, 16));
    }));
  }
}

/**
 * Decodes a Base64 string
 *
 * @param base64 The Base64 string to decode
 * @returns The decoded string
 */
export function base64Decode(base64: string): string {
  try {
    // Use the browser's atob function
    return atob(base64);
  } catch (error) {
    console.error('[Base64 Utils] Error decoding from Base64:', error);

    // Fallback for non-ASCII characters
    return decodeURIComponent(Array.from(atob(base64), c => {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
  }
}

/**
 * Encodes a string to Base64Url (URL-safe Base64)
 *
 * @param str The string to encode
 * @returns The Base64Url encoded string
 */
export function base64UrlEncode(str: string): string {
  return base64Encode(str)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

/**
 * Decodes a Base64Url string
 *
 * @param base64url The Base64Url string to decode
 * @returns The decoded string
 */
export function base64UrlDecode(base64url: string): string {
  // Convert Base64Url to Base64
  let base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');

  // Add padding if needed
  while (base64.length % 4) {
    base64 += '=';
  }

  return base64Decode(base64);
}

/**
 * Converts a Base64 string to a Uint8Array
 *
 * @param base64 The Base64 string to convert
 * @returns The Uint8Array
 */
export function base64ToUint8Array(base64: string): Uint8Array {
  try {
    // 打印输入的Base64字符串
    console.log(`[Base64 Utils] Converting Base64 to Uint8Array: ${base64.substring(0, 20)}...`);

    // 解码Base64字符串
    const binary = base64Decode(base64);
    const bytes = new Uint8Array(binary.length);

    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }

    // 打印结果的前几个字节
    let bytesHex = '';
    for (let i = 0; i < Math.min(bytes.length, 16); i++) {
      bytesHex += bytes[i].toString(16).padStart(2, '0') + ' ';
    }
    console.log(`[Base64 Utils] Converted to bytes (first 16): ${bytesHex}...`);
    console.log(`[Base64 Utils] Bytes length: ${bytes.length}`);

    return bytes;
  } catch (error) {
    console.error('[Base64 Utils] Error converting Base64 to Uint8Array:', error);
    throw error;
  }
}

/**
 * Converts a Uint8Array to a Base64 string
 *
 * @param bytes The Uint8Array to convert
 * @returns The Base64 string
 */
export function uint8ArrayToBase64(bytes: Uint8Array): string {
  let binary = '';
  const len = bytes.byteLength;

  for (let i = 0; i < len; i++) {
    binary += String.fromCharCode(bytes[i]);
  }

  return base64Encode(binary);
}
