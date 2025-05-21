#!/usr/bin/env node

/**
 * JWT Signature Verification Test
 * 
 * This script tests JWT signature verification with the backend.
 * It creates tokens with different signature algorithms and keys to identify
 * which combination works with the backend.
 */

const axios = require('axios');
const crypto = require('crypto');

// Configuration
const API_BASE_URLS = [
  'http://localhost:8080',  // Default Spring Boot port
  'http://localhost:8081',  // Alternative port
  'http://localhost:3000',  // Default Node.js port
  'http://localhost:4000',  // Alternative Node.js port
  ''                        // Relative URL (same origin)
];
const USER_INFO_ENDPOINT = '/api/auth/user/info';

// Possible secret keys to try
const SECRET_KEYS = [
  'SecretKey012345678901234567890123456789012345678901234567890123456789',
  'secret',
  'jwt-secret',
  'datalink-secret',
  'datalink-jwt-secret',
  'dataflare-secret',
  'dataflare-jwt-secret'
];

/**
 * Creates a JWT token with the specified algorithm and secret
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
    exp: now + 3600 // 1 hour expiration
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
 * Tests a JWT token with the backend
 * 
 * @param {string} token The JWT token to test
 * @returns {Promise<object>} A Promise that resolves to the test result
 */
async function testToken(token) {
  // Try each base URL
  for (const baseUrl of API_BASE_URLS) {
    try {
      const response = await axios.get(`${baseUrl}${USER_INFO_ENDPOINT}`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'X-Token': token
        }
      });
      
      return {
        success: true,
        baseUrl,
        status: response.status,
        data: response.data
      };
    } catch (error) {
      // Continue to the next base URL
    }
  }
  
  // If we get here, all base URLs failed
  return {
    success: false,
    message: 'Failed with all base URLs'
  };
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
async function main() {
  try {
    // Get username from command line arguments
    const username = process.argv[2] || 'admin';
    
    console.log('=== JWT Signature Verification Test ===');
    console.log(`Testing with username: ${username}`);
    
    // Test different algorithms and secrets
    const algorithms = ['HS256', 'HS384', 'HS512'];
    let successfulCombination = null;
    
    for (const algorithm of algorithms) {
      console.log(`\nTesting algorithm: ${algorithm}`);
      
      for (const secret of SECRET_KEYS) {
        process.stdout.write(`  Testing secret: ${secret.substring(0, 10)}... `);
        
        try {
          // Create a token with this algorithm and secret
          const token = createJwtToken(username, algorithm, secret);
          
          // Test the token
          const result = await testToken(token);
          
          if (result.success) {
            console.log('SUCCESS!');
            console.log(`    Base URL: ${result.baseUrl || '(relative)'}`);
            console.log(`    Status: ${result.status}`);
            console.log(`    Data: ${JSON.stringify(result.data)}`);
            
            successfulCombination = {
              algorithm,
              secret,
              token,
              result
            };
            
            // Break out of the loops
            break;
          } else {
            console.log('FAILED');
          }
        } catch (error) {
          console.log(`ERROR: ${error.message}`);
        }
      }
      
      if (successfulCombination) {
        break;
      }
    }
    
    if (successfulCombination) {
      console.log('\n=== Successful Combination Found ===');
      console.log(`Algorithm: ${successfulCombination.algorithm}`);
      console.log(`Secret: ${successfulCombination.secret}`);
      console.log(`Token: ${successfulCombination.token}`);
      
      // Decode the token
      const decoded = decodeToken(successfulCombination.token);
      console.log('\nDecoded Token:');
      console.log(JSON.stringify(decoded, null, 2));
      
      console.log('\n=== JWT Signature Verification Successful ===');
      console.log('Use this algorithm and secret key in your application.');
    } else {
      console.log('\n=== No Successful Combination Found ===');
      console.log('None of the tested algorithms and secrets worked with the backend.');
      console.log('You may need to check the backend configuration or try different secrets.');
      process.exit(1);
    }
  } catch (error) {
    console.error('\n=== Test Failed ===');
    console.error(`Error: ${error.message}`);
    process.exit(1);
  }
}

// Run the main function
main();
