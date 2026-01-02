package io.realguardio.orchestration;

import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.SecuritySystemCreated;
import io.eventuate.tram.commands.common.Command;
import io.eventuate.tram.commands.consumer.CommandHandlerParams;
import io.eventuate.tram.commands.consumer.CommandReplyProducer;
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
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private CommandOutboxTestSupport commandOutboxTestSupport;

  @Autowired
  private CommandReplyProducer commandReplyProducer;

  public List<Message> findMessagesSentToChannel(String channel) {
    return jdbcTemplate.query("select headers,payload from message where destination = ?", (rs, rowNum) -> {
      String headers = rs.getString("headers");
      String payload = rs.getString("payload");
      return MessageBuilder.withPayload(payload).withExtraHeaders("", JSonMapper.fromJson(headers, Map.class)).build();
    }, channel);
  }

  public <T extends Command> Message assertThatCommandMessageSent(Class<T> commandMessageType, String channel) {
    // Wait for the message to appear in the outbox
    eventually(10, 500, TimeUnit.MILLISECONDS, () -> {
      List<Message> messages = findMessagesOfType(channel, commandMessageType);
      assertThat(messages).withFailMessage("Expected to find %s in channel %s", commandMessageType.getSimpleName(), channel).isNotEmpty();
    });

    logger.info("Verified {} was sent to {}", commandMessageType.getSimpleName(), channel);

    // Find the most recently sent message of this type
    List<Message> messages = findMessagesOfType(channel, commandMessageType);
    // Return the last (most recent) message
    return messages.get(messages.size() - 1);
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
