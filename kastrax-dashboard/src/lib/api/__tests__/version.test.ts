/**
 * API Version Testing
 * 
 * This test suite verifies the API versioning functionality works as expected.
 */

import { api, v1, v2, client } from '../index';
import * as v1API from '../v1';
import * as v2API from '../v2';

describe('API Versioning', () => {
  it('should export v1 namespace correctly', () => {
    // Check that v1 is exported and contains all the modules
    expect(v1).toBeDefined();
    
    // Check that specific properties from v1 API exist
    expect(typeof v1.login).toBe('function');
    expect(typeof v1.getResourceList).toBe('function');
    expect(typeof v1.getRuleList).toBe('function');
  });
  
  it('should export v2 namespace correctly', () => {
    // Check that v2 is exported and contains all the modules
    expect(v2).toBeDefined();
    
    // Check that v2 contains the auth module functions
    expect(typeof v2.login).toBe('function');
    expect(typeof v2.getInfo).toBe('function');
    expect(typeof v2.logout).toBe('function');
    
    // Check that v2 re-exports v1 functions that haven't been migrated
    expect(typeof v2.getRuleList).toBe('function');
    expect(typeof v2.getResourceList).toBe('function');
  });
  
  it('should export api namespace as alias for v2', () => {
    // Verify that api is identical to v2
    expect(api).toBe(v2);
    
    // Verify that api has the same properties as v2
    expect(Object.keys(api)).toEqual(Object.keys(v2));
    
    // Check specific functions are available
    expect(typeof api.login).toBe('function');
    expect(typeof api.logout).toBe('function');
    expect(typeof api.getRuleList).toBe('function');
  });
  
  it('should maintain backward compatibility through direct exports', () => {
    // Import direct exports to test backward compatibility
    const { login, getRuleList, getResourceList } = require('../index');
    
    // These should be the v1 versions
    expect(login).toBeDefined();
    expect(getRuleList).toBeDefined();
    expect(getResourceList).toBeDefined();
  });
  
  it('should use improved API patterns in v2 modules', () => {
    // Mock the request functions
    jest.mock('../utils', () => ({
      getRequest: jest.fn(),
      postRequest: jest.fn(),
      putRequest: jest.fn(),
      deleteRequest: jest.fn(),
    }));
    
    // Import utils to check if v2 auth module uses them
    const { postRequest } = require('../utils');
    
    // Call v2 login function
    const loginParams = { username: 'test', password: 'test' };
    v2.login(loginParams);
    
    // Verify it calls postRequest with the correct arguments
    expect(postRequest).toHaveBeenCalledWith('/api/v2/auth/login', loginParams);
  });
}); 