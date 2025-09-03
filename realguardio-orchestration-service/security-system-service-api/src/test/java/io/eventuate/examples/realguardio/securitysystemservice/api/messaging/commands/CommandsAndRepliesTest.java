package io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands;

import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.LocationNoted;

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
    
    @Test
    void shouldSerializeAndDeserializeNoteLocationCreatedCommand() throws Exception {
        NoteLocationCreatedCommand command = new NoteLocationCreatedCommand(789L, 321L);
        
        String json = objectMapper.writeValueAsString(command);
        NoteLocationCreatedCommand deserialized = objectMapper.readValue(json, NoteLocationCreatedCommand.class);
        
        assertThat(deserialized).isEqualTo(command);
        assertThat(deserialized.securitySystemId()).isEqualTo(789L);
        assertThat(deserialized.locationId()).isEqualTo(321L);
    }
    
    @Test
    void shouldSerializeAndDeserializeLocationNoted() throws Exception {
        LocationNoted reply = new LocationNoted();
        
        String json = objectMapper.writeValueAsString(reply);
        LocationNoted deserialized = objectMapper.readValue(json, LocationNoted.class);
        
        assertThat(deserialized).isEqualTo(reply);
    }
}