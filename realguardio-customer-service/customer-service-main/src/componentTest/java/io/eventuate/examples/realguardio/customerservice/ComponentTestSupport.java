package io.eventuate.examples.realguardio.customerservice;

import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.tram.commands.common.Command;
import io.eventuate.tram.commands.consumer.CommandHandlerParams;
import io.eventuate.tram.testing.producer.kafka.replies.DirectToKafkaCommandReplyProducer;
import io.eventuate.tram.commands.consumer.CommandReplyToken;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.producer.MessageBuilder;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;
import static io.eventuate.util.test.async.Eventually.eventually;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentTestSupport {

  protected static Logger logger = LoggerFactory.getLogger(ComponentTestSupport.class);

  @Autowired
  public JdbcTemplate jdbcTemplate;

  @Autowired
  private CommandOutboxTestSupport commandOutboxTestSupport;

  @Autowired
  private DirectToKafkaCommandReplyProducer commandReplyProducer;

  public List<Message> findMessagesSentToChannel(String channel) {
    return jdbcTemplate.query("select headers,payload from message where destination = ?", (rs, rowNum) -> {
      String headers = rs.getString("headers");
      String payload = rs.getString("payload");
      return MessageBuilder.withPayload(payload).withExtraHeaders("", JSonMapper.fromJson(headers, Map.class)).build();
    }, channel);
  }

  public <T extends Command> Message assertThatCommandMessageSent(Class<T> commandMessageType, String securitySystemServiceChannel) {
    eventually(10, 500, TimeUnit.MILLISECONDS, () -> {
      commandOutboxTestSupport.assertCommandMessageSent(securitySystemServiceChannel, commandMessageType);
    });

    logger.info("Verified CreateSecuritySystemCommand was sent to {}", securitySystemServiceChannel);

    List<Message> messages = findMessagesSentToChannel(securitySystemServiceChannel);
    assertThat(messages).hasSize(1);
    return messages.get(0);
  }

  public void sendReply(Message createCommandMessage, Object reply) {
    CommandHandlerParams commandHandlerParams = new CommandHandlerParams(createCommandMessage, reply.getClass(), Optional.empty());
    CommandReplyToken commandReplyToken = new CommandReplyToken(commandHandlerParams.getCorrelationHeaders(), commandHandlerParams.getDefaultReplyChannel().orElse(null));

    commandReplyProducer.sendReplies(commandReplyToken, withSuccess(reply));
  }

  public void assertDomainEventInOutbox(String aggregateType, String aggregateId, String eventType) {
    eventually(10, 500, TimeUnit.MILLISECONDS, () -> {
      List<Map<String, Object>> events = jdbcTemplate.queryForList(
          "SELECT * FROM message WHERE destination = ?",
          aggregateType);
      
      boolean foundEvent = events.stream().anyMatch(event -> {
        String headersJson = (String) event.get("headers");
        Map<String, Object> headers = JSonMapper.fromJson(headersJson, Map.class);
        
        String eventAggregateId = (String) headers.get("event-aggregate-id");
        String eventTypeHeader = (String) headers.get("event-type");
        
        return aggregateId.equals(eventAggregateId) && eventType.equals(eventTypeHeader);
      });
      
      assertThat(foundEvent)
          .withFailMessage("Expected to find event %s for aggregate %s with id %s in outbox, but found: %s", 
              eventType, aggregateType, aggregateId, events)
          .isTrue();
    });
    
    logger.info("Verified {} event was written to outbox for {} with id {}", eventType, aggregateType, aggregateId);
  }

}
