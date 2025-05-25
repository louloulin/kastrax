import React, { useState, useEffect } from 'react';
import { jwtFixed } from '../lib/utils/jwt-fixed';

/**
 * JWT验证工具页面
 * 
 * 用于验证和分析JWT令牌
 */
const JwtValidatorPage: React.FC = () => {
  const [token, setToken] = useState<string>('');
  const [validationResult, setValidationResult] = useState<any>(null);
  const [backendSecretKey, setBackendSecretKey] = useState<string>('SecretKey012345678901234567890123456789012345678901234567890123456789');
  const [secretKeyBytes, setSecretKeyBytes] = useState<string>('');

  // 初始化时尝试获取密钥字节
  useEffect(() => {
    try {
      // @ts-ignore - 访问内部函数
      const keyBytes = jwtFixed._getSecretKeyBytes ? jwtFixed._getSecretKeyBytes() : null;
      if (keyBytes) {
        let bytesStr = '';
        for (let i = 0; i < keyBytes.length; i++) {
          bytesStr += keyBytes[i].toString(16).padStart(2, '0') + ' ';
        }
        setSecretKeyBytes(bytesStr);
      }
    } catch (error) {
      console.error('Error getting secret key bytes:', error);
    }
  }, []);

  // 解析JWT令牌
  const parseJwt = (token: string) => {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        return {
          valid: false,
          error: '令牌格式不正确，应该有3个部分',
          parts: parts
        };
      }

      const [headerB64, payloadB64, signatureB64] = parts;

      // 检查Base64URL格式
      const hasBase64Padding = headerB64.includes('=') || payloadB64.includes('=') || signatureB64.includes('=');
      const hasInvalidChars = /[+/]/g.test(headerB64 + payloadB64 + signatureB64);

      // 解码header和payload
      let header, payload;
      try {
        header = JSON.parse(base64UrlDecode(headerB64));
        payload = JSON.parse(base64UrlDecode(payloadB64));
      } catch (e) {
        return {
          valid: false,
          error: '无法解码header或payload: ' + (e as Error).message,
          parts: {
            header: headerB64,
            payload: payloadB64,
            signature: signatureB64
          }
        };
      }

      return {
        valid: true,
        hasBase64Padding,
        hasInvalidChars,
        parts: {
          header: headerB64,
          payload: payloadB64,
          signature: signatureB64
        },
        decoded: {
          header,
          payload
        }
      };
    } catch (error) {
      return {
        valid: false,
        error: (error as Error).message
      };
    }
  };

  // Base64URL解码
  const base64UrlDecode = (input: string) => {
    // 转换回标准Base64格式
    let base64 = input.replace(/-/g, '+').replace(/_/g, '/');
    
    // 添加填充
    while (base64.length % 4) {
      base64 += '=';
    }
    
    // 解码
    return atob(base64);
  };

  // 验证JWT令牌
  const validateJwt = () => {
    if (!token) {
      setValidationResult({
        valid: false,
        error: '请输入JWT令牌'
      });
      return;
    }

    const result = parseJwt(token);
    
    if (!result.valid) {
      setValidationResult(result);
      return;
    }
    
    // 检查令牌是否过期
    const now = Math.floor(Date.now() / 1000);
    const isExpired = result.decoded.payload.exp && result.decoded.payload.exp < now;
    
    // 检查必要的字段
    const hasRequiredFields = 
      result.decoded.header.alg && 
      result.decoded.header.typ && 
      result.decoded.payload.sub;
    
    setValidationResult({
      ...result,
      isExpired,
      hasRequiredFields,
      timeRemaining: result.decoded.payload.exp ? result.decoded.payload.exp - now : null
    });
  };

  // 从localStorage获取token
  const getTokenFromStorage = () => {
    const storedToken = localStorage.getItem('token');
    if (storedToken) {
      setToken(storedToken);
    } else {
      alert('localStorage中没有存储token');
    }
  };

  // 创建新token
  const createNewToken = async () => {
    try {
      const username = localStorage.getItem('username') || 'admin';
      const newToken = await jwtFixed.createToken(username);
      setToken(newToken);
      validateJwt();
    } catch (error) {
      console.error('创建token失败:', error);
      alert('创建token失败: ' + (error as Error).message);
    }
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: '20px' }}>
      <h1>JWT令牌验证工具</h1>
      
      <div style={{ marginBottom: '20px' }}>
        <h2>密钥信息</h2>
        <div>
          <p><strong>后端密钥字符串:</strong> {backendSecretKey}</p>
          <p><strong>前端解码后的密钥字节:</strong> {secretKeyBytes || '无法获取'}</p>
        </div>
      </div>
      
      <div style={{ marginBottom: '20px' }}>
        <h2>JWT令牌</h2>
        <div style={{ marginBottom: '10px' }}>
          <textarea
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="输入JWT令牌进行验证"
            style={{ width: '100%', height: '80px' }}
          />
        </div>
        <div>
          <button 
            onClick={validateJwt}
            style={{ marginRight: '10px' }}
          >
            验证令牌
          </button>
          <button 
            onClick={getTokenFromStorage}
            style={{ marginRight: '10px' }}
          >
            从localStorage获取
          </button>
          <button 
            onClick={createNewToken}
          >
            创建新令牌
          </button>
        </div>
      </div>
      
      {validationResult && (
        <div style={{ marginBottom: '20px' }}>
          <h2>验证结果</h2>
          <div style={{ 
            background: validationResult.valid ? '#e6ffe6' : '#ffe6e6', 
            padding: '10px', 
            borderRadius: '5px',
            marginBottom: '10px'
          }}>
            <p><strong>格式有效:</strong> {validationResult.valid ? '✅ 是' : '❌ 否'}</p>
            {validationResult.valid && (
              <>
                <p><strong>包含Base64填充:</strong> {validationResult.hasBase64Padding ? '❌ 是 (不符合JWT规范)' : '✅ 否 (符合JWT规范)'}</p>
                <p><strong>包含无效字符:</strong> {validationResult.hasInvalidChars ? '❌ 是 (不符合JWT规范)' : '✅ 否 (符合JWT规范)'}</p>
                <p><strong>已过期:</strong> {validationResult.isExpired ? '❌ 是' : '✅ 否'}</p>
                <p><strong>包含必要字段:</strong> {validationResult.hasRequiredFields ? '✅ 是' : '❌ 否'}</p>
                {validationResult.timeRemaining !== null && (
                  <p><strong>剩余有效时间:</strong> {validationResult.timeRemaining > 0 ? `${validationResult.timeRemaining}秒` : '已过期'}</p>
                )}
              </>
            )}
            {!validationResult.valid && (
              <p><strong>错误:</strong> {validationResult.error}</p>
            )}
          </div>
          
          {validationResult.valid && (
            <>
              <h3>令牌各部分</h3>
              <div style={{ marginBottom: '10px' }}>
                <h4>Header (Base64URL):</h4>
                <textarea
                  readOnly
                  value={validationResult.parts.header}
                  style={{ width: '100%', height: '40px' }}
                />
                <h4>解码后:</h4>
                <pre style={{ background: '#f5f5f5', padding: '10px', borderRadius: '5px' }}>
                  {JSON.stringify(validationResult.decoded.header, null, 2)}
                </pre>
              </div>
              
              <div style={{ marginBottom: '10px' }}>
                <h4>Payload (Base64URL):</h4>
                <textarea
                  readOnly
                  value={validationResult.parts.payload}
                  style={{ width: '100%', height: '40px' }}
                />
                <h4>解码后:</h4>
                <pre style={{ background: '#f5f5f5', padding: '10px', borderRadius: '5px' }}>
                  {JSON.stringify(validationResult.decoded.payload, null, 2)}
                </pre>
              </div>
              
              <div>
                <h4>Signature (Base64URL):</h4>
                <textarea
                  readOnly
                  value={validationResult.parts.signature}
                  style={{ width: '100%', height: '40px' }}
                />
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default JwtValidatorPage;
