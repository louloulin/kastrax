/**
 * JWT Service
 *
 * 提供 JWT token 的生成、验证和解码功能。
 * 优先使用服务器生成的 token，只在必要时在前端生成 token。
 */

// 后端使用的密钥（与 AuthConfig.java 中的相同）
// 在后端，这个密钥直接转换为字节数组：secretKeyBytesDecoders = "SecretKey012345678901234567890123456789012345678901234567890123456789".getBytes()
const SECRET_KEY = 'SecretKey012345678901234567890123456789012345678901234567890123456789';

// Token 有效期（秒）
const TOKEN_VALIDITY_SECONDS = 18000; // 5小时，与后端相同

/**
 * Base64Url 编码
 *
 * @param input 要编码的字符串
 * @returns Base64Url 编码的字符串
 */
function base64UrlEncode(input: string): string {
  // 先使用标准 Base64 编码
  const base64 = btoa(input);
  // 转换为 Base64Url 格式（替换 +, / 并移除填充）
  return base64
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

/**
 * Base64Url 解码
 *
 * @param input Base64Url 编码的字符串
 * @returns 解码后的字符串
 */
function base64UrlDecode(input: string): string {
  // 转换回标准 Base64 格式
  let base64 = input.replace(/-/g, '+').replace(/_/g, '/');

  // 添加填充
  while (base64.length % 4) {
    base64 += '=';
  }

  // 解码
  return atob(base64);
}

/**
 * 解码 JWT token
 *
 * @param token JWT token
 * @returns 解码后的 payload，如果 token 无效则返回 null
 */
export function decodeToken(token: string): any {
  if (!token) return null;

  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;

    const [, payloadBase64] = parts;
    const payloadJson = base64UrlDecode(payloadBase64);
    return JSON.parse(payloadJson);
  } catch (error) {
    console.error('[JWT Service] Error decoding token:', error);
    return null;
  }
}

/**
 * 检查 token 是否过期
 *
 * @param token JWT token
 * @returns 如果 token 过期则返回 true，否则返回 false
 */
export function isTokenExpired(token: string): boolean {
  const payload = decodeToken(token);
  if (!payload || !payload.exp) return true;

  const now = Math.floor(Date.now() / 1000);
  return payload.exp < now;
}

/**
 * 在浏览器中使用 Web Crypto API 创建 JWT token
 *
 * @param username 用户名
 * @returns 返回 Promise，解析为 JWT token
 */
export async function createTokenWithWebCrypto(username: string): Promise<string> {
  try {
    // 创建 header
    const header = {
      alg: 'HS256',
      typ: 'JWT'
    };

    // 创建 payload - 确保与后端格式完全一致
    const now = Math.floor(Date.now() / 1000);
    const payload = {
      sub: username,
      exp: now + TOKEN_VALIDITY_SECONDS
    };

    // 编码 header 和 payload
    const encodedHeader = base64UrlEncode(JSON.stringify(header));
    const encodedPayload = base64UrlEncode(JSON.stringify(payload));

    // 要签名的数据
    const dataToSign = `${encodedHeader}.${encodedPayload}`;

    // 将密钥转换为 Uint8Array - 确保与后端使用相同的字节编码
    const encoder = new TextEncoder();
    const keyData = encoder.encode(SECRET_KEY);

    // 导入密钥
    const key = await window.crypto.subtle.importKey(
      'raw',
      keyData,
      { name: 'HMAC', hash: { name: 'SHA-256' } },
      false,
      ['sign']
    );

    // 签名
    const signature = await window.crypto.subtle.sign(
      'HMAC',
      key,
      encoder.encode(dataToSign)
    );

    // 将签名转换为 Base64Url 字符串
    const signatureArray = new Uint8Array(signature);
    let signatureString = '';
    for (let i = 0; i < signatureArray.length; i++) {
      signatureString += String.fromCharCode(signatureArray[i]);
    }
    const signatureBase64 = base64UrlEncode(signatureString);

    // 打印调试信息
    console.log(`[JWT Service] Created token header: ${encodedHeader}`);
    console.log(`[JWT Service] Created token payload: ${encodedPayload}`);
    console.log(`[JWT Service] Created token signature: ${signatureBase64.substring(0, 10)}...`);

    // 返回完整的 JWT token
    const token = `${encodedHeader}.${encodedPayload}.${signatureBase64}`;
    console.log(`[JWT Service] Complete token: ${token}`);
    return token;
  } catch (error) {
    console.error('[JWT Service] Error creating token with Web Crypto API:', error);
    throw error;
  }
}

/**
 * 获取有效的 JWT token
 *
 * 优先使用存储在 localStorage 中的 token，如果不存在或已过期，
 * 则尝试创建一个新的 token。
 *
 * @param username 用户名（可选，如果需要创建新 token）
 * @returns 返回 Promise，解析为 JWT token 或 null
 */
export async function getValidToken(username?: string): Promise<string | null> {
  // 从 localStorage 获取 token
  const token = localStorage.getItem('token');

  // 如果 token 存在且未过期，直接返回
  if (token && !isTokenExpired(token)) {
    return token;
  }

  // 如果没有提供用户名，无法创建新 token
  if (!username) {
    username = localStorage.getItem('username') || '';
    if (!username) {
      console.warn('[JWT Service] No username provided, cannot create token');
      return null;
    }
  }

  try {
    // 创建新 token
    const newToken = await createTokenWithWebCrypto(username);

    // 存储新 token
    localStorage.setItem('token', newToken);
    localStorage.setItem('username', username);

    return newToken;
  } catch (error) {
    console.error('[JWT Service] Error getting valid token:', error);
    return null;
  }
}

/**
 * JWT 服务
 */
export const jwtService = {
  decodeToken,
  isTokenExpired,
  createTokenWithWebCrypto,
  getValidToken
};
