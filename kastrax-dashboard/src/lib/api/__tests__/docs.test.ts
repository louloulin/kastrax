/**
 * API Documentation Generator Testing
 * 
 * This test suite verifies the API documentation generator works correctly.
 * It uses a mock filesystem and checks if the documentation files are generated
 * with the correct content.
 */

import fs from 'fs';
import path from 'path';

// Mock fs and path modules
jest.mock('fs', () => ({
  existsSync: jest.fn(),
  readFileSync: jest.fn(),
  writeFileSync: jest.fn(),
  mkdirSync: jest.fn(),
}));

jest.mock('path', () => ({
  resolve: jest.fn(),
  join: jest.fn(),
  basename: jest.fn(),
  extname: jest.fn(),
  relative: jest.fn(),
}));

jest.mock('glob', () => ({
  sync: jest.fn(),
}));

describe('API Documentation Generator', () => {
  // Store original process.cwd
  const originalCwd = process.cwd;
  // Mock process.cwd() to return a fixed path
  process.cwd = jest.fn().mockReturnValue('/test/dataflare-ui');
  
  // Reset mocks before each test
  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock path.resolve for API_DIR and DOCS_DIR
    (path.resolve as jest.Mock).mockImplementation((dir, subDir) => {
      if (subDir === '../src/lib/api') return '/test/dataflare-ui/src/lib/api';
      if (subDir === '../docs/api') return '/test/dataflare-ui/docs/api';
      return path.join(dir, subDir);
    });
    
    // Mock path.join to concatenate paths
    (path.join as jest.Mock).mockImplementation((...parts) => parts.join('/'));
    
    // Mock path.basename to return the file name
    (path.basename as jest.Mock).mockImplementation((filePath, ext) => {
      const parts = filePath.split('/');
      const fileName = parts[parts.length - 1];
      if (ext) return fileName.replace(ext, '');
      return fileName;
    });
    
    // Mock path.extname to return the extension
    (path.extname as jest.Mock).mockImplementation((filePath) => {
      return filePath.includes('.') ? '.' + filePath.split('.').pop() : '';
    });
    
    // Mock fs.existsSync
    (fs.existsSync as jest.Mock).mockReturnValue(false);
  });
  
  // Restore original process.cwd after tests
  afterAll(() => {
    process.cwd = originalCwd;
  });
  
  it('should create docs directory if it does not exist', () => {
    // Mock fs.existsSync to return false (directory doesn't exist)
    (fs.existsSync as jest.Mock).mockReturnValue(false);
    
    // Import the documentation generator
    require('../../../scripts/generate-api-docs');
    
    // Verify that mkdirSync was called with the docs directory
    expect(fs.mkdirSync).toHaveBeenCalledWith('/test/dataflare-ui/docs/api', { recursive: true });
  });
  
  it('should generate documentation for v1 and v2 APIs', () => {
    // Mock glob.sync to return some test files
    const glob = require('glob');
    (glob.sync as jest.Mock).mockImplementation((pattern) => {
      if (pattern.includes('!(v1|v2)')) {
        return [
          '/test/dataflare-ui/src/lib/api/auth.ts',
          '/test/dataflare-ui/src/lib/api/resources.ts'
        ];
      }
      if (pattern.includes('v2')) {
        return [
          '/test/dataflare-ui/src/lib/api/v2/auth.ts',
          '/test/dataflare-ui/src/lib/api/v2/index.ts'
        ];
      }
      return [];
    });
    
    // Mock fs.readFileSync to return a test file content
    (fs.readFileSync as jest.Mock).mockImplementation((filePath) => {
      if (filePath.includes('auth.ts')) {
        return `
          /**
           * Auth API
           * 
           * This module provides authentication functions
           */
          
          export interface User {
            userId: string;
            username: string;
          }
          
          export const login = (params) => {
            // Implementation
          };
        `;
      }
      return '';
    });
    
    // Import the documentation generator
    require('../../../scripts/generate-api-docs');
    
    // Verify that the documentation files were created
    expect(fs.writeFileSync).toHaveBeenCalledTimes(6); // 2 module docs + 2 index files + main index + v2 index
    
    // Check v1 index was created
    expect(fs.writeFileSync).toHaveBeenCalledWith(
      '/test/dataflare-ui/docs/api/v1.md',
      expect.stringContaining('API v1 Documentation')
    );
    
    // Check v2 index was created
    expect(fs.writeFileSync).toHaveBeenCalledWith(
      '/test/dataflare-ui/docs/api/v2.md',
      expect.stringContaining('API v2 Documentation')
    );
    
    // Check auth module documentation was created
    expect(fs.writeFileSync).toHaveBeenCalledWith(
      '/test/dataflare-ui/docs/api/v1/auth.md',
      expect.stringContaining('Auth API')
    );
  });
  
  it('should extract interfaces and functions from file content', () => {
    // Mock fs functions
    (fs.existsSync as jest.Mock).mockReturnValue(true);
    (fs.readFileSync as jest.Mock).mockReturnValue(`
      /**
       * Test API Module
       */
      
      /**
       * User interface
       */
      export interface User {
        id: string;
        name: string;
      }
      
      /**
       * Login function
       */
      export const login = (username: string, password: string) => {
        // Implementation
      };
      
      export const API_ENDPOINT = '/api/v2/auth';
    `);
    
    // Mock path functions
    (path.relative as jest.Mock).mockReturnValue('test.ts');
    (path.basename as jest.Mock).mockImplementation((filePath, ext) => {
      if (ext) return 'test';
      return 'test.ts';
    });
    
    // Import the generateFileDoc function
    const generateApiDocs = require('../../../scripts/generate-api-docs');
    
    // Call generateFileDoc manually with a test file
    const markdown = generateApiDocs.__getMarkdownForTesting('/test/api/test.ts');
    
    // Verify the generated markdown contains all the expected sections
    expect(markdown).toContain('# test API');
    expect(markdown).toContain('Test API Module');
    expect(markdown).toContain('## Types');
    expect(markdown).toContain('### User');
    expect(markdown).toContain('User interface');
    expect(markdown).toContain('## Functions');
    expect(markdown).toContain('### login');
    expect(markdown).toContain('Login function');
    expect(markdown).toContain('## API Endpoints');
    expect(markdown).toContain('/api/v2/auth');
  });
}); 