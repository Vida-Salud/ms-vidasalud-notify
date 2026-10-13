package cl.duoc.ms_vidasalud_notify.consumer.record;

import cl.duoc.ms_vidasalud_notify.config.RabbitMQConfig;
import cl.duoc.ms_vidasalud_notify.consumer.support.ProcesadorMensajes;
import cl.duoc.ms_vidasalud_notify.service.RecordService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Escucha {@link RabbitMQConfig#RECORD_QUEUE} (q.cmd.record) y ejecuta {@link RecordService#generarComprobante}. */
@Component
public class RecordConsumer {

    private final ProcesadorMensajes procesador;
    private final RecordService recordService;

    public RecordConsumer(ProcesadorMensajes procesador, RecordService recordService) {
        this.procesador = procesador;
        this.recordService = recordService;
    }

    @RabbitListener(queues = RabbitMQConfig.RECORD_QUEUE)
    public void recibir(Message message, Channel channel) throws IOException {
        procesador.procesar(message, channel, RabbitMQConfig.RECORD_QUEUE, recordService::generarComprobante);
    }
}
