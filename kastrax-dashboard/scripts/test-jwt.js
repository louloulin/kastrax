#!/usr/bin/env node

/**
 * JWT Authentication Test Script
 *
 * This script tests JWT authentication with the backend.
 *
 * Usage:
 *   node test-jwt.js <username> <password>
 */

const axios = require('axios');

// Configuration
// Try multiple possible backend URLs
const API_BASE_URLS = [
  'http://localhost:8080',  // Default Spring Boot port
  'http://localhost:8081',  // Alternative port
  'http://localhost:3000',  // Default Node.js port
  'http://localhost:4000',  // Alternative Node.js port
  ''                        // Relative URL (same origin)
];
const LOGIN_ENDPOINT = '/api/auth/login';
const USER_INFO_ENDPOINT = '/api/auth/user/info';

/**
 * Authenticates a user and gets a JWT token
 *
 * @param {string} username The username
 * @param {string} password The password
 * @returns {Promise<string>} A Promise that resolves to the JWT token
 */
async function authenticate(username, password) {
  console.log(`Authenticating user: ${username}`);

  // Try each base URL until one works
  for (const baseUrl of API_BASE_URLS) {
    console.log(`\nTrying API Base URL: ${baseUrl || '(relative URL)'}`);

    try {
      // Try form data approach first
      const formData = new URLSearchParams();
      formData.append('username', username);
      formData.append('password', password);

      console.log(`Full URL: ${baseUrl}${LOGIN_ENDPOINT}`);
      console.log('Trying form data approach with payload:', formData.toString());

      try {
        const response = await axios.post(`${baseUrl}${LOGIN_ENDPOINT}`, formData, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          }
        });

        console.log('Form data approach response:', response.status);
        console.log('Response headers:', JSON.stringify(response.headers, null, 2));
        console.log('Response data:', JSON.stringify(response.data, null, 2));

        const token = response.data.token || response.data.accessToken || null;

        if (token) {
          console.log('Authentication successful (form data approach)');
          return token;
        } else {
          console.log('No token received in response (form data approach)');
        }
      } catch (formError) {
        console.log('Form data approach failed:', formError.message);
        if (formError.response) {
          console.log('Response status:', formError.response.status);
          console.log('Response headers:', JSON.stringify(formError.response.headers, null, 2));
          console.log('Response data:', JSON.stringify(formError.response.data, null, 2));
        } else if (formError.request) {
          console.log('No response received');
        } else {
          console.log('Error details:', formError);
        }

        console.log('Trying JSON approach...');

        // Try JSON approach
        const jsonPayload = { username, password };
        console.log('JSON payload:', JSON.stringify(jsonPayload, null, 2));

        try {
          const response = await axios.post(`${baseUrl}${LOGIN_ENDPOINT}`, jsonPayload, {
            headers: {
              'Content-Type': 'application/json'
            }
          });

          console.log('JSON approach response:', response.status);
          console.log('Response headers:', JSON.stringify(response.headers, null, 2));
          console.log('Response data:', JSON.stringify(response.data, null, 2));

          const token = response.data.token || response.data.accessToken || null;

          if (token) {
            console.log('Authentication successful (JSON approach)');
            return token;
          } else {
            console.log('No token received in response (JSON approach)');
          }
        } catch (jsonError) {
          console.log('JSON approach failed:', jsonError.message);
          if (jsonError.response) {
            console.log('Response status:', jsonError.response.status);
            console.log('Response headers:', JSON.stringify(jsonError.response.headers, null, 2));
            console.log('Response data:', JSON.stringify(jsonError.response.data, null, 2));
          } else if (jsonError.request) {
            console.log('No response received');
          } else {
            console.log('Error details:', jsonError);
          }
          // Continue to the next base URL
        }
      }
    } catch (error) {
      console.log(`Failed with base URL ${baseUrl}:`, error.message);
      // Continue to the next base URL
    }
  }

  // If we get here, all base URLs failed
  throw new Error('Authentication failed with all base URLs');
}

/**
 * Gets user information using a JWT token
 *
 * @param {string} token The JWT token
 * @returns {Promise<object>} A Promise that resolves to the user information
 */
async function getUserInfo(token) {
  console.log('Getting user information');

  // Try each base URL until one works
  for (const baseUrl of API_BASE_URLS) {
    console.log(`\nTrying API Base URL: ${baseUrl || '(relative URL)'}`);
    console.log(`Full URL: ${baseUrl}${USER_INFO_ENDPOINT}`);

    try {
      const response = await axios.get(`${baseUrl}${USER_INFO_ENDPOINT}`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'X-Token': token
        }
      });

      console.log('User information response:', response.status);
      console.log('Response headers:', JSON.stringify(response.headers, null, 2));
      console.log('Response data:', JSON.stringify(response.data, null, 2));

      console.log('User information retrieved successfully');
      return response.data;
    } catch (error) {
      console.log(`Failed with base URL ${baseUrl}:`, error.message);
      if (error.response) {
        console.log('Response status:', error.response.status);
        console.log('Response data:', JSON.stringify(error.response.data, null, 2));
      } else if (error.request) {
        console.log('No response received');
      } else {
        console.log('Error details:', error);
      }
      // Continue to the next base URL
    }
  }

  // If we get here, all base URLs failed
  throw new Error('Failed to get user information with all base URLs');
}

/**
 * Decodes a JWT token
 *
 * @param {string} token The JWT token
 * @returns {object} The decoded token
 */
function decodeToken(token) {
  try {
    console.log('Decoding token');

    const parts = token.split('.');

    if (parts.length !== 3) {
      throw new Error('Invalid token format');
    }

    // Decode the header and payload
    const header = JSON.parse(Buffer.from(parts[0], 'base64').toString());
    const payload = JSON.parse(Buffer.from(parts[1], 'base64').toString());

    return {
      header,
      payload,
      signature: parts[2]
    };
  } catch (error) {
    console.error('Error decoding token:', error.message);
    throw error;
  }
}

/**
 * Main function
 */
async function main() {
  try {
    // Get username and password from command line arguments
    const username = process.argv[2];
    const password = process.argv[3];

    if (!username || !password) {
      console.error('Usage: node test-jwt.js <username> <password>');
      process.exit(1);
    }

    console.log('=== JWT Authentication Test ===');
    console.log('Testing with credentials:');
    console.log(`Username: ${username}`);
    console.log(`Password: ${'*'.repeat(password.length)}`);
    console.log('\n=== Step 1: Authentication ===');

    // Authenticate and get token
    let token;
    try {
      token = await authenticate(username, password);
      console.log('\nAuthentication successful!');
      console.log('Token:', token);
    } catch (authError) {
      console.error('\nAuthentication failed!');
      console.error('Error details:', authError.message || authError);
      process.exit(1);
    }

    console.log('\n=== Step 2: Token Decoding ===');

    // Decode token
    let decodedToken;
    try {
      decodedToken = decodeToken(token);
      console.log('Token decoded successfully!');
      console.log('Decoded token:');
      console.log(JSON.stringify(decodedToken, null, 2));
    } catch (decodeError) {
      console.error('Token decoding failed!');
      console.error('Error details:', decodeError.message || decodeError);
      process.exit(1);
    }

    console.log('\n=== Step 3: User Information ===');

    // Get user information
    try {
      const userInfo = await getUserInfo(token);
      console.log('User information retrieved successfully!');
      console.log('User information:');
      console.log(JSON.stringify(userInfo, null, 2));
    } catch (userInfoError) {
      console.error('Failed to get user information!');
      console.error('Error details:', userInfoError.message || userInfoError);
      if (userInfoError.response) {
        console.error('Response status:', userInfoError.response.status);
        console.error('Response data:', JSON.stringify(userInfoError.response.data, null, 2));
      }
      process.exit(1);
    }

    console.log('\n=== Test Summary ===');
    console.log('All tests passed successfully!');
    console.log('JWT authentication is working correctly.');
  } catch (error) {
    console.error('\n=== Test Failed ===');
    console.error('Error details:', error.message || error);
    process.exit(1);
  }
}

// Run the main function
main();
