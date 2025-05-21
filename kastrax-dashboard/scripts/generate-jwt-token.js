#!/usr/bin/env node

/**
 * JWT Token Generator
 * 
 * This script generates a JWT token that is compatible with the backend.
 */

const crypto = require('crypto');

// Configuration
const DEFAULT_SECRET = 'SecretKey012345678901234567890123456789012345678901234567890123456789';
const DEFAULT_ALGORITHM = 'HS256';

/**
 * Creates a JWT token
 * 
 * @param {string} username The username to include in the token
 * @param {string} algorithm The signing algorithm to use
 * @param {string} secret The secret key to use
 * @returns {string} The JWT token
 */
function createJwtToken(username, algorithm, secret) {
  // Create the JWT header
  const header = {
    alg: algorithm,
    typ: 'JWT'
  };
  
  // Create the JWT payload
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    sub: username,
    name: username,
    iat: now,
    exp: now + 18000 // 5 hours expiration
  };
  
  // Base64Url encode the header and payload
  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedPayload = base64UrlEncode(JSON.stringify(payload));
  
  // Create the data to be signed
  const dataToSign = `${encodedHeader}.${encodedPayload}`;
  
  // Sign the data
  let signature;
  if (algorithm === 'HS256') {
    signature = crypto.createHmac('sha256', secret).update(dataToSign).digest('base64');
  } else if (algorithm === 'HS384') {
    signature = crypto.createHmac('sha384', secret).update(dataToSign).digest('base64');
  } else if (algorithm === 'HS512') {
    signature = crypto.createHmac('sha512', secret).update(dataToSign).digest('base64');
  } else {
    throw new Error(`Unsupported algorithm: ${algorithm}`);
  }
  
  // Convert to base64url
  signature = signature.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  
  // Return the complete JWT token
  return `${encodedHeader}.${encodedPayload}.${signature}`;
}

/**
 * Base64Url encodes a string
 * 
 * @param {string} str The string to encode
 * @returns {string} The base64url encoded string
 */
function base64UrlEncode(str) {
  return Buffer.from(str).toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

/**
 * Decodes a JWT token
 * 
 * @param {string} token The JWT token to decode
 * @returns {object} The decoded token
 */
function decodeToken(token) {
  const parts = token.split('.');
  
  if (parts.length !== 3) {
    throw new Error('Invalid token format');
  }
  
  const header = JSON.parse(Buffer.from(parts[0], 'base64').toString());
  const payload = JSON.parse(Buffer.from(parts[1], 'base64').toString());
  
  return {
    header,
    payload,
    signature: parts[2]
  };
}

/**
 * Main function
 */
function main() {
  try {
    // Get parameters from command line arguments
    const username = process.argv[2] || 'admin';
    const algorithm = process.argv[3] || DEFAULT_ALGORITHM;
    const secret = process.argv[4] || DEFAULT_SECRET;
    
    console.log('=== JWT Token Generator ===');
    console.log(`Username: ${username}`);
    console.log(`Algorithm: ${algorithm}`);
    console.log(`Secret: ${secret.substring(0, 10)}...`);
    
    // Generate token
    const token = createJwtToken(username, algorithm, secret);
    console.log('\nGenerated Token:');
    console.log(token);
    
    // Decode token
    const decoded = decodeToken(token);
    console.log('\nDecoded Token:');
    console.log(JSON.stringify(decoded, null, 2));
    
    console.log('\n=== Token Generation Complete ===');
    console.log('You can use this token for testing by setting it in localStorage:');
    console.log(`localStorage.setItem('token', '${token}');`);
    console.log(`localStorage.setItem('username', '${username}');`);
  } catch (error) {
    console.error('\n=== Token Generation Failed ===');
    console.error(`Error: ${error.message}`);
    process.exit(1);
  }
}

// Run the main function
main();
