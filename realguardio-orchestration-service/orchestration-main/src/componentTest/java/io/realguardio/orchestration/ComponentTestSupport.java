package io.realguardio.orchestration;

import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.tram.commands.common.Command;
import io.eventuate.tram.commands.consumer.CommandHandlerParams;
import io.eventuate.tram.commands.consumer.CommandReplyProducer;
import io.eventuate.tram.commands.consumer.CommandReplyToken;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.producer.MessageBuilder;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupport;
import io.realguardio.securitysystem.api.CreateSecuritySystemCommand;
import io.realguardio.securitysystem.api.SecuritySystemCreated;
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

  public <T extends Command> Message assertThatCommandMessageSent(Class<T> commandMessageType, String securitySystemServiceChannel) {
    eventually(10, 500, TimeUnit.MILLISECONDS, () -> {
      commandOutboxTestSupport.assertCommandMessageSent(securitySystemServiceChannel, commandMessageType);
    });

    logger.info("Verified CreateSecuritySystemCommand was sent to {}", securitySystemServiceChannel);

    List<Message> messages = findMessagesSentToChannel(securitySystemServiceChannel);
    assertThat(messages).hasSize(1);
    return messages.get(0);
  }

  public void sendReply(Message createCommandMessage, SecuritySystemCreated reply) {
    CommandHandlerParams commandHandlerParams = new CommandHandlerParams(createCommandMessage, CreateSecuritySystemCommand.class, Optional.empty());
    CommandReplyToken commandReplyToken = new CommandReplyToken(commandHandlerParams.getCorrelationHeaders(), commandHandlerParams.getDefaultReplyChannel().orElse(null));

    commandReplyProducer.sendReplies(commandReplyToken,
        withSuccess(reply));
  }



}
