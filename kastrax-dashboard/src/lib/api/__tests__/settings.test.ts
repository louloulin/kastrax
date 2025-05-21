import { rest } from 'msw';
import { setupServer } from 'msw/node';
import * as v2 from '../v2/settings';
import type { 
  Category,
  SystemSetting,
  SettingCreatePayload,
  SettingUpdatePayload,
  BatchUpdatePayload,
  SettingsResponse,
  SingleSettingResponse
} from '../v2/settings';

const mockSettings: SystemSetting[] = [{
  settingId: 'test-setting-1',
  key: 'test-key',
  value: 'test-value',
  description: 'Test setting',
  category: 'general',
  createTime: '2024-01-01T00:00:00Z',
  updateTime: '2024-01-01T00:00:00Z'
}];

const mockResponse = {
  code: 200,
  success: true,
  msg: 'Success',
  data: {
    settings: mockSettings
  }
};

const mockSingleResponse = {
  code: 200,
  success: true,
  msg: 'Success',
  data: {
    setting: mockSettings[0]
  }
};

const server = setupServer(
  rest.get('/api/v2/settings', (req, res, ctx) => {
    return res(ctx.json(mockResponse));
  }),
  rest.get('/api/v2/settings/category/:category', (req, res, ctx) => {
    return res(ctx.json(mockResponse));
  }),
  rest.get('/api/v2/settings/:id', (req, res, ctx) => {
    return res(ctx.json(mockSingleResponse));
  }),
  rest.post('/api/v2/settings', (req, res, ctx) => {
    return res(ctx.json(mockSingleResponse));
  }),
  rest.put('/api/v2/settings/:id', (req, res, ctx) => {
    return res(ctx.json(mockSingleResponse));
  }),
  rest.delete('/api/v2/settings/:id', (req, res, ctx) => {
    return res(ctx.json(mockSingleResponse));
  }),
  rest.put('/api/v2/settings/batch', (req, res, ctx) => {
    return res(ctx.json(mockResponse));
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Settings API', () => {
  describe('v2 Settings API', () => {
    test('getSystemSettings should fetch settings correctly', async () => {
      const response = await v2.getSystemSettings();
      expect(response).toEqual(mockResponse);
    });

    test('getSettingsByCategory should fetch settings by category', async () => {
      const category: Category = 'general';
      const response = await v2.getSettingsByCategory(category);
      expect(response).toEqual(mockResponse);
    });

    test('getSetting should fetch single setting', async () => {
      const response = await v2.getSetting('test-setting-1');
      expect(response).toEqual(mockSingleResponse);
    });

    test('createSetting should create setting', async () => {
      const payload: SettingCreatePayload = {
        key: 'test-key',
        value: 'test-value',
        description: 'Test setting',
        category: 'general'
      };
      const response = await v2.createSetting(payload);
      expect(response).toEqual(mockSingleResponse);
    });

    test('updateSetting should update setting', async () => {
      const settingId = 'test-setting-1';
      const payload: SettingUpdatePayload = {
        value: 'test-value',
        description: 'Test setting'
      };
      const response = await v2.updateSetting(settingId, payload);
      expect(response).toEqual(mockSingleResponse);
    });

    test('deleteSetting should delete setting', async () => {
      const response = await v2.deleteSetting('test-setting-1');
      expect(response).toEqual(mockSingleResponse);
    });

    test('batchUpdateSettings should update multiple settings', async () => {
      const payload: BatchUpdatePayload[] = [{
        settingId: 'test-setting-1',
        value: 'test-value'
      }];
      const response = await v2.batchUpdateSettings(payload);
      expect(response).toEqual(mockResponse);
    });
  });
});