/**
 * 客户端工厂测试
 */
import { getApiClient } from '../client-factory';

describe('Client Factory', () => {
  it('should return a valid API client', () => {
    const client = getApiClient();
    expect(client).toBeDefined();
    expect(typeof client.get).toBe('function');
    expect(typeof client.post).toBe('function');
    expect(typeof client.put).toBe('function');
    expect(typeof client.delete).toBe('function');
  });
});
