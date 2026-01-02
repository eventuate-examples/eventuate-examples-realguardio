package io.realguardio.orchestration.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.realguardio.orchestration.restapi.dto.CreateSecuritySystemRequest;
import io.realguardio.orchestration.restapi.dto.CreateSecuritySystemResponse;
import io.realguardio.orchestration.sagas.SecuritySystemSagaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SecuritySystemController.class)
@Import({GlobalExceptionHandler.class, SecuritySystemController.class})
class SecuritySystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SecuritySystemSagaService securitySystemSagaService;

    @Test
    void shouldCreateSecuritySystemSuccessfully() throws Exception {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(100L, "Warehouse", null);
        Long securitySystemId = 200L;

        when(securitySystemSagaService.createSecuritySystem(100L, "Warehouse"))
                .thenReturn(CompletableFuture.completedFuture(securitySystemId));

        var result = mockMvc.perform(post("/securitysystems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andDo(print())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.securitySystemId").value(200));
    }

    @Test
    void shouldReturn400WhenNeitherLocationIdNorCustomerIdProvided() throws Exception {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(null, null, null);

        mockMvc.perform(post("/securitysystems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenLocationNameIsBlankForOldFlow() throws Exception {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(100L, "", null);

        mockMvc.perform(post("/securitysystems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn503WhenTimeoutOccurs() throws Exception {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(100L, "Warehouse", null);

        CompletableFuture<Long> future = new CompletableFuture<>();
        future.completeExceptionally(new TimeoutException("Request timed out"));

        when(securitySystemSagaService.createSecuritySystem(anyLong(), anyString()))
                .thenReturn(future);

        var result = mockMvc.perform(post("/securitysystems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service temporarily unavailable"));
    }

    @Test
    void shouldCreateSecuritySystemWithLocationId() throws Exception {
        Long locationId = 100L;
        Long securitySystemId = 200L;
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(null, null, locationId);

        when(securitySystemSagaService.createSecuritySystemWithLocationId(locationId))
                .thenReturn(CompletableFuture.completedFuture(securitySystemId));

        var result = mockMvc.perform(post("/securitysystems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andDo(print())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.securitySystemId").value(200));
    }
}