/**
 * 参数转换测试
 */
import { 
  transformAuthParams, 
  transformRuleParams,
  transformResourceParams,
  transformScriptParams,
  transformSystemParams
} from '../adapter/transform';

describe('Parameter Transformations', () => {
  describe('Auth Parameters', () => {
    it('should transform login parameters', () => {
      const params = {
        username: 'test',
        password: 'password',
        extra: 'value'
      };
      const result = transformAuthParams.login(params);
      expect(result).toEqual(params);
    });

    it('should handle null parameters', () => {
      const result = transformAuthParams.login(null);
      expect(result).toEqual(null);
    });
  });

  describe('Rule Parameters', () => {
    it('should transform list parameters', () => {
      const params = {
        page: 1,
        size: 10,
        filter: 'test'
      };
      const result = transformRuleParams.list(params);
      expect(result).toEqual({ filter: 'test' });
    });

    it('should transform create parameters', () => {
      const params = {
        graph: { nodes: [], edges: [] },
        type: 'test',
        description: 'description'
      };
      const result = transformRuleParams.create(params);
      expect(result).toEqual({
        graphJson: JSON.stringify({ nodes: [], edges: [] }),
        type: 'test',
        description: 'description'
      });
    });

    it('should handle error in transformation', () => {
      const params = {
        graph: { 
          circular: {} // Circular reference
        }
      };
      // Make circular reference
      params.graph.circular = params.graph;
      
      // This should not throw an error
      const result = transformRuleParams.create(params);
      expect(result).toEqual(params);
    });
  });
});
