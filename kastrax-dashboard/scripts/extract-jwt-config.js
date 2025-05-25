#!/usr/bin/env node

/**
 * JWT Configuration Extractor
 * 
 * This script extracts JWT configuration from the backend code.
 * It looks for common patterns in Java code that define JWT secret keys and algorithms.
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Configuration
const BACKEND_DIRS = [
  '../datalink-backend',
  '../datalink-server',
  '../datalink-api',
  '../datalink',
  '../backend',
  '../server',
  '../api'
];

// Patterns to look for
const SECRET_KEY_PATTERNS = [
  /secret\s*=\s*["']([^"']+)["']/i,
  /secretKey\s*=\s*["']([^"']+)["']/i,
  /jwtSecret\s*=\s*["']([^"']+)["']/i,
  /jwt\.secret\s*=\s*["']([^"']+)["']/i,
  /setSigningKey\(["']([^"']+)["']\)/i,
  /setSecretKey\(["']([^"']+)["']\)/i,
  /Decoders\.BASE64\.decode\(["']([^"']+)["']\)/i
];

const ALGORITHM_PATTERNS = [
  /algorithm\s*=\s*["']([^"']+)["']/i,
  /setAlgorithm\(["']([^"']+)["']\)/i,
  /SignatureAlgorithm\.([A-Z0-9_]+)/i
];

/**
 * Searches for files in a directory
 * 
 * @param {string} dir The directory to search
 * @param {RegExp} pattern The file pattern to match
 * @returns {string[]} The matching file paths
 */
function findFiles(dir, pattern) {
  try {
    if (!fs.existsSync(dir)) {
      return [];
    }
    
    const command = `find ${dir} -type f -name "${pattern}" | grep -v "node_modules" | grep -v "target" | grep -v "build"`;
    const output = execSync(command, { encoding: 'utf8' });
    
    return output.trim().split('\n').filter(Boolean);
  } catch (error) {
    console.error(`Error finding files in ${dir}:`, error.message);
    return [];
  }
}

/**
 * Searches for patterns in a file
 * 
 * @param {string} filePath The file path
 * @param {RegExp[]} patterns The patterns to search for
 * @returns {string[]} The matching values
 */
function searchPatterns(filePath, patterns) {
  try {
    const content = fs.readFileSync(filePath, 'utf8');
    const matches = [];
    
    for (const pattern of patterns) {
      const match = content.match(pattern);
      if (match && match[1]) {
        matches.push(match[1]);
      }
    }
    
    return matches;
  } catch (error) {
    console.error(`Error searching patterns in ${filePath}:`, error.message);
    return [];
  }
}

/**
 * Main function
 */
function main() {
  console.log('=== JWT Configuration Extractor ===');
  
  const results = {
    secretKeys: new Set(),
    algorithms: new Set(),
    files: []
  };
  
  // Search for Java files
  for (const dir of BACKEND_DIRS) {
    console.log(`\nSearching in ${dir}...`);
    
    const javaFiles = findFiles(dir, "*.java");
    const propertiesFiles = findFiles(dir, "*.properties");
    const ymlFiles = findFiles(dir, "*.yml");
    
    const allFiles = [...javaFiles, ...propertiesFiles, ...ymlFiles];
    
    if (allFiles.length === 0) {
      console.log('  No files found');
      continue;
    }
    
    console.log(`  Found ${allFiles.length} files`);
    
    // Search for patterns in each file
    for (const file of allFiles) {
      const secretKeys = searchPatterns(file, SECRET_KEY_PATTERNS);
      const algorithms = searchPatterns(file, ALGORITHM_PATTERNS);
      
      if (secretKeys.length > 0 || algorithms.length > 0) {
        console.log(`\n  File: ${file}`);
        
        if (secretKeys.length > 0) {
          console.log('    Secret Keys:');
          secretKeys.forEach(key => {
            console.log(`      - ${key}`);
            results.secretKeys.add(key);
          });
        }
        
        if (algorithms.length > 0) {
          console.log('    Algorithms:');
          algorithms.forEach(alg => {
            console.log(`      - ${alg}`);
            results.algorithms.add(alg);
          });
        }
        
        results.files.push({
          file,
          secretKeys,
          algorithms
        });
      }
    }
  }
  
  // Summary
  console.log('\n=== Summary ===');
  console.log(`Found ${results.secretKeys.size} unique secret keys and ${results.algorithms.size} unique algorithms in ${results.files.length} files.`);
  
  if (results.secretKeys.size > 0) {
    console.log('\nSecret Keys:');
    Array.from(results.secretKeys).forEach(key => {
      console.log(`  - ${key}`);
    });
  }
  
  if (results.algorithms.size > 0) {
    console.log('\nAlgorithms:');
    Array.from(results.algorithms).forEach(alg => {
      console.log(`  - ${alg}`);
    });
  }
  
  // Generate test commands
  if (results.secretKeys.size > 0) {
    console.log('\n=== Test Commands ===');
    console.log('Run these commands to test the extracted configurations:');
    
    Array.from(results.secretKeys).forEach(key => {
      console.log(`\nnode scripts/verify-jwt-signature.js admin # Using secret: ${key}`);
    });
  }
}

// Run the main function
main();
