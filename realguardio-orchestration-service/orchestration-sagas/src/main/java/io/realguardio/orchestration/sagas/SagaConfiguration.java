package io.realguardio.orchestration.sagas;

import io.eventuate.tram.messaging.common.ChannelMapping;
import io.eventuate.tram.sagas.orchestration.SagaInstanceFactory;
import io.eventuate.tram.sagas.spring.orchestration.SagaOrchestratorConfiguration;
import io.eventuate.tram.spring.flyway.EventuateTramFlywayMigrationConfiguration;
import io.realguardio.orchestration.sagas.proxies.CustomerServiceProxy;
import io.realguardio.orchestration.sagas.proxies.SecuritySystemServiceProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Map;

@Configuration
@Import({SagaOrchestratorConfiguration.class, EventuateTramFlywayMigrationConfiguration.class})
public class SagaConfiguration {

    @Bean
    public PendingSecuritySystemResponses pendingSecuritySystemResponses() {
        return new PendingSecuritySystemResponses();
    }

    @Bean
    public SecuritySystemSagaService securitySystemSagaService(SagaInstanceFactory sagaInstanceFactory,
                                                                 CreateSecuritySystemSaga createSecuritySystemSaga,
                                                                 CreateSecuritySystemWithLocationIdSaga createSecuritySystemWithLocationIdSaga,
                                                                 PendingSecuritySystemResponses pendingResponses) {
        return new SecuritySystemSagaService(sagaInstanceFactory, createSecuritySystemSaga,
                createSecuritySystemWithLocationIdSaga, pendingResponses);
    }

    @Bean
    public CreateSecuritySystemSaga createSecuritySystemSaga(
            SecuritySystemServiceProxy securitySystemServiceProxy,
            CustomerServiceProxy customerServiceProxy,
            PendingSecuritySystemResponses pendingResponses) {
        return new CreateSecuritySystemSaga(securitySystemServiceProxy, customerServiceProxy, pendingResponses);
    }

    @Bean
    public CreateSecuritySystemWithLocationIdSaga createSecuritySystemWithLocationIdSaga(
            CustomerServiceProxy customerServiceProxy,
            SecuritySystemServiceProxy securitySystemServiceProxy,
            PendingSecuritySystemResponses pendingResponses) {
        return new CreateSecuritySystemWithLocationIdSaga(customerServiceProxy, securitySystemServiceProxy, pendingResponses);
    }

    @Value("${securitysystemservice.channel:security-system-service}")
    private String securitySystemServiceChannel;

    @Value("${customerservice.channel:customer-service}")
    private String customerServiceChannel;

    @Bean
    public ChannelMapping channelMapping() {
        var outMappings = Map.of(
                "security-system-service", securitySystemServiceChannel,
                "customer-service", customerServiceChannel
        );
        var inMappings = Map.of(
                securitySystemServiceChannel, "security-system-service",
                customerServiceChannel, "customer-service"
        );
        return logicalChannel -> {
            if (outMappings.containsKey(logicalChannel))
                return outMappings.get(logicalChannel);
            else
                return inMappings.getOrDefault(logicalChannel, logicalChannel);
        };
    }
}