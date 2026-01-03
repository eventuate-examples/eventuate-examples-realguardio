package io.eventuate.examples.realguardio.customerservice.api.messaging.commands;

import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationNotFound;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationValidated;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserializeValidateLocationCommand() throws Exception {
        ValidateLocationCommand command = new ValidateLocationCommand(100L);

        String json = objectMapper.writeValueAsString(command);
        ValidateLocationCommand deserialized =
            objectMapper.readValue(json, ValidateLocationCommand.class);

        assertThat(deserialized).isEqualTo(command);
        assertThat(deserialized.locationId()).isEqualTo(100L);
    }

    @Test
    void shouldSerializeAndDeserializeLocationValidated() throws Exception {
        LocationValidated reply = new LocationValidated(100L, "Warehouse", 200L);

        String json = objectMapper.writeValueAsString(reply);
        LocationValidated deserialized =
            objectMapper.readValue(json, LocationValidated.class);

        assertThat(deserialized).isEqualTo(reply);
        assertThat(deserialized.locationId()).isEqualTo(100L);
        assertThat(deserialized.locationName()).isEqualTo("Warehouse");
        assertThat(deserialized.customerId()).isEqualTo(200L);
    }

    @Test
    void shouldSerializeAndDeserializeLocationNotFound() throws Exception {
        LocationNotFound reply = new LocationNotFound();

        String json = objectMapper.writeValueAsString(reply);
        LocationNotFound deserialized = objectMapper.readValue(json, LocationNotFound.class);

        assertThat(deserialized).isEqualTo(reply);
    }
}
