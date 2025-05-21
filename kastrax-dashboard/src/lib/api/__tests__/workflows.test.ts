import { rest } from 'msw';
import { setupServer } from 'msw/node';
import * as v2 from '../v2/workflows';
import type {
  Workflow,
  WorkflowStep,
  WorkflowCreatePayload,
  WorkflowUpdatePayload,
  WorkflowExecutionResult,
  WorkflowsResponse,
  SingleWorkflowResponse,
  WorkflowExecutionHistory
} from '../v2/workflows';

const mockStep: WorkflowStep = {
  stepId: 'step-1',
  name: 'Test Step',
  type: 'ACTION',
  config: {
    action: 'test'
  },
  nextSteps: ['step-2']
};

const mockWorkflow: Workflow = {
  workflowId: 'test-workflow-1',
  name: 'Test Workflow',
  description: 'Test workflow description',
  status: 'ACTIVE',
  triggerType: 'MANUAL',
  steps: [mockStep],
  createTime: '2024-01-01T00:00:00Z',
  updateTime: '2024-01-01T00:00:00Z'
};

const mockExecution: WorkflowExecutionResult = {
  executionId: 'exec-1',
  workflowId: 'test-workflow-1',
  status: 'SUCCESS',
  startTime: '2024-01-01T00:00:00Z',
  endTime: '2024-01-01T00:00:01Z',
  result: { success: true }
};

const mockListResponse = {
  code: 200,
  success: true,
  msg: 'Success',
  data: {
    workflows: [mockWorkflow],
    total: 1,
    page: 1,
    size: 10
  }
};

const mockSingleResponse = {
  code: 200,
  success: true,
  msg: 'Success',
  data: {
    workflow: mockWorkflow
  }
};

const mockExecutionResponse = {
  code: 200,
  success: true,
  msg: 'Success',
  data: mockExecution
};

const mockHistoryResponse = {
  code: 200,
  success: true,
  msg: 'Success',
  data: {
    executions: [mockExecution],
    total: 1,
    page: 1,
    size: 10
  }
};

const server = setupServer(
  rest.get('/api/v2/workflows', (req, res, ctx) => {
    return res(ctx.json(mockListResponse));
  }),
  rest.get('/api/v2/workflows/:id', (req, res, ctx) => {
    return res(ctx.json(mockSingleResponse));
  }),
  rest.post('/api/v2/workflows', (req, res, ctx) => {
    return res(ctx.json(mockSingleResponse));
  }),
  rest.put('/api/v2/workflows/:id', (req, res, ctx) => {
    return res(ctx.json(mockSingleResponse));
  }),
  rest.delete('/api/v2/workflows/:id', (req, res, ctx) => {
    return res(ctx.json({ code: 200, success: true, msg: 'Success' }));
  }),
  rest.post('/api/v2/workflows/:id/execute', (req, res, ctx) => {
    return res(ctx.json(mockExecutionResponse));
  }),
  rest.get('/api/v2/workflows/:id/executions/:execId', (req, res, ctx) => {
    return res(ctx.json(mockExecutionResponse));
  }),
  rest.put('/api/v2/workflows/:id/status', (req, res, ctx) => {
    return res(ctx.json(mockSingleResponse));
  }),
  rest.get('/api/v2/workflows/:id/executions', (req, res, ctx) => {
    return res(ctx.json(mockHistoryResponse));
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Workflows API', () => {
  describe('v2 Workflows API', () => {
    test('getWorkflows should fetch workflows correctly', async () => {
      const response = await v2.getWorkflows(1, 10);
      expect(response).toEqual(mockListResponse);
    });

    test('getWorkflow should fetch single workflow', async () => {
      const response = await v2.getWorkflow('test-workflow-1');
      expect(response).toEqual(mockSingleResponse);
    });

    test('createWorkflow should create workflow', async () => {
      const payload: WorkflowCreatePayload = {
        name: 'Test Workflow',
        description: 'Test workflow description',
        triggerType: 'MANUAL',
        steps: [{
          name: 'Test Step',
          type: 'ACTION',
          config: {
            action: 'test'
          },
          nextSteps: ['step-2']
        }]
      };
      const response = await v2.createWorkflow(payload);
      expect(response).toEqual(mockSingleResponse);
    });

    test('updateWorkflow should update workflow', async () => {
      const workflowId = 'test-workflow-1';
      const payload: WorkflowUpdatePayload = {
        name: 'Updated Workflow',
        description: 'Updated description'
      };
      const response = await v2.updateWorkflow(workflowId, payload);
      expect(response).toEqual(mockSingleResponse);
    });

    test('deleteWorkflow should delete workflow', async () => {
      const response = await v2.deleteWorkflow('test-workflow-1');
      expect(response).toEqual({ code: 200, success: true, msg: 'Success' });
    });

    test('executeWorkflow should execute workflow', async () => {
      const response = await v2.executeWorkflow('test-workflow-1', { test: true });
      expect(response).toEqual(mockExecutionResponse);
    });

    test('getWorkflowExecutionStatus should fetch execution status', async () => {
      const response = await v2.getWorkflowExecutionStatus('test-workflow-1', 'exec-1');
      expect(response).toEqual(mockExecutionResponse);
    });

    test('updateWorkflowStatus should update workflow status', async () => {
      const response = await v2.updateWorkflowStatus('test-workflow-1', 'PAUSED');
      expect(response).toEqual(mockSingleResponse);
    });

    test('getWorkflowExecutionHistory should fetch execution history', async () => {
      const response = await v2.getWorkflowExecutionHistory('test-workflow-1', 1, 10);
      expect(response).toEqual(mockHistoryResponse);
    });
  });
}); 