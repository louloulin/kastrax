/**
 * JWT验证工具
 * 
 * 这个工具用于验证JWT令牌的格式和内容，帮助调试JWT认证问题
 */

/**
 * 解析JWT令牌
 * @param {string} token JWT令牌
 * @returns {Object} 解析结果
 */
function parseJwt(token) {
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
        error: '无法解码header或payload: ' + e.message,
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
      error: error.message
    };
  }
}

/**
 * Base64URL解码
 * @param {string} input Base64URL编码的字符串
 * @returns {string} 解码后的字符串
 */
function base64UrlDecode(input) {
  // 转换回标准Base64格式
  let base64 = input.replace(/-/g, '+').replace(/_/g, '/');
  
  // 添加填充
  while (base64.length % 4) {
    base64 += '=';
  }
  
  // 解码
  return atob(base64);
}

/**
 * 验证JWT令牌
 * @param {string} token JWT令牌
 * @returns {Object} 验证结果
 */
function validateJwt(token) {
  const result = parseJwt(token);
  
  if (!result.valid) {
    return result;
  }
  
  // 检查令牌是否过期
  const now = Math.floor(Date.now() / 1000);
  const isExpired = result.decoded.payload.exp && result.decoded.payload.exp < now;
  
  // 检查必要的字段
  const hasRequiredFields = 
    result.decoded.header.alg && 
    result.decoded.header.typ && 
    result.decoded.payload.sub;
  
  return {
    ...result,
    isExpired,
    hasRequiredFields,
    timeRemaining: result.decoded.payload.exp ? result.decoded.payload.exp - now : null
  };
}

/**
 * 打印JWT令牌验证结果
 * @param {string} token JWT令牌
 */
function printJwtValidation(token) {
  const result = validateJwt(token);
  
  console.group('JWT令牌验证');
  console.log('令牌:', token);
  
  if (!result.valid) {
    console.error('验证失败:', result.error);
    console.groupEnd();
    return;
  }
  
  console.log('格式有效:', result.valid);
  console.log('包含Base64填充:', result.hasBase64Padding ? '是 (不符合JWT规范)' : '否 (符合JWT规范)');
  console.log('包含无效字符:', result.hasInvalidChars ? '是 (不符合JWT规范)' : '否 (符合JWT规范)');
  console.log('已过期:', result.isExpired ? '是' : '否');
  console.log('包含必要字段:', result.hasRequiredFields ? '是' : '否');
  
  if (result.timeRemaining !== null) {
    console.log('剩余有效时间:', result.timeRemaining > 0 ? `${result.timeRemaining}秒` : '已过期');
  }
  
  console.group('令牌各部分');
  console.log('Header (Base64URL):', result.parts.header);
  console.log('Payload (Base64URL):', result.parts.payload);
  console.log('Signature (Base64URL):', result.parts.signature);
  console.groupEnd();
  
  console.group('解码内容');
  console.log('Header:', result.decoded.header);
  console.log('Payload:', result.decoded.payload);
  console.groupEnd();
  
  console.groupEnd();
}

// 导出工具函数
export const jwtValidator = {
  parseJwt,
  validateJwt,
  printJwtValidation
};

// 添加到全局对象，方便在控制台调试
if (typeof window !== 'undefined') {
  window.jwtValidator = jwtValidator;
}
