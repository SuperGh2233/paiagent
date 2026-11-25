package com.paiagent.engine.executor.impl;

import com.paiagent.dto.ExecutionEvent;
import com.paiagent.engine.model.WorkflowNode;
import com.paiagent.service.AgnesVideoClient;
import com.paiagent.service.AgentPlanConfigResolver;
import com.paiagent.service.MinioService;
import com.paiagent.service.ResolvedAgentPlanConfig;
import com.paiagent.service.VolcengineAgentPlanClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoGenerateNodeExecutorTest {

    @Mock
    private AgentPlanConfigResolver configResolver;

    @Mock
    private VolcengineAgentPlanClient agentPlanClient;

    @Mock
    private AgnesVideoClient agnesVideoClient;

    @Mock
    private MinioService minioService;

    @Mock
    private WorkflowNode node;

    @Mock
    private Consumer<ExecutionEvent> progressCallback;

    private final ResolvedAgentPlanConfig config = new ResolvedAgentPlanConfig(
            1L,
            "volcengine_agent_plan",
            "https://example.invalid",
            "test-api-key",
            "test-video-model",
            null,
            null,
            "test-video-model",
            false
    );

    private VideoGenerateNodeExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new VideoGenerateNodeExecutor(
                configResolver,
                agentPlanClient,
                agnesVideoClient,
                minioService,
                0
        );
        lenient().when(node.getId()).thenReturn("node-1");
        lenient().when(node.getType()).thenReturn("video_generate");
        lenient().when(node.getData()).thenReturn(Map.of());
        lenient().when(configResolver.resolve(node, "video")).thenReturn(config);
    }

    @Test
    void executeCompletesTaskAndPersistsVideo() throws Exception {
        String taskId = "task-123";
        String sourceUrl = "https://media.example.invalid/video.mp4";
        String persistedUrl = "https://storage.example.invalid/video.mp4";
        when(agentPlanClient.createVideoTask(eq(config), eq("A cat playing piano"), isNull(),
                eq(5), isNull(), eq("adaptive"), isNull())).thenReturn(taskId);
        when(agentPlanClient.getVideoTask(config, taskId))
                .thenReturn(Map.of("status", "running"))
                .thenReturn(Map.of("status", "success", "videoUrl", sourceUrl, "coverUrl", "cover.jpg"));
        when(minioService.uploadFromUrl(sourceUrl, "videos/generated-" + taskId + ".mp4", "video/mp4"))
                .thenReturn(persistedUrl);

        Map<String, Object> result = executor.execute(
                node, Map.of("prompt", "A cat playing piano"), progressCallback
        );

        assertEquals(taskId, result.get("taskId"));
        assertEquals("succeeded", result.get("status"));
        assertEquals(persistedUrl, result.get("videoUrl"));
        assertEquals("cover.jpg", result.get("coverUrl"));
        assertEquals("test-video-model", result.get("model"));
        verify(agentPlanClient, times(2)).getVideoTask(config, taskId);
        verify(progressCallback, atLeast(4)).accept(any(ExecutionEvent.class));
    }

    @Test
    void executeRejectsMissingPrompt() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(node, Map.of(), progressCallback)
        );
        assertTrue(error.getMessage().contains("视频生成节点缺少 prompt"));
        verifyNoInteractions(agentPlanClient);
    }

    @Test
    void executeRejectsMissingTaskId() throws Exception {
        when(agentPlanClient.createVideoTask(any(), anyString(), any(), anyInt(), any(), any(), any()))
                .thenReturn("");

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> executor.execute(node, Map.of("prompt", "test prompt"), progressCallback)
        );

        assertTrue(error.getMessage().contains("视频生成接口未返回 taskId"));
        verify(agentPlanClient, never()).getVideoTask(any(), anyString());
    }

    @Test
    void executeFailsWhenProviderReportsFailedTask() throws Exception {
        when(agentPlanClient.createVideoTask(any(), anyString(), any(), anyInt(), any(), any(), any()))
                .thenReturn("task-failed");
        when(agentPlanClient.getVideoTask(config, "task-failed"))
                .thenReturn(Map.of("status", "failed", "error", "provider failure"));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> executor.execute(node, Map.of("prompt", "test prompt"), progressCallback)
        );

        assertTrue(error.getMessage().contains("视频生成失败"));
    }

    @Test
    void executeFailsWhenPollingEndsWithoutVideoUrl() throws Exception {
        when(agentPlanClient.createVideoTask(any(), anyString(), any(), anyInt(), any(), any(), any()))
                .thenReturn("task-timeout");
        when(agentPlanClient.getVideoTask(config, "task-timeout"))
                .thenReturn(Map.of("status", "running"));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> executor.execute(node, Map.of("prompt", "test prompt"), progressCallback)
        );

        assertTrue(error.getMessage().contains("视频生成超时或未返回 videoUrl"));
        verify(agentPlanClient, times(60)).getVideoTask(config, "task-timeout");
    }

    @Test
    void executeKeepsOriginalUrlWhenMinioPersistenceFails() throws Exception {
        String taskId = "task-minio-fail";
        String sourceUrl = "https://media.example.invalid/video.mp4";
        when(agentPlanClient.createVideoTask(any(), anyString(), any(), anyInt(), any(), any(), any()))
                .thenReturn(taskId);
        when(agentPlanClient.getVideoTask(config, taskId))
                .thenReturn(Map.of("status", "success", "videoUrl", sourceUrl));
        when(minioService.uploadFromUrl(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("MinIO unavailable"));

        Map<String, Object> result = executor.execute(
                node, Map.of("prompt", "test prompt"), progressCallback
        );

        assertEquals(sourceUrl, result.get("videoUrl"));
        assertEquals(sourceUrl, result.get("output"));
        assertEquals("succeeded", result.get("status"));
    }

    @Test
    void exposesVideoGenerateNodeType() {
        assertEquals("video_generate", executor.getSupportedNodeType());
    }
}
