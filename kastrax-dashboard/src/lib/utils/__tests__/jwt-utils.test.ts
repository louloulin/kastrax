/**
 * JWT Utilities Tests
 */
import { createJwtToken, isValidJwtFormat, decodeJwt, isTokenExpired } from '../jwt-utils';

describe('JWT Utilities', () => {
  it('should create a valid JWT token', () => {
    const username = 'testuser';
    const token = createJwtToken(username);
    
    // Check token format
    expect(token).toBeDefined();
    expect(typeof token).toBe('string');
    
    // Check token structure
    const parts = token.split('.');
    expect(parts.length).toBe(3);
    
    // Check if token is valid according to our validator
    expect(isValidJwtFormat(token)).toBe(true);
  });
  
  it('should validate JWT format correctly', () => {
    // Valid token
    const validToken = createJwtToken('testuser');
    expect(isValidJwtFormat(validToken)).toBe(true);
    
    // Invalid tokens
    expect(isValidJwtFormat('')).toBe(false);
    expect(isValidJwtFormat('invalid')).toBe(false);
    expect(isValidJwtFormat('header.payload')).toBe(false);
    expect(isValidJwtFormat('header.payload.signature.extra')).toBe(false);
  });
  
  it('should decode JWT token correctly', () => {
    const username = 'testuser';
    const token = createJwtToken(username);
    
    const decoded = decodeJwt(token);
    expect(decoded).toBeDefined();
    expect(decoded.sub).toBe(username);
    expect(decoded.name).toBe(username);
    expect(typeof decoded.iat).toBe('number');
    expect(typeof decoded.exp).toBe('number');
  });
  
  it('should check token expiration correctly', () => {
    // Create a token that expires in 5 hours (not expired)
    const token = createJwtToken('testuser');
    expect(isTokenExpired(token)).toBe(false);
    
    // Invalid token should be considered expired
    expect(isTokenExpired('')).toBe(true);
    expect(isTokenExpired('invalid.token.format')).toBe(true);
  });
});
