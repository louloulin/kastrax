#!/usr/bin/env node

/**
 * JWT Configuration Updater
 * 
 * This script updates the JWT configuration in the application.
 */

const fs = require('fs');
const path = require('path');

// Configuration
const JWT_UTILS_PATH = 'src/lib/utils/jwt-utils.ts';
const DEFAULT_SECRET = 'SecretKey012345678901234567890123456789012345678901234567890123456789';
const DEFAULT_ALGORITHM = 'HS256';

/**
 * Updates the JWT configuration in the application
 * 
 * @param {string} secret The secret key to use
 * @param {string} algorithm The signing algorithm to use
 */
function updateJwtConfig(secret, algorithm) {
  try {
    const filePath = path.resolve(JWT_UTILS_PATH);
    
    if (!fs.existsSync(filePath)) {
      throw new Error(`File not found: ${filePath}`);
    }
    
    let content = fs.readFileSync(filePath, 'utf8');
    
    // Update the secret key
    content = content.replace(
      /const SECRET_KEY = ['"].*['"]/,
      `const SECRET_KEY = '${secret}'`
    );
    
    // Update the algorithm
    content = content.replace(
      /algorithm: ['"].*['"]/,
      `algorithm: '${algorithm}'`
    );
    
    // Write the updated content back to the file
    fs.writeFileSync(filePath, content, 'utf8');
    
    console.log(`Updated JWT configuration in ${filePath}`);
    console.log(`Secret: ${secret.substring(0, 10)}...`);
    console.log(`Algorithm: ${algorithm}`);
  } catch (error) {
    console.error(`Error updating JWT configuration: ${error.message}`);
    throw error;
  }
}

/**
 * Main function
 */
function main() {
  try {
    // Get parameters from command line arguments
    const secret = process.argv[2] || DEFAULT_SECRET;
    const algorithm = process.argv[3] || DEFAULT_ALGORITHM;
    
    console.log('=== JWT Configuration Updater ===');
    
    // Update the JWT configuration
    updateJwtConfig(secret, algorithm);
    
    console.log('\n=== Configuration Update Complete ===');
    console.log('The JWT configuration has been updated successfully.');
    console.log('You should rebuild the application for the changes to take effect.');
  } catch (error) {
    console.error('\n=== Configuration Update Failed ===');
    console.error(`Error: ${error.message}`);
    process.exit(1);
  }
}

// Run the main function
main();
