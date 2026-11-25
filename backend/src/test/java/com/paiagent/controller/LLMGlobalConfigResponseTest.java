package com.paiagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiagent.dto.LLMGlobalConfigResponse;
import com.paiagent.entity.LLMGlobalConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LLMGlobalConfigResponseTest {

    @Test
    void responseExposesOnlyWhetherApiKeyIsConfigured() throws Exception {
        LLMGlobalConfig config = new LLMGlobalConfig();
        config.setId(7L);
        config.setProvider("openai");
        config.setConfigName("test-config");
        config.setApiKey("sk-test-secret-must-not-leak");
        config.setModel("gpt-test");

        LLMGlobalConfigResponse response = LLMGlobalConfigResponse.from(config);
        String json = new ObjectMapper().writeValueAsString(response);

        assertTrue(response.apiKeyConfigured());
        assertTrue(json.contains("\\"apiKeyConfigured\\":true"));
        assertFalse(json.contains("sk-test-secret-must-not-leak"));
        assertFalse(json.contains("\\"apiKey\\":"));
    }
}
