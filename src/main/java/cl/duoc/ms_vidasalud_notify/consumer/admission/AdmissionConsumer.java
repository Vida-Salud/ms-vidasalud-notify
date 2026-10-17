package cl.duoc.ms_vidasalud_notify.consumer.admission;

import cl.duoc.ms_vidasalud_notify.config.RabbitMQConfig;
import cl.duoc.ms_vidasalud_notify.consumer.support.ProcesadorMensajes;
import cl.duoc.ms_vidasalud_notify.service.AdmissionService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Escucha {@link RabbitMQConfig#ADMISSION_QUEUE} (q.cmd.admission) y ejecuta {@link AdmissionService#procesar}:
 * emite el ticket de admisión o libera el cupo del box si la atención se cancela.
 */
@Component
public class AdmissionConsumer {

    private final ProcesadorMensajes procesador;
    private final AdmissionService admissionService;

    public AdmissionConsumer(ProcesadorMensajes procesador, AdmissionService admissionService) {
        this.procesador = procesador;
        this.admissionService = admissionService;
    }

    @RabbitListener(queues = RabbitMQConfig.ADMISSION_QUEUE)
    public void recibir(Message message, Channel channel) throws IOException {
        procesador.procesar(message, channel, RabbitMQConfig.ADMISSION_QUEUE, admissionService::procesar);
    }
}
