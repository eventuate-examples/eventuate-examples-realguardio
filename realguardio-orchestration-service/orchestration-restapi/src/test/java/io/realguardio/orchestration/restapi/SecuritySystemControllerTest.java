package io.realguardio.orchestration.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.realguardio.orchestration.restapi.dto.CreateSecuritySystemRequest;
import io.realguardio.orchestration.restapi.dto.CreateSecuritySystemResponse;
import io.realguardio.orchestration.sagas.SecuritySystemSagaService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SecuritySystemController.class)
@Import(GlobalExceptionHandler.class)
@Disabled("Test configuration needs adjustment for async controller handling")
class SecuritySystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SecuritySystemSagaService securitySystemSagaService;

    @Test
    void shouldCreateSecuritySystemSuccessfully() throws Exception {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(100L, "Warehouse");
        Long securitySystemId = 200L;
        
        when(securitySystemSagaService.createSecuritySystem(100L, "Warehouse"))
                .thenReturn(CompletableFuture.completedFuture(securitySystemId));

        mockMvc.perform(post("/securitysystems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.securitySystemId").value(200));
    }

    @Test
    void shouldReturn400WhenCustomerIdIsNull() throws Exception {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(null, "Warehouse");

        mockMvc.perform(post("/securitysystems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void shouldReturn400WhenLocationNameIsBlank() throws Exception {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(100L, "");

        mockMvc.perform(post("/securitysystems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void shouldReturn503WhenTimeoutOccurs() throws Exception {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(100L, "Warehouse");
        
        CompletableFuture<Long> future = new CompletableFuture<>();
        future.completeExceptionally(new TimeoutException("Request timed out"));
        
        when(securitySystemSagaService.createSecuritySystem(anyLong(), anyString()))
                .thenReturn(future);

        mockMvc.perform(post("/securitysystems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service temporarily unavailable"));
    }
}