package cl.duoc.ms_vidasalud_notify.consumer.email;

import cl.duoc.ms_vidasalud_notify.config.RabbitMQConfig;
import cl.duoc.ms_vidasalud_notify.consumer.support.ProcesadorMensajes;
import cl.duoc.ms_vidasalud_notify.service.EmailService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Escucha {@link RabbitMQConfig#EMAIL_QUEUE} (q.cmd.email) y ejecuta {@link EmailService#enviar}. */
@Component
public class EmailConsumer {

    private final ProcesadorMensajes procesador;
    private final EmailService emailService;

    public EmailConsumer(ProcesadorMensajes procesador, EmailService emailService) {
        this.procesador = procesador;
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void recibir(Message message, Channel channel) throws IOException {
        procesador.procesar(message, channel, RabbitMQConfig.EMAIL_QUEUE, emailService::enviar);
    }
}
