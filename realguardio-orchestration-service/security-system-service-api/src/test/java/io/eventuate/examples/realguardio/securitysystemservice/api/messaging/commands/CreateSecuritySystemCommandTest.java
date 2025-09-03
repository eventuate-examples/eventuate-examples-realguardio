package io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateSecuritySystemCommandTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserialize() throws Exception {
        CreateSecuritySystemCommand command = new CreateSecuritySystemCommand("Main Office");
        
        String json = objectMapper.writeValueAsString(command);
        CreateSecuritySystemCommand deserialized = objectMapper.readValue(json, CreateSecuritySystemCommand.class);
        
        assertThat(deserialized).isEqualTo(command);
        assertThat(deserialized.locationName()).isEqualTo("Main Office");
    }
}