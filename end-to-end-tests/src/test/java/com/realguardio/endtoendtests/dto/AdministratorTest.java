package com.realguardio.endtoendtests.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AdministratorTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void shouldSerializeAdministrator() throws Exception {
        Administrator admin = new Administrator("John", "Doe", "john.doe@example.com");
        
        String json = objectMapper.writeValueAsString(admin);
        
        assertThat(json).contains("\"firstName\":\"John\"");
        assertThat(json).contains("\"lastName\":\"Doe\"");
        assertThat(json).contains("\"email\":\"john.doe@example.com\"");
    }
    
    @Test
    void shouldDeserializeAdministrator() throws Exception {
        String json = "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john.doe@example.com\"}";
        
        Administrator admin = objectMapper.readValue(json, Administrator.class);
        
        assertThat(admin.firstName()).isEqualTo("John");
        assertThat(admin.lastName()).isEqualTo("Doe");
        assertThat(admin.email()).isEqualTo("john.doe@example.com");
    }
}