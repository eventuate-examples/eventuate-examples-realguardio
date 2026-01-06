package io.realguardio.orchestration;

import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupportConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CommandOutboxTestSupportConfiguration.class)
public class ComponentTestSupportConfiguration {

  @Bean
  public ComponentTestSupport outboxDao() {
    return new ComponentTestSupport();
  }
}
