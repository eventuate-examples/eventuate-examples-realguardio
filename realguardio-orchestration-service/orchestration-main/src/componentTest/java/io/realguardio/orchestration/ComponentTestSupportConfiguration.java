package io.realguardio.orchestration;

import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupportConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.UUID;

@Configuration
@Import(CommandOutboxTestSupportConfiguration.class)
public class ComponentTestSupportConfiguration {
  @Bean
  public io.eventuate.tram.messaging.producer.common.MessageProducerImplementation messageSender(io.eventuate.messaging.kafka.producer.EventuateKafkaProducer eventuateKafkaProducer) {
    return new io.eventuate.tram.messaging.producer.common.MessageProducerImplementation() {
      @Override
      public void send(Message message) {
        message.getHeaders().put(Message.ID, UUID.randomUUID().toString());
        eventuateKafkaProducer.send(message.getRequiredHeader(Message.DESTINATION), "1", JSonMapper.toJson(message));
      }
    };
  }

  @Bean
  public ComponentTestSupport outboxDao() {
    return new ComponentTestSupport();
  }
}
