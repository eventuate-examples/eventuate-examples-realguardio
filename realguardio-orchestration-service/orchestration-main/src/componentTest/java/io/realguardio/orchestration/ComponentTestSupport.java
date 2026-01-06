package io.realguardio.orchestration;

import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.SecuritySystemCreated;
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
import java.util.function.Predicate;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;
import static io.eventuate.util.test.async.Eventually.eventually;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentTestSupport {

  protected static Logger logger = LoggerFactory.getLogger(ComponentTestSupport.class);

  @Autowired
  private JdbcTemplate jdbcTemplate;

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

  public <T extends Command> Message assertThatCommandMessageSent(Class<T> commandMessageType, String channel) {
    return assertThatCommandMessageSent(commandMessageType, channel, cmd -> true);
  }

  public <T extends Command> Message assertThatCommandMessageSent(Class<T> commandMessageType, String channel, Predicate<T> predicate) {
    // Wait for a matching message to appear in the outbox
    eventually(10, 500, TimeUnit.MILLISECONDS, () -> {
      List<T> commands = findCommandsOfType(channel, commandMessageType);
      assertThat(commands.stream().anyMatch(predicate))
              .withFailMessage("Expected to find matching %s in channel %s", commandMessageType.getSimpleName(), channel)
              .isTrue();
    });

    logger.info("Verified {} was sent to {}", commandMessageType.getSimpleName(), channel);

    // Find the matching command and return its message
    List<Message> messages = findMessagesOfType(channel, commandMessageType);
    for (int i = messages.size() - 1; i >= 0; i--) {
      Message msg = messages.get(i);
      T command = JSonMapper.fromJson(msg.getPayload(), commandMessageType);
      if (predicate.test(command)) {
        return msg;
      }
    }
    throw new IllegalStateException("No matching message found");
  }

  public <T extends Command> List<T> findCommandsOfType(String channel, Class<T> commandType) {
    return findMessagesOfType(channel, commandType).stream()
            .map(msg -> JSonMapper.fromJson(msg.getPayload(), commandType))
            .toList();
  }

  public <T extends Command> List<Message> findMessagesOfType(String channel, Class<T> commandType) {
    String commandTypeName = commandType.getName();
    return jdbcTemplate.query(
            "SELECT headers, payload FROM message WHERE destination = ? AND headers LIKE ?",
            (rs, rowNum) -> {
              String headers = rs.getString("headers");
              String payload = rs.getString("payload");
              return MessageBuilder.withPayload(payload).withExtraHeaders("", JSonMapper.fromJson(headers, Map.class)).build();
            },
            channel,
            "%" + commandTypeName + "%"
    );
  }

  public void sendReply(Message createCommandMessage, SecuritySystemCreated reply) {
    sendReply(createCommandMessage, CreateSecuritySystemCommand.class, reply);
  }

  public <C extends Command> void sendReply(Message commandMessage, Class<C> commandClass, Object reply) {
    CommandHandlerParams commandHandlerParams = new CommandHandlerParams(commandMessage, commandClass, Optional.empty());
    CommandReplyToken commandReplyToken = new CommandReplyToken(commandHandlerParams.getCorrelationHeaders(), commandHandlerParams.getDefaultReplyChannel().orElse(null));

    commandReplyProducer.sendReplies(commandReplyToken, withSuccess(reply));
  }



}
