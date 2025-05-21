import React, { useState, useEffect } from 'react';
import { jwtFixed } from '../lib/utils/jwt-fixed';
import { getApiClient } from '../lib/api/client-factory';
import { apiMapping } from '../lib/api/config/api-mapping';

/**
 * JWT 调试页面
 * 
 * 用于测试和调试 JWT token 生成和验证功能
 */
const JwtDebugPage: React.FC = () => {
  const [username, setUsername] = useState('admin');
  const [storedToken, setStoredToken] = useState<string | null>(null);
  const [decodedToken, setDecodedToken] = useState<any>(null);
  const [newToken, setNewToken] = useState<string | null>(null);
  const [apiResponse, setApiResponse] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [secretKeyBytes, setSecretKeyBytes] = useState<string>('');
  
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
    
    // 显示密钥字节
    try {
      // @ts-ignore - 访问内部函数
      const keyBytes = jwtFixed._getSecretKeyBytes ? jwtFixed._getSecretKeyBytes() : null;
      if (keyBytes) {
        let bytesStr = '';
        for (let i = 0; i < Math.min(keyBytes.length, 32); i++) {
          bytesStr += keyBytes[i].toString(16).padStart(2, '0') + ' ';
        }
        setSecretKeyBytes(bytesStr + '...');
      }
    } catch (error) {
      console.error('Error getting secret key bytes:', error);
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
      setDecodedToken(decoded);
    } catch (error) {
      console.error('Error creating token:', error);
      setError('创建 token 失败: ' + (error as Error).message);
    } finally {
      setLoading(false);
    }
  };
  
  // 保存 token 到 localStorage
  const handleSaveToken = () => {
    if (newToken) {
      localStorage.setItem('token', newToken);
      localStorage.setItem('username', username);
      setStoredToken(newToken);
      alert('Token 已保存到 localStorage');
    } else {
      setError('没有可用的 token 可保存');
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
      <h1>JWT Token 调试工具</h1>
      
      <div style={{ marginBottom: '20px' }}>
        <h2>密钥信息</h2>
        <p>密钥字节 (前32字节): {secretKeyBytes || '无法获取'}</p>
      </div>
      
      <div style={{ marginBottom: '20px' }}>
        <h2>创建 Token</h2>
        <div style={{ marginBottom: '10px' }}>
          <label>
            用户名:
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              style={{ marginLeft: '10px' }}
            />
          </label>
        </div>
        <button
          onClick={handleCreateToken}
          disabled={loading || !username}
          style={{ marginRight: '10px' }}
        >
          创建 Token
        </button>
        <button
          onClick={handleSaveToken}
          disabled={loading || !newToken}
        >
          保存 Token
        </button>
      </div>
      
      {newToken && (
        <div style={{ marginBottom: '20px' }}>
          <h3>新创建的 Token:</h3>
          <textarea
            readOnly
            value={newToken}
            style={{ width: '100%', height: '60px' }}
          />
        </div>
      )}
      
      <div style={{ marginBottom: '20px' }}>
        <h2>存储的 Token</h2>
        {storedToken ? (
          <>
            <textarea
              readOnly
              value={storedToken}
              style={{ width: '100%', height: '60px' }}
            />
            <div style={{ marginTop: '10px' }}>
              <button
                onClick={handleVerifyToken}
                disabled={loading}
                style={{ marginRight: '10px' }}
              >
                验证 Token
              </button>
              <button
                onClick={handleTestApi}
                disabled={loading}
                style={{ marginRight: '10px' }}
              >
                测试 API 请求
              </button>
              <button
                onClick={handleClearToken}
                disabled={loading}
              >
                清除 Token
              </button>
            </div>
          </>
        ) : (
          <p>没有存储的 Token</p>
        )}
      </div>
      
      {decodedToken && (
        <div style={{ marginBottom: '20px' }}>
          <h2>解码的 Token 内容</h2>
          <pre style={{ background: '#f5f5f5', padding: '10px', borderRadius: '5px' }}>
            {JSON.stringify(decodedToken, null, 2)}
          </pre>
        </div>
      )}
      
      {apiResponse && (
        <div style={{ marginBottom: '20px' }}>
          <h2>API 响应</h2>
          <pre style={{ background: '#f5f5f5', padding: '10px', borderRadius: '5px' }}>
            {JSON.stringify(apiResponse, null, 2)}
          </pre>
        </div>
      )}
      
      {error && (
        <div style={{ color: 'red', marginTop: '20px' }}>
          <h2>错误</h2>
          <p>{error}</p>
        </div>
      )}
    </div>
  );
};

export default JwtDebugPage;
