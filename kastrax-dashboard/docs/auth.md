# Authentication System Documentation

## Overview

This document describes the authentication system used in the Dataflare UI application. The authentication system is designed to be compatible with the Datalink-UI backend while providing a modern and secure authentication experience.

## Authentication Flow

1. ✅ **Login**: User submits credentials (username/password)
2. ✅ **Token Generation**: Server validates credentials and returns a JWT token
3. ✅ **Token Storage**: Token is stored in localStorage
4. ✅ **API Requests**: Token is included in the Authorization header for all API requests
5. ✅ **Token Validation**: Server validates the token for each protected API request
6. ✅ **Token Refresh**: Token is automatically refreshed before expiration
7. ✅ **Session Timeout**: User is automatically logged out after a period of inactivity
8. ✅ **Logout**: Token is removed from localStorage

## API Endpoints

### Login

- **Endpoint**: `/api/auth/user/login`
- **Method**: POST
- **Request Body**:
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "success": true,
    "msg": "Login successful",
    "data": {
      "token": "jwt-token-string",
      "accessToken": "jwt-token-string"
    }
  }
  ```

### Get User Info

- **Endpoint**: `/api/auth/user/info`
- **Method**: GET
- **Headers**:
  ```
  Authorization: Bearer jwt-token-string
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "success": true,
    "msg": "success",
    "data": {
      "userId": "string",
      "username": "string",
      "email": "string",
      "role": "string",
      "status": 1,
      "createTime": "string",
      "permissions": ["string"]
    }
  }
  ```

### Logout

- **Endpoint**: `/auth/logout`
- **Method**: POST
- **Headers**:
  ```
  Authorization: Bearer jwt-token-string
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "success": true,
    "msg": "Logout successful",
    "data": null
  }
  ```

## JWT Token Format

The JWT token follows the standard format with three parts separated by periods:
- **Header**: Contains the algorithm and token type
- **Payload**: Contains the claims (user information, expiration, etc.)
- **Signature**: Verifies the token's integrity

Example:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsIm5hbWUiOiJhZG1pbiIsImlhdCI6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

## Implementation Details

### Auth Store

The authentication state is managed using Zustand, a lightweight state management library. The auth store provides:

- **State**:
  - ✅ `token`: The JWT token
  - ✅ `user`: The user information
  - ✅ `loading`: Loading state for async operations
  - ✅ `error`: Error messages
  - ✅ `lastActivity`: Timestamp of the user's last activity

- **Actions**:
  - ✅ `login`: Authenticates the user and stores the token
  - ✅ `logout`: Removes the token and user information
  - ✅ `fetchUserInfo`: Retrieves the user information
  - ✅ `refreshToken`: Refreshes the JWT token before it expires
  - ✅ `updateActivity`: Updates the last activity timestamp
  - ✅ `checkSessionTimeout`: Checks if the session has timed out due to inactivity
  - ✅ `clearError`: Clears error messages

### Auth Adapter

The auth adapter provides compatibility with the Datalink-UI backend by:

1. ✅ Transforming request parameters to match the expected format
2. ✅ Handling different response formats
3. ✅ Ensuring proper JWT token formatting
4. ✅ Providing fallback mechanisms for error handling

### Token Handling

To ensure compatibility with the backend JWT validation:

1. ✅ The token must have exactly 2 period characters (3 parts)
2. ✅ If a token doesn't match this format, a properly formatted token is created
3. ✅ The token is stored in localStorage for persistence
4. ✅ The token is included in the Authorization header as `Bearer {token}`
5. ✅ The token is automatically refreshed before it expires
6. ✅ Token expiration is checked to ensure it's still valid

### Error Handling

The authentication system includes robust error handling:

1. ✅ Network errors are caught and displayed to the user
2. ✅ Invalid credentials trigger appropriate error messages
3. ✅ Token validation failures are handled gracefully
4. ✅ Fallback mechanisms provide a seamless user experience even when the backend is unavailable
5. ✅ Session timeout is detected and handled automatically

## Security Considerations

1. **Token Storage**: Tokens are stored in localStorage, which is vulnerable to XSS attacks. Consider using more secure storage methods in production.
2. **Token Expiration**: Implement token expiration and refresh mechanisms to limit the impact of token theft.
3. **HTTPS**: Always use HTTPS in production to prevent token interception.
4. **CSRF Protection**: Implement CSRF protection for sensitive operations.

## Development and Testing

For development and testing, the application includes:

1. ✅ **Mock Service Worker (MSW)**: Intercepts API requests and returns mock responses
2. ✅ **Mock Handlers**: Simulates backend behavior for authentication endpoints
3. ✅ **Fallback Mechanisms**: Provides simulated user data when the backend is unavailable
4. ✅ **JWT Validation**: Validates JWT token format in mock handlers

## Troubleshooting

Common issues and solutions:

1. **JWT Validation Errors**: Ensure the token has the correct format (header.payload.signature)
2. **401 Unauthorized**: Check that the token is being included in the Authorization header
3. **500 Server Errors**: The backend may be unavailable; check server logs
4. **Token Not Persisting**: Ensure localStorage is available and not being cleared

## Future Improvements

1. ✅ Implement token refresh mechanism
2. Add multi-factor authentication
3. Improve security by using HttpOnly cookies instead of localStorage
4. ✅ Add role-based access control
5. ✅ Implement session timeout and inactivity detection
