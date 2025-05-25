/**
 * Test the API versioning
 */
import { v1, v2, api } from '../index';

describe('API Versioning', () => {
  it('should export v1 and v2 namespaces', () => {
    // Check that v1 and v2 are defined
    expect(v1).toBeDefined();
    expect(v2).toBeDefined();
    
    // Check that api is defined as an alias for v2
    expect(api).toBeDefined();
    expect(api).toBe(v2);
  });
  
  it('should have auth functions in both v1 and v2', () => {
    // Check v1 auth functions
    expect(typeof v1.login).toBe('function');
    expect(typeof v1.logout).toBe('function');
    expect(typeof v1.getInfo).toBe('function');
    expect(typeof v1.refreshToken).toBe('function');
    
    // Check v2 auth functions
    expect(typeof v2.login).toBe('function');
    expect(typeof v2.logout).toBe('function');
    expect(typeof v2.getInfo).toBe('function');
    expect(typeof v2.refreshToken).toBe('function');
  });
  
  it('should have rules functions in both v1 and v2', () => {
    // Check v1 rules functions
    expect(typeof v1.getRuleList).toBe('function');
    expect(typeof v1.getRuleById).toBe('function');
    expect(typeof v1.createRule).toBe('function');
    expect(typeof v1.updateRule).toBe('function');
    expect(typeof v1.deleteRule).toBe('function');
    
    // Check v2 rules functions
    expect(typeof v2.getRuleList).toBe('function');
    expect(typeof v2.getRuleById).toBe('function');
    expect(typeof v2.createRule).toBe('function');
    expect(typeof v2.updateRule).toBe('function');
    expect(typeof v2.deleteRule).toBe('function');
  });
  
  it('should have resources functions in both v1 and v2', () => {
    // Check v1 resources functions
    expect(typeof v1.getResourceList).toBe('function');
    expect(typeof v1.getResourceById).toBe('function');
    expect(typeof v1.createResource).toBe('function');
    expect(typeof v1.updateResource).toBe('function');
    expect(typeof v1.deleteResource).toBe('function');
    expect(typeof v1.testResourceConnection).toBe('function');
    
    // Check v2 resources functions
    expect(typeof v2.getResourceList).toBe('function');
    expect(typeof v2.getResourceById).toBe('function');
    expect(typeof v2.createResource).toBe('function');
    expect(typeof v2.updateResource).toBe('function');
    expect(typeof v2.deleteResource).toBe('function');
    expect(typeof v2.testResourceConnection).toBe('function');
  });
}); 