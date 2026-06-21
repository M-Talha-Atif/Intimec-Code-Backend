package com.aidentx.llmservice.controller;

import com.aidentx.llmservice.dto.LlmRequest;
import com.aidentx.llmservice.dto.LlmResponse;
import com.aidentx.llmservice.model.enums.ProviderType;
import com.aidentx.llmservice.repository.ProviderRepository;
import com.aidentx.llmservice.service.LlmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LlmController.class)
class LlmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmService llmService;
    
    @MockBean
    private ProviderRepository providerRepository;
    
    @Test
    void healthEndpoint_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/llm/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("LLM Service is running"));
    }

    @Test
    void processLlmRequest_WithValidRequest_ShouldReturnOk() throws Exception {
        // Given
        LlmResponse mockResponse = new LlmResponse(
            "Test response", 
            ProviderType.GOOGLE_AI_STUDIO,
            "test-model"
        );
        mockResponse.setRequestId("test-id");
        mockResponse.setSuccess(true);  // Set success to true for valid request
        
        when(llmService.processRequest(any(LlmRequest.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/llm/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "provider": "GOOGLE_AI_STUDIO",
                        "apiKey": "test-api-key",
                        "prompt": "Test prompt"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Test response"))
                .andExpect(jsonPath("$.provider").value("GOOGLE_AI_STUDIO"))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void processLlmRequest_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given - Mock service to return error response for invalid request (missing apiKey)
        LlmResponse errorResponse = new LlmResponse(
            "API key is required", 
            ProviderType.DEEPSEEK
        );
        errorResponse.setSuccess(false);
        
        when(llmService.processRequest(any(LlmRequest.class))).thenReturn(errorResponse);
        
        // When & Then
        mockMvc.perform(post("/api/v1/llm/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "provider": "DEEPSEEK",
                        "prompt": "Test prompt"
                    }
                    """))
                .andExpect(status().isBadRequest());
    }
}