import { getRequest, postRequest, putRequest, deleteRequest } from '../utils';

export type WorkflowStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ARCHIVED';
export type WorkflowTriggerType = 'MANUAL' | 'SCHEDULED' | 'EVENT';

export interface WorkflowStep {
  stepId: string;
  name: string;
  type: string;
  config: Record<string, any>;
  nextSteps?: string[];
}

export interface Workflow {
  workflowId: string;
  name: string;
  description?: string;
  status: WorkflowStatus;
  triggerType: WorkflowTriggerType;
  triggerConfig?: Record<string, any>;
  steps: WorkflowStep[];
  createTime: string;
  updateTime: string;
  lastRunTime?: string;
}

export interface WorkflowCreatePayload {
  name: string;
  description?: string;
  triggerType: WorkflowTriggerType;
  triggerConfig?: Record<string, any>;
  steps: Omit<WorkflowStep, 'stepId'>[];
}

export interface WorkflowUpdatePayload {
  name?: string;
  description?: string;
  triggerType?: WorkflowTriggerType;
  triggerConfig?: Record<string, any>;
  steps?: WorkflowStep[];
  status?: WorkflowStatus;
}

export interface WorkflowsResponse {
  workflows: Workflow[];
  total: number;
  page: number;
  size: number;
}

export interface SingleWorkflowResponse {
  workflow: Workflow;
}

export interface WorkflowExecutionResult {
  executionId: string;
  workflowId: string;
  status: 'SUCCESS' | 'FAILED' | 'RUNNING';
  startTime: string;
  endTime?: string;
  result?: any;
  error?: string;
}

// Get workflows list with pagination
export async function getWorkflows(page: number = 1, size: number = 10) {
  return getRequest<WorkflowsResponse>('/v2/workflows', { page, size });
}

// Get single workflow
export async function getWorkflow(workflowId: string) {
  return getRequest<SingleWorkflowResponse>(`/v2/workflows/${workflowId}`);
}

// Create workflow
export async function createWorkflow(data: WorkflowCreatePayload) {
  return postRequest<SingleWorkflowResponse>('/v2/workflows', data);
}

// Update workflow
export async function updateWorkflow(workflowId: string, data: WorkflowUpdatePayload) {
  return putRequest<SingleWorkflowResponse>(`/v2/workflows/${workflowId}`, data);
}

// Delete workflow
export async function deleteWorkflow(workflowId: string) {
  return deleteRequest<void>(`/v2/workflows/${workflowId}`);
}

// Execute workflow
export async function executeWorkflow(workflowId: string, input?: Record<string, any>) {
  return postRequest<WorkflowExecutionResult>(`/v2/workflows/${workflowId}/execute`, { input });
}

// Get workflow execution status
export async function getWorkflowExecutionStatus(workflowId: string, executionId: string) {
  return getRequest<WorkflowExecutionResult>(`/v2/workflows/${workflowId}/executions/${executionId}`);
}

// Update workflow status
export async function updateWorkflowStatus(workflowId: string, status: WorkflowStatus) {
  return putRequest<SingleWorkflowResponse>(`/v2/workflows/${workflowId}/status`, { status });
}

// Get workflow execution history
export interface WorkflowExecutionHistory {
  executions: WorkflowExecutionResult[];
  total: number;
  page: number;
  size: number;
}

export async function getWorkflowExecutionHistory(workflowId: string, page: number = 1, size: number = 10) {
  return getRequest<WorkflowExecutionHistory>(`/v2/workflows/${workflowId}/executions`, { page, size });
} 