import api from '../utils/request';
import { buildBackendUrl } from '../config/api';
import { clearStoredAuth, ensureValidAccessToken } from '../utils/auth';

export interface NodeDefinition {
  id: number;
  nodeType: string;
  displayName: string;
  category: string;
  icon: string;
  inputSchema: string;
  outputSchema: string;
  configSchema: string;
}

export interface WorkflowData {
  name: string;
  description?: string;
  flowData: string;
  engineType?: string;
}

export interface Workflow {
  id: number;
  name: string;
  description: string;
  flowData: string;
  engineType?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface ExecutionNodeResult {
  nodeId: string;
  nodeName: string;
  status: 'SUCCESS' | 'FAILED' | 'RUNNING';
  input?: unknown;
  output?: unknown;
  duration?: number;
  error?: string;
}

export interface ExecutionResponse {
  executionId: number;
  status: 'SUCCESS' | 'FAILED' | 'RUNNING';
  inputData?: unknown;
  nodeResults: ExecutionNodeResult[];
  outputData: unknown;
  duration: number;
  errorMessage?: string;
}

export interface ResumeExecutionRequest {
  startNodeId?: string;
  useSnapshotVariables?: boolean;
  modifiedVariables?: Record<string, unknown>;
  skipSuccessNodes?: boolean;
}

export interface ExecutionSnapshot {
  id: number;
  executionId: number;
  flowId: number;
  nodeId: string;
  nodeType: string;
  nodeName?: string;
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED';
  inputData?: Record<string, unknown>;
  outputData?: Record<string, unknown>;
  errorMessage?: string;
  startedAt?: string;
  completedAt?: string;
  duration?: number;
  retryCount?: number;
  executionOrder?: number;
  createdAt?: string;
}

/**
 * 获取所有节点类型
 */
export const getNodeTypes = (): Promise<ApiResult<NodeDefinition[]>> => {
  return api.get('/api/node-types');
};

/**
 * 创建工作流
 */
export const createWorkflow = (data: WorkflowData): Promise<ApiResult<Workflow>> => {
  return api.post('/api/workflows', data);
};

/**
 * 获取工作流列表
 */
export const getWorkflows = (): Promise<ApiResult<Workflow[]>> => {
  return api.get('/api/workflows');
};

/**
 * 获取工作流详情
 */
export const getWorkflow = (id: number): Promise<ApiResult<Workflow>> => {
  return api.get(`/api/workflows/${id}`);
};

/**
 * 更新工作流
 */
export const updateWorkflow = (id: number, data: WorkflowData): Promise<ApiResult<Workflow>> => {
  return api.put(`/api/workflows/${id}`, data);
};

/**
 * 删除工作流
 */
export const deleteWorkflow = (id: number): Promise<ApiResult<void>> => {
  return api.delete(`/api/workflows/${id}`);
};

/**
 * 执行工作流
 */
export const executeWorkflow = (id: number, inputData: string): Promise<ApiResult<ExecutionResponse>> => {
  return api.post(`/api/workflows/${id}/execute`, { inputData });
};

/**
 * 获取最近一次执行记录
 */
export const getLatestExecution = (id: number): Promise<ApiResult<ExecutionResponse | null>> => {
  return api.get(`/api/workflows/${id}/executions/latest`);
};

export const getExecutionSnapshots = (
  id: number,
  executionId: number
): Promise<ApiResult<ExecutionSnapshot[]>> => {
  return api.get(`/api/workflows/${id}/executions/${executionId}/snapshots`);
};

export const resumeWorkflowExecution = (
  id: number,
  executionId: number,
  request: ResumeExecutionRequest = {}
): Promise<ApiResult<ExecutionResponse>> => {
  return api.post(`/api/workflows/${id}/executions/${executionId}/resume`, {
    skipSuccessNodes: true,
    useSnapshotVariables: true,
    ...request,
  });
};

export interface ExecutionEvent {
  eventType: string;
  nodeId?: string;
  nodeName?: string;
  status?: string;
  message?: string;
  data?: unknown;
  timestamp?: number;
}

export const executeWorkflowStream = async (
  id: number,
  inputData: string,
  onEvent: (event: ExecutionEvent) => void,
  onComplete: () => void,
  onError: (error: Error) => void
): Promise<AbortController | null> => {
  const token = await ensureValidAccessToken();

  if (!token) {
    clearStoredAuth();
    window.location.href = '/login';
    onError(new Error('未登录'));
    return null;
  }

  const controller = new AbortController();

  try {
    const response = await fetch(buildBackendUrl(`/api/workflows/${id}/execute/stream`), {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      body: JSON.stringify({ inputData }),
      signal: controller.signal,
    });

    if (response.status === 401) {
      clearStoredAuth();
      window.location.href = '/login';
      onError(new Error('认证失败，请重新登录'));
      return null;
    }
    if (!response.ok) {
      throw new Error(`工作流连接失败（HTTP ${response.status}）`);
    }
    if (!response.body) {
      throw new Error('浏览器不支持流式响应');
    }

    const readStream = async () => {
      const reader = response.body!.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let terminalEventReceived = false;

      const dispatchBlock = (block: string) => {
        let eventName = 'message';
        const dataLines: string[] = [];

        for (const line of block.replace(/\r\n/g, '\n').split('\n')) {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim();
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).replace(/^ /, ''));
          }
        }

        if (dataLines.length === 0) return;
        const event = JSON.parse(dataLines.join('\n')) as ExecutionEvent;
        onEvent(event);

        if (eventName === 'ERROR') {
          terminalEventReceived = true;
          onError(new Error(event.message || '工作流执行失败'));
        } else if (eventName === 'WORKFLOW_COMPLETE') {
          terminalEventReceived = true;
          onComplete();
        }
      };

      try {
        while (true) {
          const { value, done } = await reader.read();
          if (value) buffer += decoder.decode(value, { stream: !done });
          if (done) {
            buffer += decoder.decode();
            buffer = buffer.replace(/\r\n/g, '\n');
            if (buffer.trim()) dispatchBlock(buffer);
            break;
          }

          buffer = buffer.replace(/\r\n/g, '\n');
          let boundary = buffer.indexOf('\n\n');
          while (boundary >= 0) {
            const block = buffer.slice(0, boundary);
            buffer = buffer.slice(boundary + 2);
            dispatchBlock(block);
            if (terminalEventReceived) {
              await reader.cancel();
              return;
            }
            boundary = buffer.indexOf('\n\n');
          }
        }

        if (!terminalEventReceived) onComplete();
      } catch (error) {
        if (error instanceof Error && error.name === 'AbortError') return;
        onError(error instanceof Error ? error : new Error('流式连接中断'));
      } finally {
        reader.releaseLock();
      }
    };

    void readStream();
    return controller;
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') return null;
    onError(error instanceof Error ? error : new Error('工作流连接失败'));
    return null;
  }
};
