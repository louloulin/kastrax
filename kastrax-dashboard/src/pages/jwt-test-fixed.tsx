import React, { useState, useEffect } from 'react';
import { jwtFixed } from '../lib/utils/jwt-fixed';
import { getApiClient } from '../lib/api/client-factory';
import { apiMapping } from '../lib/api/config/api-mapping';

/**
 * JWT 测试页面（修复版）
 * 
 * 用于测试修复后的 JWT token 生成和验证功能
 */
const JwtTestFixedPage: React.FC = () => {
  const [username, setUsername] = useState('admin');
  const [storedToken, setStoredToken] = useState<string | null>(null);
  const [decodedToken, setDecodedToken] = useState<any>(null);
  const [newToken, setNewToken] = useState<string | null>(null);
  const [apiResponse, setApiResponse] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  
  // 加载存储的 token
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      setStoredToken(token);
      try {
        const decoded = jwtFixed.decodeToken(token);
        setDecodedToken(decoded);
      } catch (error) {
        console.error('Error decoding token:', error);
      }
    }
  }, []);
  
  // 创建新的 token
  const handleCreateToken = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const token = await jwtFixed.createToken(username);
      setNewToken(token);
      
      // 解码 token
      const decoded = jwtFixed.decodeToken(token);
      console.log('Decoded token:', decoded);
    } catch (error) {
      console.error('Error creating token:', error);
      setError('创建 token 失败: ' + (error as Error).message);
    } finally {
      setLoading(false);
    }
  };
  
  // 保存新的 token
  const handleSaveToken = () => {
    if (newToken) {
      localStorage.setItem('token', newToken);
      localStorage.setItem('username', username);
      setStoredToken(newToken);
      setDecodedToken(jwtFixed.decodeToken(newToken));
      setNewToken(null);
      alert('Token 已保存到 localStorage');
    }
  };
  
  // 测试 API 请求
  const handleTestApi = async () => {
    setLoading(true);
    setError(null);
    setApiResponse(null);
    
    try {
      const token = storedToken || newToken;
      if (!token) {
        throw new Error('没有可用的 token');
      }
      
      const apiClient = getApiClient();
      const response = await apiClient.get(apiMapping.auth.info.legacy, {
        headers: {
          Authorization: `Bearer ${token}`,
          'X-Token': token
        }
      });
      
      setApiResponse(response.data);
    } catch (error) {
      console.error('API 请求失败:', error);
      setError('API 请求失败: ' + (error as any).message);
    } finally {
      setLoading(false);
    }
  };
  
  // 清除 token
  const handleClearToken = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    setStoredToken(null);
    setDecodedToken(null);
    setNewToken(null);
    alert('Token 已从 localStorage 中清除');
  };
  
  // 验证 token
  const handleVerifyToken = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const token = storedToken || newToken;
      if (!token) {
        throw new Error('没有可用的 token');
      }
      
      const verified = await jwtFixed.verifyToken(token);
      if (verified) {
        alert('Token 验证成功');
        console.log('Verified token:', verified);
      } else {
        setError('Token 验证失败');
      }
    } catch (error) {
      console.error('Token 验证失败:', error);
      setError('Token 验证失败: ' + (error as Error).message);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: '20px' }}>
      <h1>JWT Token 测试（修复版）</h1>
      
      <div style={{ marginBottom: '20px', padding: '15px', border: '1px solid #ddd', borderRadius: '4px' }}>
        <h2>创建 Token</h2>
        <div style={{ marginBottom: '10px' }}>
          <label style={{ display: 'block', marginBottom: '5px' }}>用户名:</label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
          />
        </div>
        <button
          onClick={handleCreateToken}
          disabled={loading}
          style={{
            padding: '10px 15px',
            backgroundColor: '#4CAF50',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: loading ? 'not-allowed' : 'pointer',
            marginRight: '10px'
          }}
        >
          {loading ? '创建中...' : '创建 Token'}
        </button>
        {newToken && (
          <button
            onClick={handleSaveToken}
            style={{
              padding: '10px 15px',
              backgroundColor: '#2196F3',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            保存 Token
          </button>
        )}
      </div>
      
      {newToken && (
        <div style={{ marginBottom: '20px', padding: '15px', border: '1px solid #ddd', borderRadius: '4px' }}>
          <h2>新创建的 Token</h2>
          <textarea
            value={newToken}
            readOnly
            style={{ width: '100%', height: '60px', marginBottom: '10px', padding: '8px' }}
          />
          <h3>解码后的 Payload</h3>
          <pre style={{ backgroundColor: '#f5f5f5', padding: '10px', borderRadius: '4px', overflow: 'auto' }}>
            {JSON.stringify(jwtFixed.decodeToken(newToken), null, 2)}
          </pre>
          <div style={{ marginTop: '10px' }}>
            <button
              onClick={handleVerifyToken}
              disabled={loading}
              style={{
                padding: '10px 15px',
                backgroundColor: '#9C27B0',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: loading ? 'not-allowed' : 'pointer',
                marginRight: '10px'
              }}
            >
              {loading ? '验证中...' : '验证 Token'}
            </button>
            <button
              onClick={handleTestApi}
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
              {loading ? '测试中...' : '测试 API 请求'}
            </button>
          </div>
        </div>
      )}
      
      {storedToken && (
        <div style={{ marginBottom: '20px', padding: '15px', border: '1px solid #ddd', borderRadius: '4px' }}>
          <h2>存储的 Token</h2>
          <textarea
            value={storedToken}
            readOnly
            style={{ width: '100%', height: '60px', marginBottom: '10px', padding: '8px' }}
          />
          <h3>解码后的 Payload</h3>
          <pre style={{ backgroundColor: '#f5f5f5', padding: '10px', borderRadius: '4px', overflow: 'auto' }}>
            {JSON.stringify(decodedToken, null, 2)}
          </pre>
          <div style={{ marginTop: '10px' }}>
            <button
              onClick={handleVerifyToken}
              disabled={loading}
              style={{
                padding: '10px 15px',
                backgroundColor: '#9C27B0',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: loading ? 'not-allowed' : 'pointer',
                marginRight: '10px'
              }}
            >
              {loading ? '验证中...' : '验证 Token'}
            </button>
            <button
              onClick={handleTestApi}
              disabled={loading}
              style={{
                padding: '10px 15px',
                backgroundColor: '#FF9800',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: loading ? 'not-allowed' : 'pointer',
                marginRight: '10px'
              }}
            >
              {loading ? '测试中...' : '测试 API 请求'}
            </button>
            <button
              onClick={handleClearToken}
              style={{
                padding: '10px 15px',
                backgroundColor: '#F44336',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              清除 Token
            </button>
          </div>
        </div>
      )}
      
      {apiResponse && (
        <div style={{ marginBottom: '20px', padding: '15px', border: '1px solid #ddd', borderRadius: '4px' }}>
          <h2>API 响应</h2>
          <pre style={{ backgroundColor: '#f5f5f5', padding: '10px', borderRadius: '4px', overflow: 'auto' }}>
            {JSON.stringify(apiResponse, null, 2)}
          </pre>
        </div>
      )}
      
      {error && (
        <div
          style={{
            marginBottom: '20px',
            padding: '15px',
            backgroundColor: '#FFEBEE',
            color: '#D32F2F',
            borderRadius: '4px'
          }}
        >
          <strong>错误:</strong> {error}
        </div>
      )}
    </div>
  );
};

export default JwtTestFixedPage;
