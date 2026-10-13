package cl.duoc.ms_vidasalud_notify.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DIRECT_EXCHANGE = "cmd.direct";
    public static final String TOPIC_EXCHANGE = "cmd.topic";
    public static final String DEAD_LETTER_EXCHANGE = "cmd.dead.dlx";

    public static final String EMAIL_QUEUE = "q.cmd.email";
    public static final String ADMISSION_QUEUE = "q.cmd.admission";
    public static final String RECORD_QUEUE = "q.cmd.record";

    public static final String EMAIL_DLQ = "q.cmd.email.dlq";
    public static final String ADMISSION_DLQ = "q.cmd.admission.dlq";
    public static final String RECORD_DLQ = "q.cmd.record.dlq";

    public static final String EMAIL_ROUTING_KEY = "email.send";
    public static final String ADMISSION_ROUTING_KEY = "admission.ticket";
    public static final String RECORD_ROUTING_KEY = "record.gen";

    public static final String EMAIL_TOPIC_PATTERN = "email.*";
    public static final String ADMISSION_TOPIC_PATTERN = "admission.#";
    public static final String RECORD_TOPIC_PATTERN = "record.*";

    public static final String EMAIL_DEAD_ROUTING_KEY = "email.dead";
    public static final String ADMISSION_DEAD_ROUTING_KEY = "admission.dead";
    public static final String RECORD_DEAD_ROUTING_KEY = "record.dead";

    public static final int QUEUE_TTL_MS = 30_000;
    public static final int DLQ_TTL_MS = 86_400_000;
    public static final int QUEUE_MAX_LENGTH = 1_000;

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return mainQueue(EMAIL_QUEUE, EMAIL_DEAD_ROUTING_KEY);
    }

    @Bean
    public Queue admissionQueue() {
        return mainQueue(ADMISSION_QUEUE, ADMISSION_DEAD_ROUTING_KEY);
    }

    @Bean
    public Queue recordQueue() {
        return mainQueue(RECORD_QUEUE, RECORD_DEAD_ROUTING_KEY);
    }

    private static Queue mainQueue(String name, String deadRoutingKey) {
        return QueueBuilder.durable(name)
                .ttl(QUEUE_TTL_MS)
                .maxLength(QUEUE_MAX_LENGTH)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(deadRoutingKey)
                .build();
    }

    @Bean
    public Queue emailDlq() {
        return dlq(EMAIL_DLQ);
    }

    @Bean
    public Queue admissionDlq() {
        return dlq(ADMISSION_DLQ);
    }

    @Bean
    public Queue recordDlq() {
        return dlq(RECORD_DLQ);
    }

    private static Queue dlq(String name) {
        return QueueBuilder.durable(name)
                .ttl(DLQ_TTL_MS)
                .build();
    }

    @Bean
    public Binding emailDirectBinding(@Qualifier("emailQueue") Queue queue,
                                      @Qualifier("directExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding admissionDirectBinding(@Qualifier("admissionQueue") Queue queue,
                                          @Qualifier("directExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ADMISSION_ROUTING_KEY);
    }

    @Bean
    public Binding recordDirectBinding(@Qualifier("recordQueue") Queue queue,
                                       @Qualifier("directExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RECORD_ROUTING_KEY);
    }

    @Bean
    public Binding emailTopicBinding(@Qualifier("emailQueue") Queue queue,
                                     @Qualifier("topicExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(EMAIL_TOPIC_PATTERN);
    }

    @Bean
    public Binding admissionTopicBinding(@Qualifier("admissionQueue") Queue queue,
                                         @Qualifier("topicExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ADMISSION_TOPIC_PATTERN);
    }

    @Bean
    public Binding recordTopicBinding(@Qualifier("recordQueue") Queue queue,
                                      @Qualifier("topicExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RECORD_TOPIC_PATTERN);
    }

    @Bean
    public Binding emailDlqBinding(@Qualifier("emailDlq") Queue queue,
                                   @Qualifier("deadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(EMAIL_DEAD_ROUTING_KEY);
    }

    @Bean
    public Binding admissionDlqBinding(@Qualifier("admissionDlq") Queue queue,
                                       @Qualifier("deadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ADMISSION_DEAD_ROUTING_KEY);
    }

    @Bean
    public Binding recordDlqBinding(@Qualifier("recordDlq") Queue queue,
                                    @Qualifier("deadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RECORD_DEAD_ROUTING_KEY);
    }
}
