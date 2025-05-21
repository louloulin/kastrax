/**
 * 响应转换测试
 */
import { transformResponse } from '../adapter/response';

describe('Response Transformation', () => {
  it('should handle empty response', () => {
    const result = transformResponse(null);
    expect(result).toEqual({
      code: 500,
      success: false,
      msg: '无响应',
      data: null
    });
  });

  it('should handle response with code 200', () => {
    const response = {
      code: 200,
      message: 'Success',
      data: { id: 1 }
    };
    const result = transformResponse(response);
    expect(result).toEqual({
      code: 200,
      success: true,
      msg: 'Success',
      data: { id: 1 }
    });
  });

  it('should handle response with success true', () => {
    const response = {
      success: true,
      message: 'Success',
      data: { id: 1 }
    };
    const result = transformResponse(response);
    expect(result).toEqual({
      code: 500,
      success: true,
      msg: 'Success',
      data: { id: 1 }
    });
  });

  it('should handle error response', () => {
    const response = {
      code: 400,
      message: 'Bad Request',
      data: null
    };
    const result = transformResponse(response);
    expect(result).toEqual({
      code: 400,
      success: false,
      msg: 'Bad Request',
      data: null
    });
  });

  it('should handle unexpected response format', () => {
    const response = {
      unexpected: 'format'
    };
    const result = transformResponse(response);
    expect(result.success).toBe(false);
    expect(result.code).toBe(500);
  });
});
