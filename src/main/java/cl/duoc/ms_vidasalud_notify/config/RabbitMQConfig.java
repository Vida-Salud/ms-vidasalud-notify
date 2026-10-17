package cl.duoc.ms_vidasalud_notify.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología RabbitMQ de notify (patrón punto a punto con colas de trabajo, más un fanout para la cancelación).
 * Todos los nombres están centralizados en las constantes de esta clase.
 * <table class="striped">
 *   <caption>Rutas de mensajería</caption>
 *   <tr><th>Exchange</th><th>Tipo</th><th>Routing key/patrón</th><th>Cola</th><th>Consumidor</th><th>DLQ</th></tr>
 *   <tr><td>cmd.direct</td><td>direct</td><td>email.send</td><td>q.cmd.email</td>
 *       <td>EmailConsumer</td><td>q.cmd.email.dlq</td></tr>
 *   <tr><td>cmd.direct</td><td>direct</td><td>admission.ticket</td><td>q.cmd.admission</td>
 *       <td>AdmissionConsumer</td><td>q.cmd.admission.dlq</td></tr>
 *   <tr><td>cmd.direct</td><td>direct</td><td>record.gen</td><td>q.cmd.record</td>
 *       <td>RecordConsumer</td><td>q.cmd.record.dlq</td></tr>
 *   <tr><td>cmd.topic</td><td>topic</td><td>email.*</td><td>q.cmd.email</td>
 *       <td>EmailConsumer</td><td>q.cmd.email.dlq</td></tr>
 *   <tr><td>cmd.topic</td><td>topic</td><td>admission.#</td><td>q.cmd.admission</td>
 *       <td>AdmissionConsumer</td><td>q.cmd.admission.dlq</td></tr>
 *   <tr><td>cmd.topic</td><td>topic</td><td>record.*</td><td>q.cmd.record</td>
 *       <td>RecordConsumer</td><td>q.cmd.record.dlq</td></tr>
 *   <tr><td>cmd.fanout</td><td>fanout</td><td>(sin routing key)</td><td>q.cmd.email</td>
 *       <td>EmailConsumer</td><td>q.cmd.email.dlq</td></tr>
 *   <tr><td>cmd.fanout</td><td>fanout</td><td>(sin routing key)</td><td>q.cmd.admission</td>
 *       <td>AdmissionConsumer</td><td>q.cmd.admission.dlq</td></tr>
 *   <tr><td>cmd.dead.dlx</td><td>direct</td><td>email.dead</td><td>q.cmd.email.dlq</td>
 *       <td>(sin consumidor)</td><td>-</td></tr>
 *   <tr><td>cmd.dead.dlx</td><td>direct</td><td>admission.dead</td><td>q.cmd.admission.dlq</td>
 *       <td>(sin consumidor)</td><td>-</td></tr>
 *   <tr><td>cmd.dead.dlx</td><td>direct</td><td>record.dead</td><td>q.cmd.record.dlq</td>
 *       <td>(sin consumidor)</td><td>-</td></tr>
 * </table>
 * <p>
 * <b>Colas principales</b> (durables), con los argumentos:
 * <ul>
 *   <li>{@code x-message-ttl} = 30 s ({@link #QUEUE_TTL_MS}): un mensaje que espera más que eso en la cola expira y va al DLX.</li>
 *   <li>{@code x-max-length} = 1000 ({@link #QUEUE_MAX_LENGTH}): al superarlo, el mensaje más antiguo sale de la cola y va al DLX.</li>
 *   <li>{@code x-dead-letter-exchange} = {@code cmd.dead.dlx}: destino de los mensajes rechazados con NACK sin
 *       requeue, expirados o desbordados.</li>
 *   <li>{@code x-dead-letter-routing-key} = {@code email.dead} / {@code admission.dead} / {@code record.dead}:
 *       con ella el DLX (direct) entrega cada mensaje solo a la DLQ de su cola de origen.</li>
 * </ul>
 * <b>DLQ</b> (durables): {@code x-message-ttl} = 24 h ({@link #DLQ_TTL_MS}) y sin consumidor, para que los mensajes
 * fallidos se puedan inspeccionar en la consola de RabbitMQ antes de expirar.
 */
@Configuration
public class RabbitMQConfig {

    public static final String DIRECT_EXCHANGE = "cmd.direct";
    public static final String TOPIC_EXCHANGE = "cmd.topic";
    public static final String FANOUT_EXCHANGE = "cmd.fanout";
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

    /**
     * Devuelve la DLQ asociada a una cola principal.
     *
     * @param cola cola principal ({@link #EMAIL_QUEUE}, {@link #ADMISSION_QUEUE} o {@link #RECORD_QUEUE})
     * @return la DLQ correspondiente
     * @throws IllegalArgumentException si la cola no tiene DLQ configurada
     */
    public static String dlqDe(String cola) {
        return switch (cola) {
            case EMAIL_QUEUE -> EMAIL_DLQ;
            case ADMISSION_QUEUE -> ADMISSION_DLQ;
            case RECORD_QUEUE -> RECORD_DLQ;
            default -> throw new IllegalArgumentException("Cola sin DLQ configurada: " + cola);
        };
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    /**
     * Exchange fanout (publish-subscribe): copia cada mensaje a TODAS las colas enlazadas, sin usar routing key.
     * Se usa para eventos que interesan a varios consumidores, como la cancelación de una atención
     * (avisar al paciente por email y liberar el box en admisión).
     */
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE, true, false);
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
    public Binding emailFanoutBinding(@Qualifier("emailQueue") Queue queue,
                                      @Qualifier("fanoutExchange") FanoutExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange);
    }

    @Bean
    public Binding admissionFanoutBinding(@Qualifier("admissionQueue") Queue queue,
                                          @Qualifier("fanoutExchange") FanoutExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange);
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
