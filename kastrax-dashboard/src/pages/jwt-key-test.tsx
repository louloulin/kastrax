import React, { useState } from 'react';

/**
 * JWT 密钥测试页面
 * 
 * 用于测试不同的密钥处理方式
 */
const JwtKeyTestPage: React.FC = () => {
  const [secretKey, setSecretKey] = useState('SecretKey012345678901234567890123456789012345678901234567890123456789');
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  
  // 测试直接使用字符串作为密钥
  const testDirectString = async () => {
    setLoading(true);
    setError(null);
    setResult(null);
    
    try {
      // 将字符串转换为字节数组
      const encoder = new TextEncoder();
      const keyData = encoder.encode(secretKey);
      
      // 导入密钥
      const key = await window.crypto.subtle.importKey(
        'raw',
        keyData,
        { name: 'HMAC', hash: { name: 'SHA-256' } },
        false,
        ['sign']
      );
      
      // 签名测试数据
      const testData = encoder.encode('test data');
      const signature = await window.crypto.subtle.sign(
        'HMAC',
        key,
        testData
      );
      
      // 将签名转换为十六进制字符串
      const signatureArray = new Uint8Array(signature);
      let signatureHex = '';
      for (let i = 0; i < signatureArray.length; i++) {
        signatureHex += ('00' + signatureArray[i].toString(16)).slice(-2);
      }
      
      setResult({
        method: '直接使用字符串作为密钥',
        keyLength: keyData.length,
        signature: signatureHex
      });
    } catch (error) {
      console.error('Error testing direct string:', error);
      setError('测试失败: ' + (error as Error).message);
    } finally {
      setLoading(false);
    }
  };
  
  // 测试将字符串作为 Base64 解码后作为密钥
  const testBase64Decode = async () => {
    setLoading(true);
    setError(null);
    setResult(null);
    
    try {
      // 尝试将字符串作为 Base64 解码
      let keyData: Uint8Array;
      try {
        // 将 Base64 转换为二进制字符串
        const binaryString = atob(secretKey);
        // 创建 Uint8Array
        keyData = new Uint8Array(binaryString.length);
        // 填充字节数组
        for (let i = 0; i < binaryString.length; i++) {
          keyData[i] = binaryString.charCodeAt(i);
        }
      } catch (decodeError) {
        setError('Base64 解码失败: ' + (decodeError as Error).message);
        return;
      }
      
      // 导入密钥
      const key = await window.crypto.subtle.importKey(
        'raw',
        keyData,
        { name: 'HMAC', hash: { name: 'SHA-256' } },
        false,
        ['sign']
      );
      
      // 签名测试数据
      const testData = new TextEncoder().encode('test data');
      const signature = await window.crypto.subtle.sign(
        'HMAC',
        key,
        testData
      );
      
      // 将签名转换为十六进制字符串
      const signatureArray = new Uint8Array(signature);
      let signatureHex = '';
      for (let i = 0; i < signatureArray.length; i++) {
        signatureHex += ('00' + signatureArray[i].toString(16)).slice(-2);
      }
      
      setResult({
        method: '将字符串作为 Base64 解码后作为密钥',
        keyLength: keyData.length,
        signature: signatureHex
      });
    } catch (error) {
      console.error('Error testing Base64 decode:', error);
      setError('测试失败: ' + (error as Error).message);
    } finally {
      setLoading(false);
    }
  };
  
  // 测试将字符串转换为 Base64 后再解码作为密钥
  const testBase64EncodeAndDecode = async () => {
    setLoading(true);
    setError(null);
    setResult(null);
    
    try {
      // 先将字符串转换为 Base64
      const base64 = btoa(secretKey);
      
      // 再将 Base64 解码为字节数组
      const binaryString = atob(base64);
      const keyData = new Uint8Array(binaryString.length);
      for (let i = 0; i < binaryString.length; i++) {
        keyData[i] = binaryString.charCodeAt(i);
      }
      
      // 导入密钥
      const key = await window.crypto.subtle.importKey(
        'raw',
        keyData,
        { name: 'HMAC', hash: { name: 'SHA-256' } },
        false,
        ['sign']
      );
      
      // 签名测试数据
      const testData = new TextEncoder().encode('test data');
      const signature = await window.crypto.subtle.sign(
        'HMAC',
        key,
        testData
      );
      
      // 将签名转换为十六进制字符串
      const signatureArray = new Uint8Array(signature);
      let signatureHex = '';
      for (let i = 0; i < signatureArray.length; i++) {
        signatureHex += ('00' + signatureArray[i].toString(16)).slice(-2);
      }
      
      setResult({
        method: '将字符串转换为 Base64 后再解码作为密钥',
        keyLength: keyData.length,
        signature: signatureHex
      });
    } catch (error) {
      console.error('Error testing Base64 encode and decode:', error);
      setError('测试失败: ' + (error as Error).message);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: '20px' }}>
      <h1>JWT 密钥测试</h1>
      
      <div style={{ marginBottom: '20px', padding: '15px', border: '1px solid #ddd', borderRadius: '4px' }}>
        <h2>密钥设置</h2>
        <div style={{ marginBottom: '10px' }}>
          <label style={{ display: 'block', marginBottom: '5px' }}>密钥字符串:</label>
          <input
            type="text"
            value={secretKey}
            onChange={(e) => setSecretKey(e.target.value)}
            style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
          />
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={testDirectString}
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
            {loading ? '测试中...' : '测试直接使用字符串'}
          </button>
          <button
            onClick={testBase64Decode}
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
            {loading ? '测试中...' : '测试 Base64 解码'}
          </button>
          <button
            onClick={testBase64EncodeAndDecode}
            disabled={loading}
            style={{
              padding: '10px 15px',
              backgroundColor: '#9C27B0',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: loading ? 'not-allowed' : 'pointer'
            }}
          >
            {loading ? '测试中...' : '测试 Base64 编码解码'}
          </button>
        </div>
      </div>
      
      {result && (
        <div style={{ marginBottom: '20px', padding: '15px', border: '1px solid #ddd', borderRadius: '4px' }}>
          <h2>测试结果</h2>
          <pre style={{ backgroundColor: '#f5f5f5', padding: '10px', borderRadius: '4px', overflow: 'auto' }}>
            {JSON.stringify(result, null, 2)}
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

export default JwtKeyTestPage;
