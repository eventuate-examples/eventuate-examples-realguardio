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

    @Value("${securitysystemservice.channel:security-system-service}")
    private String securitySystemServiceChannel;

    @Bean
    public ChannelMapping channelMapping() {
        var outMappings = Map.of(
                "security-system-service", securitySystemServiceChannel
        );
        var inMappings = Map.of(
                securitySystemServiceChannel, "security-system-service"
        );
        return logicalChannel -> {
            if (outMappings.containsKey(logicalChannel))
                return outMappings.get(logicalChannel);
            else
                return inMappings.getOrDefault(logicalChannel, logicalChannel);
        };
    }
}