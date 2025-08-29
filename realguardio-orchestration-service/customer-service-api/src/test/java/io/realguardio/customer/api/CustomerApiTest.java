package io.realguardio.customer.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserializeCreateLocationWithSecuritySystemCommand() throws Exception {
        CreateLocationWithSecuritySystemCommand command = 
            new CreateLocationWithSecuritySystemCommand(100L, "Warehouse", 200L);
        
        String json = objectMapper.writeValueAsString(command);
        CreateLocationWithSecuritySystemCommand deserialized = 
            objectMapper.readValue(json, CreateLocationWithSecuritySystemCommand.class);
        
        assertThat(deserialized).isEqualTo(command);
        assertThat(deserialized.customerId()).isEqualTo(100L);
        assertThat(deserialized.locationName()).isEqualTo("Warehouse");
        assertThat(deserialized.securitySystemId()).isEqualTo(200L);
    }
    
    @Test
    void shouldSerializeAndDeserializeLocationCreatedWithSecuritySystem() throws Exception {
        LocationCreatedWithSecuritySystem reply = new LocationCreatedWithSecuritySystem(300L);
        
        String json = objectMapper.writeValueAsString(reply);
        LocationCreatedWithSecuritySystem deserialized = 
            objectMapper.readValue(json, LocationCreatedWithSecuritySystem.class);
        
        assertThat(deserialized).isEqualTo(reply);
        assertThat(deserialized.locationId()).isEqualTo(300L);
    }
    
    @Test
    void shouldSerializeAndDeserializeCustomerNotFound() throws Exception {
        CustomerNotFound reply = new CustomerNotFound();
        
        String json = objectMapper.writeValueAsString(reply);
        CustomerNotFound deserialized = objectMapper.readValue(json, CustomerNotFound.class);
        
        assertThat(deserialized).isEqualTo(reply);
    }
    
    @Test
    void shouldSerializeAndDeserializeLocationAlreadyHasSecuritySystem() throws Exception {
        LocationAlreadyHasSecuritySystem reply = new LocationAlreadyHasSecuritySystem();
        
        String json = objectMapper.writeValueAsString(reply);
        LocationAlreadyHasSecuritySystem deserialized = 
            objectMapper.readValue(json, LocationAlreadyHasSecuritySystem.class);
        
        assertThat(deserialized).isEqualTo(reply);
    }
}