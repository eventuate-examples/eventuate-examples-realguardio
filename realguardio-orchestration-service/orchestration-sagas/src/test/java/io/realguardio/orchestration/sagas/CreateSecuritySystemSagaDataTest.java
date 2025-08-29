package io.realguardio.orchestration.sagas;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateSecuritySystemSagaDataTest {

    @Test
    void shouldStoreAndRetrieveCustomerData() {
        CreateSecuritySystemSagaData data = new CreateSecuritySystemSagaData(100L, "Warehouse");
        
        assertThat(data.getCustomerId()).isEqualTo(100L);
        assertThat(data.getLocationName()).isEqualTo("Warehouse");
    }
    
    @Test
    void shouldStoreSecuritySystemId() {
        CreateSecuritySystemSagaData data = new CreateSecuritySystemSagaData(100L, "Warehouse");
        
        data.setSecuritySystemId(200L);
        
        assertThat(data.getSecuritySystemId()).isEqualTo(200L);
    }
    
    @Test
    void shouldStoreLocationId() {
        CreateSecuritySystemSagaData data = new CreateSecuritySystemSagaData(100L, "Warehouse");
        
        data.setLocationId(300L);
        
        assertThat(data.getLocationId()).isEqualTo(300L);
    }
    
    @Test
    void shouldStoreRejectionReason() {
        CreateSecuritySystemSagaData data = new CreateSecuritySystemSagaData(100L, "Warehouse");
        
        data.setRejectionReason("Customer not found");
        
        assertThat(data.getRejectionReason()).isEqualTo("Customer not found");
    }
    
    @Test
    void shouldStoreSagaId() {
        CreateSecuritySystemSagaData data = new CreateSecuritySystemSagaData(100L, "Warehouse");
        
        data.setSagaId("saga-456");
        
        assertThat(data.getSagaId()).isEqualTo("saga-456");
    }
}