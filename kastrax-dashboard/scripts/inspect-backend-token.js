#!/usr/bin/env node

/**
 * Backend Token Inspector
 * 
 * This script authenticates with the backend and inspects the JWT token
 * to understand its structure and signature algorithm.
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
const LOGIN_ENDPOINT = '/api/auth/login';

/**
 * Authenticates with the backend and gets a JWT token
 * 
 * @param {string} username The username
 * @param {string} password The password
 * @returns {Promise<string>} A Promise that resolves to the JWT token
 */
async function authenticate(username, password) {
  // Try each base URL
  for (const baseUrl of API_BASE_URLS) {
    console.log(`\nTrying API Base URL: ${baseUrl || '(relative)'}`);
    
    try {
      // Try form data approach
      const formData = new URLSearchParams();
      formData.append('username', username);
      formData.append('password', password);
      
      try {
        console.log(`POST ${baseUrl}${LOGIN_ENDPOINT} (form data)`);
        const response = await axios.post(`${baseUrl}${LOGIN_ENDPOINT}`, formData, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          }
        });
        
        console.log(`Response status: ${response.status}`);
        
        const token = response.data.token || response.data.accessToken || null;
        
        if (token) {
          console.log('Token received (form data approach)');
          return token;
        } else {
          console.log('No token in response (form data approach)');
        }
      } catch (formError) {
        console.log(`Form data approach failed: ${formError.message}`);
        
        // Try JSON approach
        try {
          console.log(`POST ${baseUrl}${LOGIN_ENDPOINT} (JSON)`);
          const response = await axios.post(`${baseUrl}${LOGIN_ENDPOINT}`, {
            username,
            password
          }, {
            headers: {
              'Content-Type': 'application/json'
            }
          });
          
          console.log(`Response status: ${response.status}`);
          
          const token = response.data.token || response.data.accessToken || null;
          
          if (token) {
            console.log('Token received (JSON approach)');
            return token;
          } else {
            console.log('No token in response (JSON approach)');
          }
        } catch (jsonError) {
          console.log(`JSON approach failed: ${jsonError.message}`);
        }
      }
    } catch (error) {
      console.log(`Failed with base URL ${baseUrl}: ${error.message}`);
    }
  }
  
  throw new Error('Authentication failed with all base URLs');
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
 * Verifies a JWT token signature
 * 
 * @param {string} token The JWT token to verify
 * @param {string} secret The secret key to use
 * @returns {boolean} Whether the signature is valid
 */
function verifySignature(token, secret) {
  const parts = token.split('.');
  
  if (parts.length !== 3) {
    return false;
  }
  
  const [header, payload, signature] = parts;
  const data = `${header}.${payload}`;
  
  try {
    // Decode the header to get the algorithm
    const decodedHeader = JSON.parse(Buffer.from(header, 'base64').toString());
    const algorithm = decodedHeader.alg;
    
    let hmac;
    if (algorithm === 'HS256') {
      hmac = crypto.createHmac('sha256', secret);
    } else if (algorithm === 'HS384') {
      hmac = crypto.createHmac('sha384', secret);
    } else if (algorithm === 'HS512') {
      hmac = crypto.createHmac('sha512', secret);
    } else {
      throw new Error(`Unsupported algorithm: ${algorithm}`);
    }
    
    const expectedSignature = hmac.update(data).digest('base64')
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
    
    return signature === expectedSignature;
  } catch (error) {
    console.error(`Error verifying signature: ${error.message}`);
    return false;
  }
}

/**
 * Main function
 */
async function main() {
  try {
    // Get username and password from command line arguments
    const username = process.argv[2] || 'admin';
    const password = process.argv[3] || 'datalink';
    
    console.log('=== Backend Token Inspector ===');
    console.log(`Username: ${username}`);
    console.log(`Password: ${'*'.repeat(password.length)}`);
    
    // Authenticate and get token
    const token = await authenticate(username, password);
    console.log('\nToken:', token);
    
    // Decode token
    const decoded = decodeToken(token);
    console.log('\nDecoded Token:');
    console.log(JSON.stringify(decoded, null, 2));
    
    // Try to verify the signature with common secrets
    console.log('\nTrying to verify signature with common secrets...');
    
    const commonSecrets = [
      'SecretKey012345678901234567890123456789012345678901234567890123456789',
      'secret',
      'jwt-secret',
      'datalink-secret',
      'datalink-jwt-secret',
      'dataflare-secret',
      'dataflare-jwt-secret'
    ];
    
    let verified = false;
    for (const secret of commonSecrets) {
      process.stdout.write(`  ${secret.substring(0, 10)}... `);
      
      if (verifySignature(token, secret)) {
        console.log('VALID');
        console.log(`\nSignature verified with secret: ${secret}`);
        verified = true;
        break;
      } else {
        console.log('INVALID');
      }
    }
    
    if (!verified) {
      console.log('\nCould not verify signature with any of the common secrets.');
      console.log('You may need to extract the secret from the backend code.');
    }
    
    console.log('\n=== Inspection Complete ===');
  } catch (error) {
    console.error('\n=== Inspection Failed ===');
    console.error(`Error: ${error.message}`);
    process.exit(1);
  }
}

// Run the main function
main();
