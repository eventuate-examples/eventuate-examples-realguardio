package io.realguardio.orchestration.restapi.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateSecuritySystemDTOTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldSerializeCreateSecuritySystemRequest() throws Exception {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(100L, "Warehouse");
        
        String json = objectMapper.writeValueAsString(request);
        DocumentContext jsonContext = JsonPath.parse(json);
        
        assertThat(jsonContext.read("$.customerId", Long.class)).isEqualTo(100L);
        assertThat(jsonContext.read("$.locationName", String.class)).isEqualTo("Warehouse");
    }

    @Test
    void shouldDeserializeCreateSecuritySystemRequest() throws Exception {
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("customerId", 100L);
        jsonNode.put("locationName", "Warehouse");
        String json = objectMapper.writeValueAsString(jsonNode);
        
        CreateSecuritySystemRequest request = objectMapper.readValue(json, CreateSecuritySystemRequest.class);
        
        assertThat(request.customerId()).isEqualTo(100L);
        assertThat(request.locationName()).isEqualTo("Warehouse");
    }

    @Test
    void shouldValidateCreateSecuritySystemRequestWithValidData() {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(100L, "Warehouse");
        
        Set<ConstraintViolation<CreateSecuritySystemRequest>> violations = validator.validate(request);
        
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailValidationWhenCustomerIdIsNull() {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(null, "Warehouse");
        
        Set<ConstraintViolation<CreateSecuritySystemRequest>> violations = validator.validate(request);
        
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("must not be null");
    }

    @Test
    void shouldFailValidationWhenLocationNameIsBlank() {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(100L, "");
        
        Set<ConstraintViolation<CreateSecuritySystemRequest>> violations = validator.validate(request);
        
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("must not be blank");
    }

    @Test
    void shouldSerializeCreateSecuritySystemResponse() throws Exception {
        CreateSecuritySystemResponse response = new CreateSecuritySystemResponse(200L);
        
        String json = objectMapper.writeValueAsString(response);
        DocumentContext jsonContext = JsonPath.parse(json);
        
        assertThat(jsonContext.read("$.securitySystemId", Long.class)).isEqualTo(200L);
    }

    @Test
    void shouldDeserializeCreateSecuritySystemResponse() throws Exception {
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("securitySystemId", 200L);
        String json = objectMapper.writeValueAsString(jsonNode);
        
        CreateSecuritySystemResponse response = objectMapper.readValue(json, CreateSecuritySystemResponse.class);
        
        assertThat(response.securitySystemId()).isEqualTo(200L);
    }
}