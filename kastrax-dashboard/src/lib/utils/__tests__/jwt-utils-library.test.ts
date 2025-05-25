/**
 * JWT Utilities Tests with jsonwebtoken library
 */
import { createJwtToken, isValidJwtFormat, decodeJwt, isTokenExpired, verifyJwt } from '../jwt-utils';
import jwt from 'jsonwebtoken';

// Mock jsonwebtoken
jest.mock('jsonwebtoken');
const mockedJwt = jwt as jest.Mocked<typeof jwt>;

describe('JWT Utilities with jsonwebtoken library', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });
  
  it('should create a valid JWT token using the library', () => {
    // Setup
    const username = 'testuser';
    const mockToken = 'header.payload.signature';
    mockedJwt.sign.mockReturnValue(mockToken);
    
    // Call the function
    const token = createJwtToken(username);
    
    // Verify
    expect(token).toBe(mockToken);
    expect(mockedJwt.sign).toHaveBeenCalledWith(
      expect.objectContaining({
        sub: username,
        name: username,
        iat: expect.any(Number),
        exp: expect.any(Number)
      }),
      expect.any(String),
      expect.objectContaining({ algorithm: 'HS256' })
    );
  });
  
  it('should verify a JWT token using the library', () => {
    // Setup
    const token = 'header.payload.signature';
    const decodedToken = { sub: 'testuser', exp: Date.now() / 1000 + 3600 };
    mockedJwt.verify.mockReturnValue(decodedToken);
    
    // Call the function
    const result = verifyJwt(token);
    
    // Verify
    expect(result).toBe(decodedToken);
    expect(mockedJwt.verify).toHaveBeenCalledWith(
      token,
      expect.any(String),
      expect.objectContaining({ algorithms: ['HS256'] })
    );
  });
  
  it('should handle verification errors', () => {
    // Setup
    const token = 'header.payload.signature';
    mockedJwt.verify.mockImplementation(() => {
      throw new Error('Invalid signature');
    });
    
    // Call the function
    const result = verifyJwt(token);
    
    // Verify
    expect(result).toBeNull();
  });
  
  it('should decode a JWT token using the library', () => {
    // Setup
    const token = 'header.payload.signature';
    const decodedToken = { sub: 'testuser', exp: Date.now() / 1000 + 3600 };
    mockedJwt.decode.mockReturnValue(decodedToken);
    
    // Call the function
    const result = decodeJwt(token);
    
    // Verify
    expect(result).toBe(decodedToken);
  });
});
