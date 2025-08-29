package io.realguardio.orchestration.sagas;

import io.eventuate.tram.sagas.orchestration.SagaInstanceFactory;
import io.eventuate.tram.sagas.spring.orchestration.SagaOrchestratorConfiguration;
import io.realguardio.orchestration.sagas.proxies.CustomerServiceProxy;
import io.realguardio.orchestration.sagas.proxies.SecuritySystemServiceProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(SagaOrchestratorConfiguration.class)
public class SagaConfiguration {

    @Bean
    public PendingSecuritySystemResponses pendingSecuritySystemResponses() {
        return new PendingSecuritySystemResponses();
    }

    @Bean
    public SecuritySystemSagaService securitySystemSagaService(SagaInstanceFactory sagaInstanceFactory,
                                                                 CreateSecuritySystemSaga createSecuritySystemSaga,
                                                                 PendingSecuritySystemResponses pendingResponses) {
        return new SecuritySystemSagaService(sagaInstanceFactory, createSecuritySystemSaga, pendingResponses);
    }

    @Bean
    public CreateSecuritySystemSaga createSecuritySystemSaga(
            SecuritySystemServiceProxy securitySystemServiceProxy,
            CustomerServiceProxy customerServiceProxy,
            PendingSecuritySystemResponses pendingResponses) {
        return new CreateSecuritySystemSaga(securitySystemServiceProxy, customerServiceProxy, pendingResponses);
    }
}