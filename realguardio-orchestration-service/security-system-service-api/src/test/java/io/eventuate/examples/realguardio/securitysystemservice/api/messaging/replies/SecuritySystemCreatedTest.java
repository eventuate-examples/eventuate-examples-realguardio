package io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecuritySystemCreatedTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserialize() throws Exception {
        SecuritySystemCreated reply = new SecuritySystemCreated(123L);
        
        String json = objectMapper.writeValueAsString(reply);
        SecuritySystemCreated deserialized = objectMapper.readValue(json, SecuritySystemCreated.class);
        
        assertThat(deserialized).isEqualTo(reply);
        assertThat(deserialized.securitySystemId()).isEqualTo(123L);
    }
}