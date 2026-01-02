package io.realguardio.orchestration.restapi.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateSecuritySystemDTOTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldSerializeCreateSecuritySystemRequest() throws Exception {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(100L);

        String json = objectMapper.writeValueAsString(request);
        DocumentContext jsonContext = JsonPath.parse(json);

        assertThat(jsonContext.read("$.locationId", Long.class)).isEqualTo(100L);
    }

    @Test
    void shouldDeserializeCreateSecuritySystemRequest() throws Exception {
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("locationId", 100L);
        String json = objectMapper.writeValueAsString(jsonNode);

        CreateSecuritySystemRequest request = objectMapper.readValue(json, CreateSecuritySystemRequest.class);

        assertThat(request.locationId()).isEqualTo(100L);
    }

    @Test
    void shouldBeValidWithLocationId() {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(100L);
        assertThat(request.isValid()).isTrue();
    }

    @Test
    void shouldBeInvalidWithNullLocationId() {
        CreateSecuritySystemRequest request = new CreateSecuritySystemRequest(null);
        assertThat(request.isValid()).isFalse();
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