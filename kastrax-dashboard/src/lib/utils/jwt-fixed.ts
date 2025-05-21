/**
 * 修复的 JWT 工具类
 *
 * 这个模块提供了与后端兼容的 JWT token 生成和验证功能。
 * 关键修复：正确处理密钥，将 Base64 编码的密钥字符串解码为字节数组。
 */

/**
 * 密钥字符串
 * 在后端，这个字符串直接转换为字节数组：
 * secretKeyBytesDecoders = "SecretKey012345678901234567890123456789012345678901234567890123456789".getBytes();
 */
const SECRET_KEY_STRING = 'SecretKey012345678901234567890123456789012345678901234567890123456789';

/**
 * Base64 解码函数
 * 将 Base64 编码的字符串解码为 Uint8Array
 */
function base64Decode(base64: string): Uint8Array {
  try {
    // 将 Base64 转换为二进制字符串
    const binaryString = atob(base64);
    // 创建 Uint8Array
    const bytes = new Uint8Array(binaryString.length);
    // 填充字节数组
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes;
  } catch (error) {
    console.error('[JWT Fixed] Error decoding Base64:', error);
    throw error;
  }
}

/**
 * 检查字符串是否是有效的 Base64 编码
 */
function isValidBase64(str: string): boolean {
  try {
    return btoa(atob(str)) === str;
  } catch (e) {
    return false;
  }
}

/**
 * 将字符串转换为字节数组
 */
function stringToBytes(str: string): Uint8Array {
  return new TextEncoder().encode(str);
}

/**
 * 获取密钥字节数组
 * 与后端的 getSecretKeyBytesDecoders 方法相同
 */
function getSecretKeyBytes(): Uint8Array {
  // 直接将字符串转换为字节数组
  // 后端现在使用: secretKeyBytesDecoders = "SecretKey012345678901234567890123456789012345678901234567890123456789".getBytes();
  // 所以我们直接使用字符串的UTF-8编码
  console.log(`[JWT Fixed] Using raw string as key: ${SECRET_KEY_STRING.substring(0, 20)}...`);

  // 使用TextEncoder将字符串转换为Uint8Array
  const bytes = stringToBytes(SECRET_KEY_STRING);

  // 打印密钥字节以便调试
  let bytesHex = '';
  for (let i = 0; i < Math.min(bytes.length, 16); i++) {
    bytesHex += bytes[i].toString(16).padStart(2, '0') + ' ';
  }
  console.log(`[JWT Fixed] Secret key bytes (first 16): ${bytesHex}...`);
  console.log(`[JWT Fixed] Secret key bytes length: ${bytes.length}`);

  return bytes;
}

/**
 * Base64Url 编码 - 与后端的 Base64.getUrlEncoder().withoutPadding().encodeToString() 保持一致
 */
function base64UrlEncode(input: string): string {
  // 先将字符串转换为 UTF-8 字节数组
  const utf8Bytes = new TextEncoder().encode(input);

  // 将字节数组转换为 Base64 字符串
  let base64 = '';
  const base64Chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

  // 每3个字节转换为4个 Base64 字符
  for (let i = 0; i < utf8Bytes.length; i += 3) {
    const byte1 = utf8Bytes[i];
    const byte2 = i + 1 < utf8Bytes.length ? utf8Bytes[i + 1] : 0;
    const byte3 = i + 2 < utf8Bytes.length ? utf8Bytes[i + 2] : 0;

    const triplet = (byte1 << 16) | (byte2 << 8) | byte3;

    const char1 = base64Chars[(triplet >> 18) & 0x3F];
    const char2 = base64Chars[(triplet >> 12) & 0x3F];
    const char3 = i + 1 < utf8Bytes.length ? base64Chars[(triplet >> 6) & 0x3F] : '=';
    const char4 = i + 2 < utf8Bytes.length ? base64Chars[triplet & 0x3F] : '=';

    base64 += char1 + char2 + char3 + char4;
  }

  // 转换为 Base64Url 格式（替换 +, / 并移除填充）
  const result = base64
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');

  console.log(`[JWT Fixed] Base64Url encoded: ${input.substring(0, 20)}... => ${result.substring(0, 20)}...`);
  return result;
}

/**
 * Base64Url 解码
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
    console.error('[JWT Fixed] Error decoding token:', error);
    return null;
  }
}

/**
 * 检查 token 是否过期
 */
export function isTokenExpired(token: string): boolean {
  const payload = decodeToken(token);
  if (!payload || !payload.exp) return true;

  const now = Math.floor(Date.now() / 1000);
  return payload.exp < now;
}

/**
 * 创建 JWT token
 * 使用与后端相同的密钥处理方式
 */
export async function createToken(username: string): Promise<string> {
  try {
    // 创建 header - 与后端完全一致
    const header = {
      alg: 'HS256',
      typ: 'JWT'
    };

    // 创建 payload - 与后端完全一致
    const now = Math.floor(Date.now() / 1000);

    // 注意：与后端保持完全一致
    // 后端使用 Jwts.claims().setSubject(userName) 只设置了subject和过期时间
    // 不要添加任何其他字段，如iat、name等
    const payload = {
      sub: username,
      exp: now + 18000 // 5小时，与后端相同
    };

    // 打印详细信息以便调试
    console.log(`[JWT Fixed] Creating token for user: ${username}`);
    console.log(`[JWT Fixed] Token expiration: ${new Date((now + 18000) * 1000).toISOString()}`);

    // 编码 header 和 payload
    const headerStr = JSON.stringify(header);
    const payloadStr = JSON.stringify(payload);

    console.log(`[JWT Fixed] Header: ${headerStr}`);
    console.log(`[JWT Fixed] Payload: ${payloadStr}`);

    const encodedHeader = base64UrlEncode(headerStr);
    const encodedPayload = base64UrlEncode(payloadStr);

    // 要签名的数据
    const dataToSign = `${encodedHeader}.${encodedPayload}`;
    console.log(`[JWT Fixed] Data to sign: ${dataToSign}`);

    // 打印数据的字节表示
    const dataToSignBytes = new TextEncoder().encode(dataToSign);
    let dataHex = '';
    for (let i = 0; i < Math.min(dataToSignBytes.length, 32); i++) {
      dataHex += dataToSignBytes[i].toString(16).padStart(2, '0') + ' ';
    }
    console.log(`[JWT Fixed] Data to sign bytes (first 32): ${dataHex}...`);

    // 获取密钥字节数组，与后端相同
    const keyData = getSecretKeyBytes();

    // 导入密钥
    const key = await window.crypto.subtle.importKey(
      'raw',
      keyData,
      { name: 'HMAC', hash: { name: 'SHA-256' } },
      false,
      ['sign']
    );

    // 签名 - 使用与后端相同的方式
    // 注意：后端使用 dataToSign.getBytes() 获取字节，这在Java中是使用UTF-8编码
    const signature = await window.crypto.subtle.sign(
      'HMAC',
      key,
      new TextEncoder().encode(dataToSign)
    );

    console.log(`[JWT Fixed] Signature created successfully, length: ${signature.byteLength} bytes`);

    // 将签名转换为 Base64Url 字符串 - 与后端保持一致
    const signatureArray = new Uint8Array(signature);

    // 打印签名字节以便调试
    let signatureHex = '';
    for (let i = 0; i < Math.min(signatureArray.length, 16); i++) {
      signatureHex += signatureArray[i].toString(16).padStart(2, '0') + ' ';
    }
    console.log(`[JWT Fixed] Signature bytes (first 16): ${signatureHex}...`);
    console.log(`[JWT Fixed] Signature bytes length: ${signatureArray.length}`);

    // 直接将字节数组转换为 Base64 字符串
    let base64 = '';
    const base64Chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

    // 每3个字节转换为4个 Base64 字符
    for (let i = 0; i < signatureArray.length; i += 3) {
      const byte1 = signatureArray[i];
      const byte2 = i + 1 < signatureArray.length ? signatureArray[i + 1] : 0;
      const byte3 = i + 2 < signatureArray.length ? signatureArray[i + 2] : 0;

      const triplet = (byte1 << 16) | (byte2 << 8) | byte3;

      const char1 = base64Chars[(triplet >> 18) & 0x3F];
      const char2 = base64Chars[(triplet >> 12) & 0x3F];
      const char3 = i + 1 < signatureArray.length ? base64Chars[(triplet >> 6) & 0x3F] : '=';
      const char4 = i + 2 < signatureArray.length ? base64Chars[triplet & 0x3F] : '=';

      base64 += char1 + char2 + char3 + char4;
    }

    // 转换为 Base64Url 格式（替换 +, / 并移除填充）
    const signatureBase64 = base64
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');

    console.log(`[JWT Fixed] Signature Base64Url: ${signatureBase64}`);

    // 测试另一种方式生成的签名
    let signatureString = '';
    for (let i = 0; i < signatureArray.length; i++) {
      signatureString += String.fromCharCode(signatureArray[i]);
    }
    const signatureBase64Alt = base64UrlEncode(signatureString);
    console.log(`[JWT Fixed] Signature Base64Url (alt method): ${signatureBase64Alt}`);

    // 使用第一种方式生成的签名

    // 返回完整的 JWT token
    const token = `${encodedHeader}.${encodedPayload}.${signatureBase64}`;
    console.log(`[JWT Fixed] Created token: ${token}`);

    // 测试另一种方式生成的token
    const tokenAlt = `${encodedHeader}.${encodedPayload}.${signatureBase64Alt}`;
    console.log(`[JWT Fixed] Created token (alt method): ${tokenAlt}`);

    // 使用第一种方式生成的token
    return token;
  } catch (error) {
    console.error('[JWT Fixed] Error creating token:', error);
    throw error;
  }
}

/**
 * 验证 JWT token
 */
export async function verifyToken(token: string): Promise<any> {
  if (!token) return null;

  try {
    // 分割 token
    const parts = token.split('.');
    if (parts.length !== 3) return null;

    const [encodedHeader, encodedPayload, encodedSignature] = parts;

    // 要验证的数据
    const dataToVerify = `${encodedHeader}.${encodedPayload}`;

    // 获取密钥字节数组，与后端相同
    const keyData = getSecretKeyBytes();

    // 导入密钥
    const key = await window.crypto.subtle.importKey(
      'raw',
      keyData,
      { name: 'HMAC', hash: { name: 'SHA-256' } },
      false,
      ['verify']
    );

    // 将签名从 Base64Url 转换为 Uint8Array
    const signatureString = base64UrlDecode(encodedSignature);
    const signatureArray = new Uint8Array(signatureString.length);
    for (let i = 0; i < signatureString.length; i++) {
      signatureArray[i] = signatureString.charCodeAt(i);
    }

    // 验证签名
    const isValid = await window.crypto.subtle.verify(
      'HMAC',
      key,
      signatureArray,
      new TextEncoder().encode(dataToVerify)
    );

    if (!isValid) return null;

    // 解码 payload
    const payload = JSON.parse(base64UrlDecode(encodedPayload));

    // 检查 token 是否过期
    const now = Math.floor(Date.now() / 1000);
    if (payload.exp && payload.exp < now) return null;

    return payload;
  } catch (error) {
    console.error('[JWT Fixed] Error verifying token:', error);
    return null;
  }
}

/**
 * 获取有效的 token
 * 优先使用存储的 token，如果不存在或已过期，则创建新的 token
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
      console.warn('[JWT Fixed] No username provided, cannot create token');
      return null;
    }
  }

  try {
    // 创建新 token
    const newToken = await createToken(username);

    // 存储新 token
    localStorage.setItem('token', newToken);
    localStorage.setItem('username', username);

    return newToken;
  } catch (error) {
    console.error('[JWT Fixed] Error getting valid token:', error);
    return null;
  }
}

/**
 * JWT 工具对象
 */
export const jwtFixed = {
  decodeToken,
  isTokenExpired,
  createToken,
  verifyToken,
  getValidToken,
  // 内部函数，用于调试
  _getSecretKeyBytes: getSecretKeyBytes
};
