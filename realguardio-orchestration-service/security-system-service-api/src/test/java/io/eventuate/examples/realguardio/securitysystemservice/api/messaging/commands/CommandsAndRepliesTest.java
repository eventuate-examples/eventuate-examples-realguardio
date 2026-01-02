package io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandsAndRepliesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserializeUpdateCreationFailedCommand() throws Exception {
        UpdateCreationFailedCommand command = new UpdateCreationFailedCommand(456L, "Customer not found");

        String json = objectMapper.writeValueAsString(command);
        UpdateCreationFailedCommand deserialized = objectMapper.readValue(json, UpdateCreationFailedCommand.class);

        assertThat(deserialized).isEqualTo(command);
        assertThat(deserialized.securitySystemId()).isEqualTo(456L);
        assertThat(deserialized.rejectionReason()).isEqualTo("Customer not found");
    }
}