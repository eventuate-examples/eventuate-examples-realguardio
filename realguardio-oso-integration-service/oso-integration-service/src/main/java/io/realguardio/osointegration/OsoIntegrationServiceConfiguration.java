package io.realguardio.osointegration;

import io.eventuate.tram.spring.consumer.common.TramNoopDuplicateMessageDetectorConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(TramNoopDuplicateMessageDetectorConfiguration.class)
public class OsoIntegrationServiceConfiguration {
}
