import React, { useState, useEffect } from 'react';
import { authenticateUser, getUserInfo } from '../lib/api/auth-proxy';
import { getApiClient } from '../lib/api/client-factory';
import { apiMapping } from '../lib/api/config/api-mapping';
import { testJwtAuthentication, decodeJwtToken, runAuthenticationTests } from '../lib/utils/jwt-test-utils';

/**
 * JWT Authentication Test Page
 *
 * This page provides a simple interface to test JWT authentication with the backend.
 */
const JwtTestPage: React.FC = () => {
  // State for form inputs
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  // State for test results
  const [token, setToken] = useState<string | null>(null);
  const [userInfo, setUserInfo] = useState<any>(null);
  const [testResult, setTestResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Load token from localStorage on mount
  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    if (storedToken) {
      setToken(storedToken);
    }
  }, []);

  // Handle login
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const result = await authenticateUser(username, password);

      if (result.success) {
        setToken(result.token);
        setTestResult({ type: 'login', data: result });
      } else {
        setError(result.message || 'Authentication failed');
      }
    } catch (err) {
      setError(err.message || 'An error occurred during authentication');
    } finally {
      setLoading(false);
    }
  };

  // Handle getting user info
  const handleGetUserInfo = async () => {
    setLoading(true);
    setError(null);

    try {
      const result = await getUserInfo();

      if (result.success) {
        setUserInfo(result);
        setTestResult({ type: 'userInfo', data: result });
      } else {
        setError(result.message || 'Failed to get user info');
      }
    } catch (err) {
      setError(err.message || 'An error occurred while getting user info');
    } finally {
      setLoading(false);
    }
  };

  // Handle making a direct API request
  const handleDirectRequest = async () => {
    setLoading(true);
    setError(null);

    try {
      const apiClient = getApiClient();
      const storedToken = localStorage.getItem('token');

      if (!storedToken) {
        throw new Error('No token available');
      }

      // Make a request to a protected endpoint
      const response = await apiClient.get(apiMapping.auth.info.legacy, {
        headers: {
          Authorization: `Bearer ${storedToken}`,
          'X-Token': storedToken
        }
      });

      setTestResult({ type: 'directRequest', data: response.data });
    } catch (err) {
      setError(err.message || 'An error occurred during the direct request');
    } finally {
      setLoading(false);
    }
  };

  // Handle token inspection
  const handleInspectToken = () => {
    const storedToken = localStorage.getItem('token');

    if (!storedToken) {
      setError('No token available');
      return;
    }

    try {
      // Use the decodeJwtToken utility
      const decodedToken = decodeJwtToken(storedToken);

      if (!decodedToken) {
        setError('Failed to decode token');
        return;
      }

      setTestResult({
        type: 'tokenInspection',
        data: {
          token: storedToken,
          ...decodedToken
        }
      });
    } catch (err) {
      setError(err.message || 'An error occurred while inspecting the token');
    }
  };

  // Handle comprehensive authentication test
  const handleRunTests = async () => {
    setLoading(true);
    setError(null);

    try {
      const results = await runAuthenticationTests();
      setTestResult({
        type: 'comprehensiveTest',
        data: results
      });
    } catch (err) {
      setError(err.message || 'An error occurred while running tests');
    } finally {
      setLoading(false);
    }
  };

  // Handle logout
  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    setToken(null);
    setUserInfo(null);
    setTestResult(null);
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: '20px' }}>
      <h1>JWT Authentication Test</h1>

      <div style={{ marginBottom: '20px' }}>
        <h2>Current Status</h2>
        <p>Token: {token ? '✅ Available' : '❌ Not available'}</p>
        <p>User Info: {userInfo ? '✅ Available' : '❌ Not available'}</p>
      </div>

      {!token ? (
        <div style={{ marginBottom: '20px' }}>
          <h2>Login</h2>
          <form onSubmit={handleLogin}>
            <div style={{ marginBottom: '10px' }}>
              <label style={{ display: 'block', marginBottom: '5px' }}>Username:</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                style={{ padding: '8px', width: '100%' }}
                required
              />
            </div>
            <div style={{ marginBottom: '10px' }}>
              <label style={{ display: 'block', marginBottom: '5px' }}>Password:</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                style={{ padding: '8px', width: '100%' }}
                required
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              style={{
                padding: '10px 15px',
                backgroundColor: '#4CAF50',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: loading ? 'not-allowed' : 'pointer'
              }}
            >
              {loading ? 'Logging in...' : 'Login'}
            </button>
          </form>
        </div>
      ) : (
        <div style={{ marginBottom: '20px' }}>
          <h2>Test Actions</h2>
          <div style={{ display: 'flex', gap: '10px', marginBottom: '10px' }}>
            <button
              onClick={handleGetUserInfo}
              disabled={loading}
              style={{
                padding: '10px 15px',
                backgroundColor: '#2196F3',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: loading ? 'not-allowed' : 'pointer'
              }}
            >
              {loading ? 'Loading...' : 'Get User Info'}
            </button>
            <button
              onClick={handleDirectRequest}
              disabled={loading}
              style={{
                padding: '10px 15px',
                backgroundColor: '#FF9800',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: loading ? 'not-allowed' : 'pointer'
              }}
            >
              {loading ? 'Loading...' : 'Direct API Request'}
            </button>
            <button
              onClick={handleInspectToken}
              style={{
                padding: '10px 15px',
                backgroundColor: '#9C27B0',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Inspect Token
            </button>
            <button
              onClick={handleRunTests}
              disabled={loading}
              style={{
                padding: '10px 15px',
                backgroundColor: '#673AB7',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: loading ? 'not-allowed' : 'pointer'
              }}
            >
              {loading ? 'Running Tests...' : 'Run All Tests'}
            </button>
            <button
              onClick={handleLogout}
              style={{
                padding: '10px 15px',
                backgroundColor: '#F44336',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Logout
            </button>
          </div>
        </div>
      )}

      {error && (
        <div
          style={{
            padding: '15px',
            backgroundColor: '#FFEBEE',
            color: '#D32F2F',
            borderRadius: '4px',
            marginBottom: '20px'
          }}
        >
          <strong>Error:</strong> {error}
        </div>
      )}

      {testResult && (
        <div style={{ marginBottom: '20px' }}>
          <h2>Test Result: {testResult.type}</h2>
          <pre
            style={{
              backgroundColor: '#F5F5F5',
              padding: '15px',
              borderRadius: '4px',
              overflow: 'auto',
              maxHeight: '400px'
            }}
          >
            {JSON.stringify(testResult.data, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
};

export default JwtTestPage;
