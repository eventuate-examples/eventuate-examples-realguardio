package io.realguardio.orchestration.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.realguardio.orchestration.restapi.dto.CreateSecuritySystemRequest;
import io.realguardio.orchestration.sagas.LocationAlreadyHasSecuritySystemException;
import io.realguardio.orchestration.sagas.SecuritySystemSagaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.anyLong;
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
    void shouldCreateSecuritySystemWithLocationId() throws Exception {
        Long locationId = 100L;
        Long securitySystemId = 200L;
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(locationId);

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

    @Test
    void shouldReturn400WhenLocationIdNotProvided() throws Exception {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(null);

        mockMvc.perform(post("/securitysystems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn503WhenTimeoutOccurs() throws Exception {
        Long locationId = 100L;
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(locationId);

        CompletableFuture<Long> future = new CompletableFuture<>();
        future.completeExceptionally(new TimeoutException("Request timed out"));

        when(securitySystemSagaService.createSecuritySystemWithLocationId(anyLong()))
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
    void shouldReturn409WhenLocationAlreadyHasSecuritySystem() throws Exception {
        Long locationId = 100L;
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(locationId);

        CompletableFuture<Long> future = new CompletableFuture<>();
        future.completeExceptionally(new LocationAlreadyHasSecuritySystemException(locationId));

        when(securitySystemSagaService.createSecuritySystemWithLocationId(locationId))
                .thenReturn(future);

        var result = mockMvc.perform(post("/securitysystems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Location already has a SecuritySystem"));
    }
}